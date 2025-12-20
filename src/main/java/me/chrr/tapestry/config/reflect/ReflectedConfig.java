package me.chrr.tapestry.config.reflect;

import com.google.gson.JsonObject;
import me.chrr.tapestry.config.*;
import me.chrr.tapestry.config.reflect.annotation.*;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@NullMarked
public abstract class ReflectedConfig implements Config {
    private final List<Option<?, ?>> options = new ArrayList<>();
    private final Logger logger = LogManager.getLogger("Tapestry/" + getClass().getSimpleName());

    private ConfigIo.@Nullable UpgradeRewriter upgradeRewriter = null;
    private @Nullable Path currentConfigPath = null;
    private @Nullable String translationPrefix = null;

    //region Initialization & Reflection
    protected void reflectOptions() {
        // Find class properties.
        this.upgradeRewriter = findUpgradeRewriter();

        TranslateDisplayNames translateAnnotation = getClass().getAnnotation(TranslateDisplayNames.class);
        if (translateAnnotation != null)
            translationPrefix = translateAnnotation.prefix();

        // Get the default naming strategies.
        NamingStrategy serializeNaming = NamingStrategy.SNAKE_CASE;
        SerializeName.Strategy serializeNameStrategyAnnotation = getClass().getAnnotation(SerializeName.Strategy.class);
        if (serializeNameStrategyAnnotation != null)
            serializeNaming = serializeNameStrategyAnnotation.value();

        NamingStrategy displayNaming = translationPrefix == null ? NamingStrategy.SPACED_PASCAL : serializeNaming;
        DisplayName.Strategy displayNameStrategyAnnotation = getClass().getAnnotation(DisplayName.Strategy.class);
        if (displayNameStrategyAnnotation != null)
            displayNaming = displayNameStrategyAnnotation.value();

        // Construct options for all public, non-static, non-transient fields.
        for (Field field : getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
                continue;
            this.options.add(getOptionFromField(serializeNaming, displayNaming, field));
        }
    }

    private Option<?, ?> getOptionFromField(NamingStrategy serializeNaming, NamingStrategy displayNaming, Field field) {
        // Construct the value bindings from annotations, or otherwise simple reflection.
        Binding<?> serializeBinding = null;
        Binding<?> displayBinding = null;

        Rebind.Serialize serializeBindingAnnotation = field.getAnnotation(Rebind.Serialize.class);
        if (serializeBindingAnnotation != null)
            serializeBinding = getBindingFromFieldName(serializeBindingAnnotation.value());
        Rebind.Display displayBindingAnnotation = field.getAnnotation(Rebind.Display.class);
        if (displayBindingAnnotation != null)
            displayBinding = getBindingFromFieldName(displayBindingAnnotation.value());

        if (serializeBinding == null || displayBinding == null) {
            ReflectedBinding<?> reflectedBinding = new ReflectedBinding<>(field.getType(), this, field);
            if (serializeBinding == null)
                serializeBinding = reflectedBinding;
            if (displayBinding == null)
                displayBinding = reflectedBinding;
        }

        // Get the names from annotations or naming strategies.
        String serializeName;
        SerializeName serializeNameAnnotation = field.getAnnotation(SerializeName.class);
        if (serializeNameAnnotation != null) {
            serializeName = serializeNameAnnotation.value();
        } else {
            NamingStrategy strategy = serializeNaming;
            SerializeName.Strategy strategyAnnotation = field.getAnnotation(SerializeName.Strategy.class);
            if (strategyAnnotation != null)
                strategy = strategyAnnotation.value();
            serializeName = strategy.transform(field.getName());
        }

        String displayNameStr;
        DisplayName displayNameAnnotation = field.getAnnotation(DisplayName.class);
        if (displayNameAnnotation != null) {
            displayNameStr = displayNameAnnotation.value();
        } else {
            NamingStrategy strategy = displayNaming;
            DisplayName.Strategy strategyAnnotation = field.getAnnotation(DisplayName.Strategy.class);
            if (strategyAnnotation != null)
                strategy = strategyAnnotation.value();
            displayNameStr = strategy.transform(field.getName());
        }

        // Actually construct the option.
        Component displayName = this.getText("option." + displayNameStr);
        Option<?, ?> option = createOptionWithDefaultValue(serializeName, displayName, serializeBinding, displayBinding);

        Header headerAnnotation = field.getAnnotation(Header.class);
        if (headerAnnotation != null)
            option.header = this.getText("header." + headerAnnotation.value());

        // Get the controller.
        Slider.Int intSlider = field.getAnnotation(Slider.Int.class);
        if ((field.getType() == int.class || field.getType() == Integer.class) && intSlider != null)
            option.controller = new Controller.Slider<>(intSlider.min(), intSlider.max(), intSlider.step());

        Slider.Float floatSlider = field.getAnnotation(Slider.Float.class);
        if ((field.getType() == float.class || field.getType() == Float.class) && floatSlider != null)
            option.controller = new Controller.Slider<>(floatSlider.min(), floatSlider.max(), floatSlider.step());

        if (field.getType().isEnum())
            option.controller = createEnumValuesController(field.getType(), displayNaming, displayNameStr);

        return option;
    }

    private <T, D> Option<T, D> createOptionWithDefaultValue(String serializeName, Component displayName, Binding<T> serializeBinding, Binding<D> displayBinding) {
        return new Option<>(serializeName, displayName, displayBinding.get(), serializeBinding, displayBinding);
    }

    private <T> Controller.EnumValues<T> createEnumValuesController(Class<T> valueClass, NamingStrategy namingStrategy, String fieldName) {
        List<Controller.EnumValues.Value<T>> values = Arrays.stream(valueClass.getEnumConstants())
                .map((value) -> {
                    String name = namingStrategy.transform(((Enum<?>) value).name().toLowerCase());
                    Component text = this.getText("option." + fieldName + "." + name);
                    return new Controller.EnumValues.Value<>(value, text);
                })
                .toList();
        return new Controller.EnumValues<>(values);
    }

    private Binding<?> getBindingFromFieldName(String fieldName) {
        try {
            Field field = getClass().getDeclaredField(fieldName);
            if (!Binding.class.isAssignableFrom(field.getType()))
                throw new IllegalArgumentException("Rebind '" + fieldName + "' is not a valid binding");

            field.setAccessible(true);
            return (Binding<?>) field.get(this);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Field with name '" + fieldName + "' does not exist in class '" + getClass().getName() + "'", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Rebind '" + fieldName + "' is not accessible", e);
        }
    }

    private ConfigIo.@Nullable UpgradeRewriter findUpgradeRewriter() {
        ConfigIo.UpgradeRewriter upgradeRewriter = null;

        for (Method method : getClass().getDeclaredMethods()) {
            UpgradeRewriter annotation = method.getAnnotation(UpgradeRewriter.class);
            if (annotation == null)
                continue;

            if (!Modifier.isStatic(method.getModifiers()))
                throw new IllegalArgumentException("Upgrade rewriter '" + method.getName() + "' is not static");

            if (upgradeRewriter != null)
                throw new IllegalArgumentException("Config class '" + getClass().getName() + "' defines more than one upgrade rewriter");

            method.setAccessible(true);
            upgradeRewriter = new ConfigIo.UpgradeRewriter() {
                @Override
                public void upgrade(int fromVersion, JsonObject config) {
                    try {
                        method.invoke(null, fromVersion, config);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException("Failed to invoke upgrade rewriter", e);
                    }
                }

                @Override
                public int getLatestVersion() {
                    return annotation.currentVersion();
                }
            };
        }

        return upgradeRewriter;
    }
    //endregion

    //region Config IO
    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public List<Option<?, ?>> getOptions() {
        return this.options;
    }

    @Override
    public ConfigIo.@Nullable UpgradeRewriter getUpgradeRewriter() {
        return this.upgradeRewriter;
    }

    @Override
    public @Nullable String getTranslationPrefix() {
        return this.translationPrefix;
    }

    @Override
    public void save() {
        try {
            ConfigIo.saveToPath(this, Objects.requireNonNull(this.currentConfigPath));
        } catch (Exception e) {
            this.logger.error("Couldn't save config file", e);
        }
    }

    public static <T extends ReflectedConfig> T load(ConfigEnvironment environment, Class<T> configClass, String file) {
        return load(environment, configClass, file, List.of());
    }

    public static <T extends ReflectedConfig> T load(ConfigEnvironment environment, Class<T> configClass, String file, List<String> aliases) {
        try {
            Constructor<T> constructor = configClass.getConstructor();
            constructor.setAccessible(true);

            T config = constructor.newInstance();
            config.reflectOptions();

            try {
                Path confDir = environment.getConfigDir();
                Path configFile = confDir.resolve(file);
                ((ReflectedConfig) config).currentConfigPath = configFile;

                ConfigIo.loadFromPathOrSaveDefault(config, configFile,
                        aliases.stream().map(confDir::resolve).toList());
            } catch (Exception e) {
                config.getLogger().error("Couldn't load config file", e);
            }

            return config;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Config class '" + configClass.getName() + "' does not have a default constructor", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Config class '" + configClass.getName() + "' could not be instantiated", e);
        }
    }
    //endregion
}
