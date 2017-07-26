package ru.yandex.qatools.embed.postgresql;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPostgresStoredData {
	static File baseDir = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
		baseDir = Files.createTempDirectory("data").toFile();
		baseDir.deleteOnExit();
	}

	@Test
	public void testIndependent() throws Exception {
		final String dataDir = Files.createTempDirectory("data").toFile().getAbsolutePath();

		{
			final EmbeddedPostgres embeddedPostgres = new EmbeddedPostgres(dataDir);
			final String           jdbcUrl          = embeddedPostgres.start();
			try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
				initSchema(connection);
				checkRecords(connection);
			} finally {
				embeddedPostgres.stop();
			}
		}
		{
			final EmbeddedPostgres embeddedPostgres = new EmbeddedPostgres(dataDir);
			final String           jdbcUrl          = embeddedPostgres.start();
			try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
				checkRecords(connection);
			} finally {
				embeddedPostgres.stop();
			}
		}
	}

	@Test
	public void testStep0() throws Exception {
		final EmbeddedPostgres embeddedPostgres = new EmbeddedPostgres(baseDir.getAbsolutePath());
		final String           jdbcUrl          = embeddedPostgres.start();
		try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
			initSchema(connection);
			checkRecords(connection);
		} finally {
			embeddedPostgres.stop();
		}
	}

	@Test
	public void testStep1() throws Exception {
		final EmbeddedPostgres embeddedPostgres = new EmbeddedPostgres(Version.Main.PRODUCTION, baseDir.getAbsolutePath());
		final String           jdbcUrl          = embeddedPostgres.start();
		try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
			checkRecords(connection);
		} finally {
			embeddedPostgres.stop();
		}
	}

	private void initSchema(Connection conn) throws SQLException {
		assertThat(conn, not(nullValue()));
		assertThat(conn.createStatement().execute("CREATE TABLE films (code CHAR(5));"), is(false));
		assertThat(conn.createStatement().execute("INSERT INTO films VALUES ('movie');"), is(false));
	}

	private void checkRecords(Connection conn) throws SQLException {
		final Statement statement = conn.createStatement();
		assertThat(statement.execute("SELECT * FROM films;"), is(true));
		assertThat(statement.getResultSet().next(), is(true));
		assertThat(statement.getResultSet().getString("code"), is("movie"));
	}

}
