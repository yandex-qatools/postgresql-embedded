package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;

/**
 * psql executor
 * (helper to initialize the DB)
 */
public class CreateuserExecutable extends AbstractPGExecutable<PostgresConfig, CreateuserProcess> {

    public CreateuserExecutable(Distribution distribution,
                                PostgresConfig config, IRuntimeConfig runtimeConfig, IExtractedFileSet redisdExecutable) {
        super(distribution, config, runtimeConfig, redisdExecutable);
    }

    @Override
    protected CreateuserProcess start(Distribution distribution, PostgresConfig config, IRuntimeConfig runtime)
            throws IOException {
        return new CreateuserProcess<>(distribution, config, runtime, this);
    }

    @Override
    public synchronized void stop() {
        // We don't want to cleanup after this particular single invocation
    }
}