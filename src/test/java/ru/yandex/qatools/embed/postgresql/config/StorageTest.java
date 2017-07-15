package ru.yandex.qatools.embed.postgresql.config;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

public class StorageTest {

	@Test
	public void testTwoStorage() throws Exception {
		final AbstractPostgresConfig.Storage storage0 = new AbstractPostgresConfig.Storage(EmbeddedPostgres.DEFAULT_DB_NAME);
		Assert.assertTrue(storage0.dbDir().exists());

		final AbstractPostgresConfig.Storage storage1 = new AbstractPostgresConfig.Storage(EmbeddedPostgres.DEFAULT_DB_NAME);
		Assert.assertTrue(storage1.dbDir().exists());

		Assert.assertNotEquals(storage0.dbDir().getPath(), storage1.dbDir().getPath());
	}
}