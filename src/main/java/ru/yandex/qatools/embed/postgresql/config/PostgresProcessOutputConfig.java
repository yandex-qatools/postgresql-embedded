package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.io.ProcessOutput;
import ru.yandex.qatools.embed.postgresql.Command;

/**
 * @author <a href='mailto:alexey@zhokhov.com'>Alexey Zhokhov</a>
 */
public class PostgresProcessOutputConfig {

    private PostgresProcessOutputConfig() {}

    public static ProcessOutput getDefaultInstance(Command command) {
        return ProcessOutput.getDefaultInstance(command.commandName());
    }

    public static ProcessOutput getInstance(Command command, org.slf4j.Logger logger) {
        return ProcessOutput.getInstance(command.commandName(), logger);
    }

}