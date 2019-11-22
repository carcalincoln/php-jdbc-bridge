#!/bin/sh
java -cp 'lib/pjbridge.jar:lib/commons-daemon-1.2.2.jar:lib/postgresql-42.2.8.jar' Server org.postgresql.Driver 4444 > log.log &