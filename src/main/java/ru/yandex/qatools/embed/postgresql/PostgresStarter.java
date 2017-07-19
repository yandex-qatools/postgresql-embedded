package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.io.Slf4jLevel;
import de.flapdoodle.embed.process.io.Slf4jStreamProcessor;
import de.flapdoodle.embed.process.runtime.Starter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;
import ru.yandex.qatools.embed.postgresql.ext.LogWatchStreamProcessor;

import java.lang.reflect.Constructor;
import java.util.HashSet;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Starter for every pg process
 */
public class PostgresStarter<E extends AbstractPGExecutable<PostgresConfig, P>, P extends AbstractPGProcess<E, P>>
        extends Starter<PostgresConfig, E, P> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresStarter.class);
    final Class<E> execClass;

    public PostgresStarter(final Class<E> execClass, final IRuntimeConfig runtimeConfig) {
        super(runtimeConfig);
        this.execClass = execClass;
    }

    public static PostgresStarter<PostgresExecutable, PostgresProcess> getInstance(IRuntimeConfig config) {
        return new PostgresStarter<>(PostgresExecutable.class, config);
    }

    public static PostgresStarter<PostgresExecutable, PostgresProcess> getDefaultInstance() {
        return getInstance(runtimeConfig(Command.Postgres));
    }

    public static IRuntimeConfig runtimeConfig(Command cmd) {
        LogWatchStreamProcessor logWatch = new LogWatchStreamProcessor(
                "started", new HashSet<>(singletonList("failed")),
                new Slf4jStreamProcessor(getLogger("postgres"), Slf4jLevel.TRACE));
        return new RuntimeConfigBuilder()
                .defaults(cmd)
                .processOutput(new ProcessOutput(logWatch, logWatch, logWatch)).build();
    }

    public static <E extends AbstractPGExecutable<PostgresConfig, P>, P extends AbstractPGProcess<E, P>>
    PostgresStarter<E, P> getCommand(Command command, IRuntimeConfig config) {
        return new PostgresStarter<>(command.executableClass(), config);
    }

    public static <E extends AbstractPGExecutable<PostgresConfig, P>, P extends AbstractPGProcess<E, P>>
    PostgresStarter<E, P> getCommand(Command command) {
        return getCommand(command, runtimeConfig(command));
    }

    @Override
    protected E newExecutable(PostgresConfig config, Distribution distribution,
                              IRuntimeConfig runtime, IExtractedFileSet exe) {
        try {
            Constructor<E> c = execClass.getConstructor(
                    Distribution.class, PostgresConfig.class,
                    IRuntimeConfig.class, IExtractedFileSet.class
            );
            return c.newInstance(distribution, config, runtime, exe);
        } catch (Exception e) {
            LOGGER.warn("Exception while trying to create executable", e);
            throw new RuntimeException("Failed to initialize the executable (" + e.getMessage() + ")", e);
        }
    }
}
