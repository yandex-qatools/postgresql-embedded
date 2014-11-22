package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.runtime.Starter;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;

import java.lang.reflect.Constructor;


/**
 * Starter for every pg process
 */
public class PostgresStarter<E extends AbstractPGExecutable<PostgresConfig, P>, P extends AbstractPGProcess<E, P>>
        extends Starter<PostgresConfig, E, P> {
    final Class<E> execClass;

    public PostgresStarter(final Class<E> execClass, final IRuntimeConfig runtimeConfig) {
        super(runtimeConfig);
        this.execClass = execClass;
    }

    public static PostgresStarter<PostgresExecutable, PostgresProcess> getInstance(IRuntimeConfig config) {
        return new PostgresStarter(PostgresExecutable.class, config);
    }

    public static PostgresStarter<PostgresExecutable, PostgresProcess> getDefaultInstance() {
        return getInstance(new RuntimeConfigBuilder().defaults(Command.Postgres).build());
    }

    public static <E extends AbstractPGExecutable<PostgresConfig, P>, P extends AbstractPGProcess<E, P>>
    PostgresStarter<E, P> getCommand(Command command, IRuntimeConfig config) {
        return new PostgresStarter(command.executableClass(), config);
    }

    public static <E extends AbstractPGExecutable<PostgresConfig, P>, P extends AbstractPGProcess<E, P>>
    PostgresStarter<E, P> getCommand(Command command) {
        return getCommand(command, new RuntimeConfigBuilder().defaults(command).build());
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
            throw new RuntimeException("Failed to initialize the executable", e);
        }
    }
}
