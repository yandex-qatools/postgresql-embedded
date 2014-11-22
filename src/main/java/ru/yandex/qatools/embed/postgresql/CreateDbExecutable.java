package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;

/**
 * createdb executor
 * (helper to initialize the DB)
 */
class CreateDbExecutable extends AbstractPGExecutable<PostgresConfig, CreateDbProcess> {

    public CreateDbExecutable(Distribution distribution,
                              PostgresConfig config, IRuntimeConfig runtimeConfig, IExtractedFileSet redisdExecutable) {
        super(distribution, config, runtimeConfig, redisdExecutable);
    }

    @Override
    protected CreateDbProcess start(Distribution distribution, PostgresConfig config, IRuntimeConfig runtime)
            throws IOException {
        return new CreateDbProcess<>(distribution, config, runtime, this);
    }

    @Override
    public synchronized void stop() {
        // We don't want to cleanup after this particular single invocation
    }
}