package ru.yandex.qatools.embed.postgresql.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * @author Ilya Sadykov
 */
public final class ReflectUtil {

    private ReflectUtil() {
    }

    public static void setFinalField(Object object, String fieldName, Object newValue)//NOSONAR
            throws NoSuchFieldException, IllegalAccessException {
        assert object != null;
        assert fieldName != null;
        final Field field = getFieldFromClassHierarchy(object.getClass(), fieldName)
                .orElseThrow(NoSuchFieldError::new);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(object, newValue);
    }

    /**
     * Searches for a field within class hierarchy
     *
     * @return non empty field if found and empty otherwise
     */
    public static Optional<Field> getFieldFromClassHierarchy(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    return Optional.of(field);
                }
            }
            clazz = clazz.getSuperclass(); //NOSONAR
        }
        return Optional.empty();
    }

}
