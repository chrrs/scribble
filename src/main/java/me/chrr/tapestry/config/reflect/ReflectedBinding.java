package me.chrr.tapestry.config.reflect;

import me.chrr.tapestry.config.Binding;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@NullMarked
public final class ReflectedBinding<T> implements Binding<T> {
    private final String fieldName;
    private final Class<T> valueClass;
    private final Object object;
    private final Field field;

    public ReflectedBinding(Class<T> valueClass, Object object, Field field) {
        this.fieldName = field.getDeclaringClass().getTypeName() + "." + field.getName();
        this.valueClass = valueClass;
        this.object = object;
        this.field = field;

        if (Modifier.isFinal(field.getModifiers()))
            throw new IllegalArgumentException("Reflected binding '" + this.fieldName + "' is final");
        if (!field.getType().isAssignableFrom(valueClass))
            throw new IllegalArgumentException("Reflected binding '" + this.fieldName + "' is not of type '" + valueClass.getName() + "'");
        if (!field.getDeclaringClass().isAssignableFrom(object.getClass()))
            throw new IllegalArgumentException("Reflected binding '" + this.fieldName + "' is incompatible with '" + object.getClass().getName() + "'");
        if (field.getName().equals("version"))
            throw new IllegalArgumentException("Reflected binding '" + this.fieldName + "' uses the reserved name 'version'");

        field.setAccessible(true);
    }

    @Override
    public Class<T> getValueClass() {
        return this.valueClass;
    }

    @Override
    public T get() {
        try {
            //noinspection unchecked: we check if the type is correct in the constructor.
            return (T) this.field.get(this.object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Reflected binding '" + this.fieldName + "' is not accessible", e);
        }
    }

    @Override
    public void set(T value) {
        try {
            this.field.set(this.object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Reflected binding " + this.fieldName + " is not accessible", e);
        }
    }
}
