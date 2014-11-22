package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IExecutableProcessConfig;
import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.runtime.Executable;
import de.flapdoodle.embed.process.runtime.Starter;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;
import ru.yandex.qatools.embed.postgresql.config.PostgresqlConfig;
import ru.yandex.qatools.embed.postgresql.config.RuntimeConfigBuilder;

import java.lang.reflect.Constructor;


/**
 * Starter for every pg process
 */
public class PostgresStarter extends Starter<PostgresqlConfig, PostgresExecutable, PostgresProcess> {

    public PostgresStarter(IRuntimeConfig config) {
        super(config);
    }

    public static PostgresStarter getInstance(IRuntimeConfig config) {
        return new PostgresStarter(config);
    }

    public static PostgresStarter getDefaultInstance() {
        return getInstance(new RuntimeConfigBuilder().defaults(Command.Postgres).build());
    }

    @Override
    protected PostgresExecutable newExecutable(PostgresqlConfig config, Distribution distribution,
                                                 IRuntimeConfig runtime, IExtractedFileSet exe) {
        return new PostgresExecutable(distribution, config, runtime, exe);
    }


    /**
     * A bit hacky starter factory
     */
    @SuppressWarnings("unchecked")
    static <E extends Executable<C, P>,
            P extends AbstractPGProcess, C extends AbstractPostgresConfig> Starter<C, E, P>
    getStarter(final Class<E> execClass, final IRuntimeConfig runtimeConfig) {
        return new Starter(runtimeConfig) {
            @Override
            protected Executable newExecutable(IExecutableProcessConfig config,
                                               Distribution distribution, IRuntimeConfig runtime,
                                               IExtractedFileSet exe) {
                try {
                    Constructor<E> c = execClass.getConstructor(
                            Distribution.class, AbstractPostgresConfig.class,
                            IRuntimeConfig.class, IExtractedFileSet.class
                    );
                    return c.newInstance(distribution, config, runtime, exe);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize the executable", e);
                }
            }
        };
    }
}
