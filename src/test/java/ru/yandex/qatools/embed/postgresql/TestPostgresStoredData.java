package ru.yandex.qatools.embed.postgresql;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.*;
import static ru.yandex.qatools.embed.postgresql.util.SocketUtil.findFreePort;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPostgresStoredData {
	static File baseDir = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
		baseDir = Files.createTempDirectory("data").toFile();
		baseDir.deleteOnExit();
	}

	@Test
	public void testStep0() throws Exception {
		final PostgresProcess process = buildProcess(baseDir);
		Assert.assertTrue(process.getConfig().storage().dbDir().exists());
		Connection connection = DriverManager.getConnection(formatConnUrl(process.getConfig()));
		initSchema(connection);
		checkRecords(connection);
		process.stop();
	}

	@Test
	public void testStep1() throws Exception {
		final PostgresProcess process = buildProcess(baseDir);
		Assert.assertTrue(process.getConfig().storage().dbDir().exists());
		Connection connection = DriverManager.getConnection(formatConnUrl(process.getConfig()));
		checkRecords(connection);
		process.stop();
	}

	@Test
	public void testStep2() throws Exception {
		final EmbeddedPostgres embeddedPostgres = new EmbeddedPostgres(baseDir.getAbsolutePath());
		final String           jdbcUrl          = embeddedPostgres.start();
		try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
			checkRecords(connection);
		} finally {
			embeddedPostgres.stop();
		}
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
	public void testStep3() throws Exception {
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

	private PostgresProcess buildProcess(File baseDir) throws IOException {
		final PostgresStarter<PostgresExecutable, PostgresProcess> runtime = PostgresStarter.getInstance(EmbeddedPostgres.defaultRuntimeConfig());
		PostgresConfig config = new PostgresConfig(Version.Main.PRODUCTION,
		                                           new AbstractPostgresConfig.Net(DEFAULT_HOST, findFreePort()),
		                                           new AbstractPostgresConfig.Storage(DEFAULT_DB_NAME, baseDir.getPath()),
		                                           new AbstractPostgresConfig.Timeout(),
		                                           new AbstractPostgresConfig.Credentials(DEFAULT_USER, DEFAULT_PASSWORD)
		);
		PostgresExecutable exec = runtime.prepare(config);
		return exec.start();
	}

	private String formatConnUrl(PostgresConfig config) {
		return format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
		              config.net().host(),
		              config.net().port(),
		              config.storage().dbName(),
		              config.credentials().username(),
		              config.credentials().password()
		             );
	}
}
