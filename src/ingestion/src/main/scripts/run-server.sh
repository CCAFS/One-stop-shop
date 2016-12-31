#!/usr/bin/env bash

parse_jvm_options() {
  if [ -f "$1" ]; then
    echo "$(grep "^-" "$1" | tr '\n' ' ')"
  fi
}

SCRIPT="$0"

if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=`which java`
fi

if [ ! -x "$JAVA" ]; then
    echo "Could not find any executable java binary. Please install java in your PATH or set JAVA_HOME"
    exit 1
fi

PROG_HOME=`dirname "$SCRIPT"`

# make PROGRAM_HOME absolute
PROG_HOME=`cd "$PROG_HOME"; pwd`

if [ -z "JVM_OPTIONS" ]; then
    for jvm_options in "$PROG_HOME"/config/jvm.options \
                      /etc/ccafs-ingestion/jvm.options; do
        if [ -r "$jvm_options" ]; then
            JVM_OPTIONS=$jvm_options
            break
        fi
    done
fi

LOG4J_OPTS="-Dlog4j.configurationFile=$PROG_HOME/config/log4j2.xml"
PROG_OPTS="$PROG_HOME/config"

JAVA_OPTS="$(parse_jvm_options "$JVM_OPTIONS") $JAVA_OPTS"

exec "$JAVA" $JAVA_OPTS $LOG4J_OPTS -jar ingestion-engine-${version}.jar $PROG_OPTS "$@"