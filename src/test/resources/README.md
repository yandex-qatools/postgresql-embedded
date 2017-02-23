### How to build test.binary_dump ?

It has been build as a copy of test.backup using the following commands on a standalone PostgreSQL :

    createdb -E UTF-8 -O user postgresql-embedded
    psql postgresql-embedded < test.backup
    pg_dump -Fc postgresql-embedded > test.binary_dump

It may be completely built by the postgresql-embedded tests (using pg_dump).
