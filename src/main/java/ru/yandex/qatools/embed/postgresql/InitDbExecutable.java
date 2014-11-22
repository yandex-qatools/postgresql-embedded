package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.runtime.Executable;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;

import java.io.IOException;

/**
 * initdb executor
 * (helper to initialize the DB)
 */
class InitDbExecutable<C extends AbstractPostgresConfig> extends Executable<C, InitDbProcess> {

    public InitDbExecutable(Distribution distribution, C config, IRuntimeConfig runtimeConfig, IExtractedFileSet exe) {
        super(distribution, config, runtimeConfig, exe);
    }

    @Override
    protected InitDbProcess start(Distribution distribution, C config, IRuntimeConfig runtime)
            throws IOException {
        return new InitDbProcess<>(distribution, config, runtime, this);
    }


    @Override
    public synchronized void stop() {
        // We don't want to cleanup after this particular single invocation
    }
}