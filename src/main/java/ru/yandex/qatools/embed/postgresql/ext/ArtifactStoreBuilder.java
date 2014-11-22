package ru.yandex.qatools.embed.postgresql.ext;

import de.flapdoodle.embed.process.builder.TypedProperty;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.extract.ITempNaming;
import de.flapdoodle.embed.process.extract.UUIDTempNaming;
import de.flapdoodle.embed.process.io.directories.IDirectory;
import de.flapdoodle.embed.process.store.Downloader;
import de.flapdoodle.embed.process.store.IArtifactStore;
import de.flapdoodle.embed.process.store.IDownloader;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;

public class ArtifactStoreBuilder extends
        de.flapdoodle.embed.process.store.ArtifactStoreBuilder {
    private static final TypedProperty<ITempNaming> EXECUTABLE_NAMING = TypedProperty.with("ExecutableNaming", ITempNaming.class);
    private static final TypedProperty<IDirectory> TEMP_DIR_FACTORY = TypedProperty.with("TempDir", IDirectory.class);
    private static final TypedProperty<IDownloadConfig> DOWNLOAD_CONFIG = TypedProperty.with("DownloadConfig", IDownloadConfig.class);
    private static final TypedProperty<IDownloader> DOWNLOADER = TypedProperty.with("Downloader", IDownloader.class);

    public ArtifactStoreBuilder defaults(Command command) {
        tempDir().setDefault(new SubdirTempDir());
        executableNaming().setDefault(new UUIDTempNaming());
        download().setDefault(new DownloadConfigBuilder().defaultsForCommand(command).build());
        downloader().setDefault(new Downloader());
        return this;
    }

    @Override
    public IArtifactStore build() {
        return new PostgresArtifactStore(get(DOWNLOAD_CONFIG), get(TEMP_DIR_FACTORY), get(EXECUTABLE_NAMING), get(DOWNLOADER));
    }

    public ArtifactStoreBuilder defaultsWithoutCache(Command command) {
        tempDir().setDefault(new SubdirTempDir());
        executableNaming().setDefault(new UUIDTempNaming());
        download().setDefault(
                new DownloadConfigBuilder().defaultsForCommand(command)
                        .build());
        downloader().setDefault(new Downloader());
        // disable caching
        useCache().setDefault(false);
        return this;
    }
}
