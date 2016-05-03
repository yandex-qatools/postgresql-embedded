package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.runtime.AbstractProcess;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.IStopable;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;

import static java.io.File.*;
import static org.apache.commons.lang3.SystemUtils.JAVA_HOME;

public abstract class AbstractPGProcess<E extends Executable<PostgresConfig, P>, P extends IStopable>
        extends AbstractProcess<PostgresConfig, E, P> {

    public AbstractPGProcess(Distribution distribution, PostgresConfig config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected void onBeforeProcessStart(ProcessBuilder processBuilder, PostgresConfig config, IRuntimeConfig runtimeConfig) {
        if (config.credentials() != null) {
            processBuilder.environment().put("PGUSER", config.credentials().username());
            processBuilder.environment().put("PGPASSWORD", config.credentials().password());
        }
        processBuilder.environment().put("PATH",
                processBuilder.environment().get("PATH") + pathSeparatorChar
                        + JAVA_HOME + separator + "bin"
                        + pathSeparatorChar
                        + JAVA_HOME + separator + "jre" + separator + "bin"
        );
    }

    @Override
    protected void stopInternal() {

    }

    @Override
    protected void cleanupInternal() {

    }
}