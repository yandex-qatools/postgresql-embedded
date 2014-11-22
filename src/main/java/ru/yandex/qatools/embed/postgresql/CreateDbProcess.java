package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * createdb process
 * (helper to initialize the DB)
 */
class CreateDbProcess<C extends AbstractPostgresConfig, E extends CreateDbExecutable<C>>
        extends AbstractPGProcess<C, E, CreateDbProcess> {

    public CreateDbProcess(Distribution distribution, C config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, AbstractPostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.add(exe.executable().getAbsolutePath());
        ret.addAll(Arrays.asList(
                "-h", config.net().getServerAddress().getHostName(),
                "-p", String.valueOf(config.net().port())
        ));
        ret.add(config.storage().dbName());

        return ret;
    }
}