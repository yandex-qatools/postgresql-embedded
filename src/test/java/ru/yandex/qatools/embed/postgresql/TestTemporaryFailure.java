package ru.yandex.qatools.embed.postgresql;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestTemporaryFailure {

    @Test
    public void itShouldFail() throws Exception {
        assertThat(false, equalTo(true));
    }
}
