package ru.yandex.qatools.embed.postgresql;

import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class TestMultipleInstance {
    @Test
    public void itShouldAllowToRunTwoInstancesWithDifferentVersions() throws Exception {
        final EmbeddedPostgres postgres0 = new EmbeddedPostgres();
        postgres0.start();
        assertThat(postgres0.getConnectionUrl().isPresent(), is(true));
        checkVersion(postgres0.getConnectionUrl().get(), "PostgreSQL 9.6");
        postgres0.stop();

        final EmbeddedPostgres postgres1 = new EmbeddedPostgres(Version.Main.V9_5);
        postgres1.start();
        assertThat(postgres1.getConnectionUrl().isPresent(), is(true));
        checkVersion(postgres1.getConnectionUrl().get(), "PostgreSQL 9.5");
        postgres1.stop();
    }

    @Test
    public void itShouldAllowToRunTwoInstancesAtSameTime() throws Exception {
        final EmbeddedPostgres postgres0 = new EmbeddedPostgres();
        postgres0.start();
        assertThat(postgres0.getConnectionUrl().isPresent(), is(true));

        final EmbeddedPostgres postgres1 = new EmbeddedPostgres();
        postgres1.start();
        assertThat(postgres1.getConnectionUrl().isPresent(), is(true));

        checkVersion(postgres0.getConnectionUrl().get(), "PostgreSQL 9.6");
        checkVersion(postgres1.getConnectionUrl().get(), "PostgreSQL 9.6");

        postgres0.stop();
        postgres1.stop();
    }

    @Test
    public void itShouldAllowToRunTwoInstancesAtSameTimeAndWithDifferentVersions() throws Exception {
        final EmbeddedPostgres postgres0 = new EmbeddedPostgres(Version.Main.V9_5);
        postgres0.start();
        assertThat(postgres0.getConnectionUrl().isPresent(), is(true));

        final EmbeddedPostgres postgres1 = new EmbeddedPostgres(Version.Main.V9_6);
        postgres1.start();
        assertThat(postgres1.getConnectionUrl().isPresent(), is(true));

        checkVersion(postgres0.getConnectionUrl().get(), "PostgreSQL 9.5");
        checkVersion(postgres1.getConnectionUrl().get(), "PostgreSQL 9.6");

        postgres0.stop();
        postgres1.stop();
    }

    private void checkVersion(String jdbcUrl, String expectedVersion) throws Exception {
        try (final Connection conn = DriverManager.getConnection(jdbcUrl);
             final Statement statement = conn.createStatement()) {
            assertThat(statement.execute("SELECT version();"), is(true));
            assertThat(statement.getResultSet().next(), is(true));
            assertThat(statement.getResultSet().getString("version"), containsString(expectedVersion));
        }
    }
}
