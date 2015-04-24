package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;

/**
 * postgres executable
 */
public class PostgresExecutable extends AbstractPGExecutable<PostgresConfig, PostgresProcess> {
    final IRuntimeConfig runtimeConfig;

    public PostgresExecutable(Distribution distribution,
                              PostgresConfig config, IRuntimeConfig runtimeConfig, IExtractedFileSet exe) {
        super(distribution, config, runtimeConfig, exe);
        this.runtimeConfig = runtimeConfig;
    }

    @Override
    protected PostgresProcess start(Distribution distribution, PostgresConfig config, IRuntimeConfig runtime)
            throws IOException {
        return new PostgresProcess(distribution, config, runtime, this);
    }
}