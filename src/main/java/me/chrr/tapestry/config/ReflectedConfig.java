package me.chrr.tapestry.config;

import com.google.gson.JsonObject;
import me.chrr.tapestry.config.annotation.*;
import me.chrr.tapestry.config.value.Constraint;
import me.chrr.tapestry.config.value.TrackedValue;
import me.chrr.tapestry.config.value.Value;
import me.chrr.tapestry.config.value.VirtualValue;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

@NullMarked
public abstract class ReflectedConfig implements Config {
    private final List<Option<?>> options = new ArrayList<>();

    private Component title = Component.empty();
    private ConfigIo.@Nullable UpgradeRewriter upgradeRewriter = null;

    private @Nullable Path currentConfigPath = null;
    private @Nullable TranslationPrefix translationPrefix = null;

    //region Value constructors
    protected <T> Value<T> value(T defaultValue) {
        //noinspection unchecked: this should be correct.
        return new TrackedValue<>((Class<T>) defaultValue.getClass(), defaultValue);
    }

    protected <U, V> Value<V> map(Value<U> value, Function<U, V> aToB, Function<V, U> bToA) {
        V defaultValue = aToB.apply(value.getDefaultValue());

        //noinspection unchecked: this should be correct.
        return new VirtualValue<>((Class<V>) defaultValue.getClass(), defaultValue,
                () -> aToB.apply(value.get()), (v) -> value.set(bToA.apply(v)));
    }
    //endregion

    //region Initialization & reflection
    private void reflectOptions() {
        this.upgradeRewriter = reflectUpgradeRewriter();
        this.translationPrefix = getClass().getAnnotation(TranslationPrefix.class);

        // Get the default naming strategy.
        NamingStrategy namingStrategy = NamingStrategy.SNAKE_CASE;
        SerializeName.Strategy serializeNameStrategyAnnotation = getClass().getAnnotation(SerializeName.Strategy.class);
        if (serializeNameStrategyAnnotation != null)
            namingStrategy = serializeNameStrategyAnnotation.value();

        // Get the config screen title.
        this.title = getTranslatedName("title");

        // Construct options for all public, non-static, non-transient fields.
        for (Field field : getClass().getFields()) {
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isTransient(field.getModifiers()))
                continue;
            this.options.add(reflectOptionFromField(field, namingStrategy));
        }
    }

    private ConfigIo.@Nullable UpgradeRewriter reflectUpgradeRewriter() {
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

    private Option<?> reflectOptionFromField(Field field, NamingStrategy defaultNamingStrategy) {
        try {
            Class<?> type = field.getType();
            if (!Value.class.isAssignableFrom(type))
                throw new IllegalArgumentException("All (non-transient) public fields in a config class should be Value<?>, which " + type + " isn't");

            Value<?> value = (Value<?>) field.get(this);
            return reflectOptionFromValue(value, field, defaultNamingStrategy);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't initialize config field " + field);
        }
    }

    private <T> Option<T> reflectOptionFromValue(Value<T> value, Field field, NamingStrategy defaultNamingStrategy) {
        Option<T> option = new Option<>(value);

        SerializeName serializeName = field.getAnnotation(SerializeName.class);
        SerializeName.Strategy strategy = getClass().getAnnotation(SerializeName.Strategy.class);
        NamingStrategy namingStrategy = strategy != null ? strategy.value() : defaultNamingStrategy;

        // Serialization options.
        if (value instanceof TrackedValue<T> trackedValue) {
            option.serializedName = serializeName != null ? serializeName.value() : namingStrategy.transform(field.getName());

            // Fill in some value properties if it's an enum.
            Class<T> valueType = trackedValue.getValueType();
            if (valueType.isEnum()) {
                if (value.constraint == null)
                    value.constraint = new Constraint.Values<>(Arrays.asList(valueType.getEnumConstants()));

                if (!value.didSetTextProvider)
                    value.textProvider = (v) -> getTranslatedName(
                            "value." + namingStrategy.transform(valueType.getSimpleName())
                                    + "." + namingStrategy.transform(v.toString()));
            }
        }

        // GUI-specific options.
        if (field.isAnnotationPresent(Hidden.class)) {
            option.hidden = true;
        } else {
            option.displayName = getTranslatedName("option." + namingStrategy.transform(field.getName()));
        }

        Header header = field.getAnnotation(Header.class);
        if (header != null)
            option.header = getTranslatedName("category." + header.value());

        return option;
    }

    private Component getTranslatedName(String key) {
        if (this.translationPrefix == null) {
            return Component.literal(key);
        } else {
            return Component.translatable(this.translationPrefix.value() + "." + key);
        }
    }
    //endregion

    //region Saving & loading
    @Override
    public void save() {
        ConfigIo.saveToPath(this, Objects.requireNonNull(this.currentConfigPath));
    }

    public static <T extends ReflectedConfig> T load(ConfigEnvironment environment, Class<T> configClass, String file, List<String> aliases) {
        try {
            Constructor<T> constructor = configClass.getConstructor();
            constructor.setAccessible(true);

            T config = constructor.newInstance();
            ((ReflectedConfig) config).reflectOptions();

            Path confDir = environment.getConfigDir();
            Path configFile = confDir.resolve(file);
            ((ReflectedConfig) config).currentConfigPath = configFile;

            ConfigIo.loadFromPathOrSaveDefault(config, configFile,
                    aliases.stream().map(confDir::resolve).toList());

            return config;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Config class '" + configClass.getName() + "' does not have a default constructor", e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Config class '" + configClass.getName() + "' could not be instantiated", e);
        }
    }
    //endregion

    //region Interface implementations
    @Override
    public Collection<Option<?>> getOptions() {
        return this.options;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public ConfigIo.@Nullable UpgradeRewriter getUpgradeRewriter() {
        return this.upgradeRewriter;
    }
    //endregion
}
