#!/bin/bash

# Direct build script - bypass Gradle wrapper daemon issues

JAVA_HOME="/Users/cangcang/Documents/HMCL-main/jdk21"
export JAVA_HOME

GRADLE_USER_HOME="/Users/cangcang/Documents/HMCL-main/.gradle-user-home"
export GRADLE_USER_HOME

# Path to downloaded Gradle 8.5
GRADLE_HOME="/Users/cangcang/.gradle/wrapper/dists/gradle-8.5-bin/5t9huq95ubn472n8rpzujfbqh/gradle-8.5"

# Run Gradle directly, with all daemon-disabling flags
"$JAVA_HOME/bin/java" \
  -Xmx2048m \
  -Dorg.gradle.daemon=false \
  -Dorg.gradle.jvmargs=-Xmx2048m \
  -classpath "$GRADLE_HOME/lib/gradle-launcher-8.5.jar" \
  org.gradle.launcher.GradleMain \
  --no-daemon \
  --project-dir "$PWD" \
  --gradle-user-home "$GRADLE_USER_HOME" \
  clean build
