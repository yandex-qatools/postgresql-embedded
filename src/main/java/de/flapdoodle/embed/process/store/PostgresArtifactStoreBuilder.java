package de.flapdoodle.embed.process.store;

import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.config.PostgresDownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.SubdirTempDir;

public class PostgresArtifactStoreBuilder extends
        de.flapdoodle.embed.process.store.ArtifactStoreBuilder {

    public PostgresArtifactStoreBuilder defaults(Command command) {
        tempDir().setDefault(new SubdirTempDir());
        executableNaming().setDefault(new UUIDTempNaming());
        download().setDefault(new PostgresDownloadConfigBuilder().defaultsForCommand(command).build());
        downloader().setDefault(new Downloader());
        return this;
    }

    @Override
    public IArtifactStore build() {
        return new CachedPostgresArtifactStore(get(DOWNLOAD_CONFIG), get(TEMP_DIR_FACTORY), get(EXECUTABLE_NAMING), get(DOWNLOADER));
    }

}
