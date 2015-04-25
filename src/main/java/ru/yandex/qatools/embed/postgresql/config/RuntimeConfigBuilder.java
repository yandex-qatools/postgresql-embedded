package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.io.ProcessOutput;
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
        artifactStore().setDefault(new ArtifactStoreBuilder().defaultsWithoutCache(command).build());
        return this;
    }
}
