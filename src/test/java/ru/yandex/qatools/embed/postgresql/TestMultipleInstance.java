package ru.yandex.qatools.embed.postgresql;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.distribution.Version;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TestMultipleInstance {
	@Test
	public void testRunTwoConsistentlyInstanceDifferenceVersions() throws Exception {
		final EmbeddedPostgres postgres0 = new EmbeddedPostgres();
		postgres0.start();
		Assert.assertTrue(postgres0.getConnectionUrl().isPresent());
		checkVersion(postgres0.getConnectionUrl().get(), "PostgreSQL 9.6");
		postgres0.stop();

		final EmbeddedPostgres postgres1 = new EmbeddedPostgres(Version.Main.V9_5);
		postgres1.start();
		Assert.assertTrue(postgres1.getConnectionUrl().isPresent());
		checkVersion(postgres1.getConnectionUrl().get(), "PostgreSQL 9.5");
		postgres1.stop();
	}

	@Test
	public void testRunTwoInstanceAtOne() throws Exception {
		final EmbeddedPostgres postgres0 = new EmbeddedPostgres();
		postgres0.start();
		Assert.assertTrue(postgres0.getConnectionUrl().isPresent());
		checkVersion(postgres0.getConnectionUrl().get(), "PostgreSQL 9.6");

		final EmbeddedPostgres postgres1 = new EmbeddedPostgres();
		postgres1.start();
		Assert.assertTrue(postgres1.getConnectionUrl().isPresent());
		checkVersion(postgres1.getConnectionUrl().get(), "PostgreSQL 9.6");

		postgres0.stop();
		postgres1.stop();
	}

	@Test
	public void testRunTwoInstanceAtOneDifferenceVersions() throws Exception {
		final EmbeddedPostgres postgres0 = new EmbeddedPostgres(Version.Main.V9_5);
		postgres0.start();
		Assert.assertTrue(postgres0.getConnectionUrl().isPresent());

		final EmbeddedPostgres postgres1 = new EmbeddedPostgres(Version.Main.V9_6);
		postgres1.start();
		Assert.assertTrue(postgres1.getConnectionUrl().isPresent());

		checkVersion(postgres0.getConnectionUrl().get(), "PostgreSQL 9.5");
		checkVersion(postgres1.getConnectionUrl().get(), "PostgreSQL 9.6");

		postgres0.stop();
		postgres1.stop();
	}

	private void checkVersion(String jdbcUrl, String expectedVersion) throws Exception {
		try (final Connection conn = DriverManager.getConnection(jdbcUrl);
		     final Statement statement = conn.createStatement()) {
			Assert.assertTrue(statement.execute("SELECT version();"));
			Assert.assertTrue(statement.getResultSet().next());
			Assert.assertTrue(statement.getResultSet().getString("version").contains(expectedVersion));
		}
	}
}
