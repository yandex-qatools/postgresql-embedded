package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import ru.yandex.qatools.embed.postgresql.config.DownloadConfigBuilder;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.CachedArtifactStoreBuilder;

import java.nio.file.Paths;

public class TestPostgresCachedDirStarter extends TestPostgresStarter {

    @Override
    protected IRuntimeConfig buildRuntimeConfig() {
        // turns off the default functionality of unzipping on every run.
        final String tmpDir = Paths.get(System.getProperty("java.io.tmpdir"), "pgembed").toFile().getPath();
        final Command cmd = Command.Postgres;
        final FixedPath cachedDir = new FixedPath(tmpDir);
        return new RuntimeConfigBuilder()
                .defaults(cmd)
                .artifactStore(new CachedArtifactStoreBuilder()
                        .defaults(cmd)
                        .tempDir(cachedDir)
                        .download(new DownloadConfigBuilder()
                                .defaultsForCommand(cmd)
                                .packageResolver(new PackagePaths(cmd, cachedDir))
                                .build()))
                .build();
    }
}
