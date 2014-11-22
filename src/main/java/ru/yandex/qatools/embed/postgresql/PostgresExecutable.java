package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.runtime.Executable;
import ru.yandex.qatools.embed.postgresql.config.PostgresqlConfig;
import ru.yandex.qatools.embed.postgresql.ext.PostgresArtifactStore;

import java.io.IOException;

import static de.flapdoodle.embed.process.io.file.Files.forceDelete;

/**
 * postgres executable
 */
public class PostgresExecutable extends Executable<PostgresqlConfig, PostgresProcess> {
    final IRuntimeConfig runtimeConfig;

    public PostgresExecutable(Distribution distribution,
                              PostgresqlConfig config, IRuntimeConfig runtimeConfig, IExtractedFileSet exe) {
        super(distribution, config, runtimeConfig, exe);
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    protected PostgresProcess start(Distribution distribution, PostgresqlConfig config, IRuntimeConfig runtime)
            throws IOException {
        return new PostgresProcess(distribution, config, runtime, this);
    }

    @Override
    public synchronized void stop() {
        try {
            super.stop();
        } catch (Exception ignored) {
        }
        if (runtimeConfig.getArtifactStore() instanceof PostgresArtifactStore) {
            forceDelete(((PostgresArtifactStore) runtimeConfig.getArtifactStore()).getTempDir().asFile());
        }
    }
}