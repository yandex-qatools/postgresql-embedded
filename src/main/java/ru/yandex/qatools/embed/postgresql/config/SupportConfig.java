package ru.yandex.qatools.embed.postgresql.config;

import de.flapdoodle.embed.process.config.ISupportConfig;
import ru.yandex.qatools.embed.postgresql.Command;

public class SupportConfig implements ISupportConfig {
    private final Command command;

    public SupportConfig(Command command) {
        this.command = command;
    }

    @Override
    public String getName() {
        return command.commandName();
    }

    @Override
    public String getSupportUrl() {
        return "https://github.com/yandex-qatools/postgresql-embedded/issues\n";
    }

    @Override
    public String messageOnException(Class<?> context, Exception exception) {
        return null;
    }
}