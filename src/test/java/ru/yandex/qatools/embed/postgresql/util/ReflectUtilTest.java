package ru.yandex.qatools.embed.postgresql.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.embed.postgresql.util.ReflectUtil.setFinalField;

/**
 * @author Ilya Sadykov
 */
public class ReflectUtilTest {

    @Test
    public void changeFalseToTrueShouldBeTrue() throws Exception {
        final C obj = new C(false);
        setFinalField(obj, "value", true);
        assertThat(obj.value, is(true));
    }

    @Test
    public void changeTrueToFalseShouldBeFalse() throws Exception {
        final C obj = new C(true);
        setFinalField(obj, "value", false);
        assertThat(obj.value, is(false));
    }

    class C {
        private final boolean value;

        C(boolean value) {
            this.value = value;
        }
    }
}