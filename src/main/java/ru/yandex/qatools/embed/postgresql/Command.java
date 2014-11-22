package ru.yandex.qatools.embed.postgresql;

public enum Command {
    Postgres("postgres"),
    InitDb("initdb"),
    CreateDb("createdb"),
    PgCtl("pg_ctl"),
    ;

    private final String commandName;

    Command(String commandName) {
        this.commandName = commandName;
    }

    public String commandName() {
        return commandName;
    }
}