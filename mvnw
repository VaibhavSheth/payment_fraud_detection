#!/bin/sh
# Maven wrapper script
set -e

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-"$(dirname "$0")"}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Extract distributionUrl from properties
DISTRIBUTION_URL=$(grep distributionUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2-)

# Determine JAVA_HOME
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Download Maven if wrapper jar is missing
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    WRAPPER_URL=$(grep wrapperUrl "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2-)
    if command -v curl > /dev/null; then
        curl -fsSL -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    elif command -v wget > /dev/null; then
        wget -q -O "$MAVEN_WRAPPER_JAR" "$WRAPPER_URL"
    else
        echo "ERROR: curl or wget required to download Maven wrapper"
        exit 1
    fi
fi

exec "$JAVACMD" \
    -classpath "$MAVEN_WRAPPER_JAR" \
    "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
    org.apache.maven.wrapper.MavenWrapperMain \
    "$@"
