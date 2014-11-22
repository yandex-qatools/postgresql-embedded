package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * pg_ctl process
 * (helper to initialize the DB)
 */
class PgCtlProcess<C extends AbstractPostgresConfig, E extends PgCtlExecutable<C>>
        extends AbstractPGProcess<C, E, PgCtlProcess> {

    public PgCtlProcess(Distribution distribution, C config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, AbstractPostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.add(exe.executable().getAbsolutePath());
        ret.addAll(asList(
                "-D", config.storage().dbDir().getAbsolutePath()
        ));
        ret.addAll(
                config.args()
        );
        return ret;
    }
}