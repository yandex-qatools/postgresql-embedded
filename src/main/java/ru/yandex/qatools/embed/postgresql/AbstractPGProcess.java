package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.runtime.AbstractProcess;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.IStopable;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;

import java.io.IOException;

public abstract class AbstractPGProcess<C extends AbstractPostgresConfig,
        E extends Executable<C, P>,
        P extends IStopable>
        extends AbstractProcess<C, E, P> {

    public AbstractPGProcess(Distribution distribution, C config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected void onBeforeProcessStart(ProcessBuilder processBuilder, C config, IRuntimeConfig runtimeConfig) {
        if (config.credentials() != null) {
            processBuilder.environment().put("PGUSER", config.credentials().username());
            processBuilder.environment().put("PGPASSWORD", config.credentials().password());
        }
    }

    @Override
    protected void stopInternal() {

    }

    @Override
    protected void cleanupInternal() {

    }
}