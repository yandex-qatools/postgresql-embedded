package ru.yandex.qatools.embed.postgresql;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.config.PostgresqlConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PostgresqlStarterTest {

    private String url;
    private PostgresProcess process;
    private Connection conn;

    @Before
    public void setUp() throws Exception {
        PostgresStarter runtime = PostgresStarter.getDefaultInstance();
        final PostgresqlConfig configDb = PostgresqlConfig.defaultWithDbName("test");
        PostgresExecutable exec = runtime.prepare(configDb);
        process = exec.start();
        url = format("jdbc:postgresql://%s:%s/%s",
                configDb.net().getServerAddress().getHostAddress(),
                configDb.net().port(),
                configDb.storage().dbName()
        );
        conn = DriverManager.getConnection(url);
    }

    @After
    public void tearDown() throws Exception {
        conn.close();
        process.stop();
    }

    @Test
    public void testPostgres() throws Exception {
        assertThat(conn, not(nullValue()));
        assertThat(conn.createStatement().execute("CREATE TABLE films (code char(5));"), is(false));
        assertThat(conn.createStatement().execute("INSERT INTO films VALUES ('movie');"), is(false));
        final Statement statement = conn.createStatement();
        assertThat(statement.execute("SELECT * FROM films;"), is(true));
        assertThat(statement.getResultSet().next(), is(true));
    }
}