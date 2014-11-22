# Embedded PostgreSQL Server

Embedded postgresql will provide a platform neutral way for running postgres binary in unittests.
Much of the code has been crafted from [Flapdoodle OSS's embed process](https://github.com/flapdoodle-oss/de.flapdoodle.embed.process)

**Notice** This is still not well-tested and hacky library.

## Why?

- its easy, much easier as installing right version by hand
- you can change version per test

## Howto

TODO

```java

    // starting Postgres
    PostgresStarter runtime = PostgresStarter.getDefaultInstance();
    final PostgresqlConfig configDb = PostgresqlConfig.defaultWithDbName("test");
    PostgresExecutable exec = runtime.prepare(configDb);
    PostgresProcess process = exec.start();
    
    // connecting to a running Postgres
    String url = format("jdbc:postgresql://%s:%s/%s?user=%s&password=%s",
            configDb.net().getServerAddress().getHostAddress(),
            configDb.net().port(),
            configDb.storage().dbName()
    );
    Connection conn = DriverManager.getConnection(url);
    
    // feeding up the database
    conn.createStatement().execute("CREATE TABLE films (code char(5));");
    conn.createStatement().execute("INSERT INTO films VALUES ('movie');");
    
    // performing some assertions
    final Statement statement = conn.createStatement();
    assertThat(statement.execute("SELECT * FROM films;"), is(true));
    assertThat(statement.getResultSet().next(), is(true));
                
    // stopping Postgres
    conn.close();
    process.stop();
```

### Maven

TODO

### Supported Versions

Versions: 9.2.4, any custom
Support for Linux, Windows and MacOSX.

