package ru.yandex.qatools.embed.postgresql;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import de.flapdoodle.embed.process.distribution.Distribution;
import de.flapdoodle.embed.process.extract.IExtractedFileSet;
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * pg_ctl process
 * (helper to initialize the DB)
 */
class PgCtlProcess<E extends PgCtlExecutable> extends AbstractPGProcess<E, PgCtlProcess> {

    public PgCtlProcess(Distribution distribution, PostgresConfig config, IRuntimeConfig runtimeConfig, E executable) throws IOException {
        super(distribution, config, runtimeConfig, executable);
    }

    @Override
    protected List<String> getCommandLine(Distribution distribution, PostgresConfig config, IExtractedFileSet exe)
            throws IOException {
        List<String> ret = new ArrayList<>();
        ret.addAll(asList(exe.executable().getAbsolutePath()));
        ret.addAll(asList(
                "-o",
                String.format("\"-p %s -h %s\"", config.net().port(), config.net().host()),
                "-D", config.storage().dbDir().getAbsolutePath(),
                "-w"
        ));
        if (config.args().isEmpty()) {
            ret.add("start");
        } else {
            ret.addAll(
                    config.args()
            );
        }
        return ret;
    }
}