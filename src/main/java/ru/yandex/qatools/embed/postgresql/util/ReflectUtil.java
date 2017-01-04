package ru.yandex.qatools.embed.postgresql.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author Ilya Sadykov
 */
public final class ReflectUtil {

    private ReflectUtil() {
    }

    public static void setFinalField(Object object, String fieldName, Object newValue)
            throws NoSuchFieldException, IllegalAccessException { //NOSONAR
        assert object != null;
        assert fieldName != null;
        final Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(object, newValue);
    }
}
