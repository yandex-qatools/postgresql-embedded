package ru.yandex.qatools.embed.postgresql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Statement;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Ilya Sadykov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:test-spring-context.xml"})
public class TestPostgresWithinSpringContext {

    @Autowired
    PostgresqlService service;

    @Test
    public void testPostgresWithinSpring() throws Exception {
        assertThat(service.getConn(), not(nullValue()));
        assertThat(service.getConn().createStatement().execute("CREATE TABLE films (code char(5));"), is(false));
        assertThat(service.getConn().createStatement().execute("INSERT INTO films VALUES ('movie');"), is(false));
        final Statement statement = service.getConn().createStatement();
        assertThat(statement.execute("SELECT * FROM films;"), is(true));
        assertThat(statement.getResultSet().next(), is(true));
        assertThat(statement.getResultSet().getString("code"), is("movie"));

    }
}
