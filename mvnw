#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script for Unix, version 3.2.0
#
# Optional ENV vars
#   JAVA_HOME - Java installation directory
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
# set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
# ----------------------------------------------------------------------------

set -e

# Find the project root
MW_PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
while [ ! -d "$MW_PROJECT_ROOT/.mvn" ]; do
  MW_PROJECT_ROOT="$(cd "$MW_PROJECT_ROOT/.." && pwd)"
  if [ "$MW_PROJECT_ROOT" = "/" ]; then
    echo "ERROR: Could not find project root (.mvn directory)"
    exit 1
  fi
done

MW_JAR="$MW_PROJECT_ROOT/.mvn/wrapper/maven-wrapper.jar"
MW_PROPERTIES="$MW_PROJECT_ROOT/.mvn\wrapper\maven-wrapper.properties"

# Check if we have to download the wrapper jar
if [ ! -f "$MW_JAR" ]; then
  # Get the wrapper url from the properties file
  MW_WRAPPER_URL=$(grep "wrapperUrl=" "$MW_PROPERTIES" | cut -d'=' -f2-)
  
  if [ -z "$MW_WRAPPER_URL" ]; then
    echo "ERROR: wrapperUrl not found in $MW_PROPERTIES"
    exit 1
  fi

  echo "Downloading Maven Wrapper from $MW_WRAPPER_URL"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$MW_WRAPPER_URL" -o "$MW_JAR" --create-dirs
  elif command -v wget >/dev/null 2>&1; then
    wget "$MW_WRAPPER_URL" -O "$MW_JAR"
  else
    echo "ERROR: curl or wget is required to download the Maven Wrapper"
    exit 1
  fi
fi

# Run Maven
if [ -n "$JAVA_HOME" ]; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" $MAVEN_OPTS -classpath "$MW_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
