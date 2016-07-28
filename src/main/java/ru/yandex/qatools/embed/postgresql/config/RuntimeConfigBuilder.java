package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.config.store.IDownloadConfig;
import de.flapdoodle.embed.process.io.progress.Slf4jProgressListener;
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor;
import ru.yandex.qatools.embed.postgresql.Command;
import ru.yandex.qatools.embed.postgresql.ext.ArtifactStoreBuilder;

/**
 * Configuration builder
 */
public class RuntimeConfigBuilder extends de.flapdoodle.embed.process.config.RuntimeConfigBuilder {

    public RuntimeConfigBuilder defaults(Command command) {
        processOutput().setDefault(ProcessOutput.getDefaultInstance(command.commandName()));
        commandLinePostProcessor().setDefault(new ICommandLinePostProcessor.Noop());
        artifactStore().setDefault(storeBuilder().defaults(command).build());
        return this;
    }

    public RuntimeConfigBuilder defaultsWithLogger(Command command, org.slf4j.Logger logger) {
        defaults(command);
        processOutput().overwriteDefault(PostgresProcessOutputConfig.getInstance(command, logger));

        IDownloadConfig downloadConfig = new DownloadConfigBuilder()
                .defaultsForCommand(command)
                .progressListener(new Slf4jProgressListener(logger))
                .build();

        artifactStore().overwriteDefault(storeBuilder().defaults(command).download(downloadConfig).build());
        return this;
    }

    private ArtifactStoreBuilder storeBuilder() {
        return new ArtifactStoreBuilder();
    }

}
