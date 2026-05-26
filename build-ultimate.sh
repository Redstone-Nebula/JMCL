#!/bin/bash

# Ultimate build script - use FORK mechanism to bypass posix_spawn issues

JAVA_HOME="/Users/cangcang/Documents/HMCL-main/jdk21"
export JAVA_HOME

GRADLE_USER_HOME="/Users/cangcang/Documents/HMCL-main/.gradle-user-home"
export GRADLE_USER_HOME

GRADLE_HOME="/Users/cangcang/.gradle/wrapper/dists/gradle-9.4.0-bin/lcvyxq3t37f6mx9miaydrrgs/gradle-9.4.0"

# Force use of traditional process spawning (FORK), not posix_spawn
export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Djava.awt.headless=true"

chmod +x "$GRADLE_HOME/bin/gradle"

"$GRADLE_HOME/bin/gradle" \
  --no-daemon \
  --project-dir "$PWD" \
  --gradle-user-home "$GRADLE_USER_HOME" \
  --no-build-cache \
  --no-parallel \
  --max-workers=1 \
  clean build