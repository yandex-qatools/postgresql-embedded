package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * psql process
 * (helper to initialize the DB)
 */
public class PsqlProcess<E extends PsqlExecutable> extends AbstractPGProcess<E, PsqlProcess> {

    public PsqlProcess(Distribution distribution, PostgresConfig config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, PostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.add(exe.executable().getAbsolutePath());
        ret.addAll(Arrays.asList(
                "-h", config.net().host(),
                "-p", String.valueOf(config.net().port())
        ));
        ret.addAll(config.args());

        return ret;
    }
}