package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import de.flapdoodle.embed.process.runtime.Executable;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;

public abstract class AbstractPGExecutable<C extends AbstractPostgresConfig, P extends AbstractPGProcess>
        extends Executable<C, P> {

    public AbstractPGExecutable(Distribution distribution, C config, IRuntimeConfig runtimeConfig, IExtractedFileSet executable) {
        super(distribution, config, runtimeConfig, executable);
    }
}