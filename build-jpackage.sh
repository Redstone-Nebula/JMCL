#!/bin/bash
# =============================================================================
# JMCL jpackage Build Script
# Builds JMCL from source and packages it as a macOS .dmg installer.
#
# Usage:
#   ./build-jpackage.sh            — full build + package
#   ./build-jpackage.sh --skip-build — package only (uses existing JAR)
#   ./build-jpackage.sh --help     — show usage
#
# Prerequisites:
#   - JDK 21+ with jpackage (JAVA_HOME or JDK21_HOME)
#   - Gradle (bundled wrapper)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ---- Config ----
APP_NAME="JMCL"
VENDOR="Open Code Studio"
IDENTIFIER="org.Open_code_Studio.jmcl"
ICON_PNG="$SCRIPT_DIR/JMCL/image/jmcl.png"
BUILD_DIR="$SCRIPT_DIR/JMCL/build/libs"
DEST_DIR="$SCRIPT_DIR/dist"

# Force FORK process mechanism for all Java subprocesses (fix macOS posix_spawn bug)
export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"

# ---- JDK detection ----
if [ -n "${JDK21_HOME:-}" ]; then
    JAVA_HOME="$JDK21_HOME"
elif [ -d "/Users/cangcang/Documents/jdk21" ]; then
    JAVA_HOME="/Users/cangcang/Documents/jdk21"
else
    JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || echo "")}"
fi

if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/jpackage" ]; then
    echo "ERROR: Cannot find JDK 21+ with jpackage."
    echo "  Set JDK21_HOME or JAVA_HOME to a JDK 21+ installation."
    exit 1
fi

JPACKAGE="$JAVA_HOME/bin/jpackage"
JAVA="$JAVA_HOME/bin/java"
echo "Using JDK: $JAVA_HOME"

# ---- Arg parsing ----
SKIP_BUILD=false
if [ "${1:-}" = "--skip-build" ]; then
    SKIP_BUILD=true
elif [ "${1:-}" = "--help" ]; then
    echo "Usage: $0 [--skip-build] [--help]"
    echo ""
    echo "  --skip-build  Skip Gradle build; reuse existing JAR from $BUILD_DIR"
    echo "  --help        Show this help"
    exit 0
fi

# ---- Step 1: Build ----
if [ "$SKIP_BUILD" = false ]; then
    echo ""
    echo "=== Step 1: Building JMCL ==="
    export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"
    export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Djava.awt.headless=true"

    if command -v gradle &>/dev/null; then
        GRADLE_CMD="gradle"
    else
        GRADLE_CMD="$SCRIPT_DIR/gradlew"
        if [ ! -f "$GRADLE_CMD" ]; then
            echo "ERROR: Gradle wrapper not found at $GRADLE_CMD"
            exit 1
        fi
        chmod +x "$GRADLE_CMD"
    fi

    "$GRADLE_CMD" \
        --no-daemon \
        -g "$SCRIPT_DIR/.gradle-user-home" \
        clean build -x test

    echo "Build complete."
else
    echo ""
    echo "=== Skipping build (--skip-build) ==="
fi

# ---- Step 2: Find JAR ----
echo ""
echo "=== Step 2: Locating JAR ==="
JAR_FILE=$(ls -t "$BUILD_DIR"/*.jar 2>/dev/null | head -1)
if [ -z "$JAR_FILE" ]; then
    echo "ERROR: No JAR file found in $BUILD_DIR"
    exit 1
fi
echo "JAR: $JAR_FILE"

# Extract version from JAR filename: JVM-MCL-<version>.jar → <version>
JAR_BASENAME=$(basename "$JAR_FILE" .jar)
RAW_VERSION="${JAR_BASENAME#JVM-MCL-}"
if [ -z "$RAW_VERSION" ] || [ "$RAW_VERSION" = "$JAR_BASENAME" ]; then
    # Fallback: read from jvmmcl.properties inside the JAR
    RAW_VERSION=$(unzip -p "$JAR_FILE" "assets/jvmmcl.properties" 2>/dev/null \
        | grep "^jvmmcl.version=" | cut -d= -f2 || echo "1.0.0")
fi
# Sanitize version: remove any non-numeric prefix (e.g. DEV2026.2.1 → 2026.2.1)
# jpackage requires a pure numeric version (major.minor.patch)
APP_VERSION=$(echo "$RAW_VERSION" | sed 's/^[^0-9]*//')
echo "Version: $RAW_VERSION → $APP_VERSION"

# ---- Step 3: Prepare clean input directory (JAR only, no .exe/.sh/.deb) ----
echo ""
echo "=== Step 3: Preparing clean input directory ==="
INPUT_DIR="/tmp/jmcl-input-$$"
mkdir -p "$INPUT_DIR"
cp "$JAR_FILE" "$INPUT_DIR/"
echo "Input dir (JAR only): $INPUT_DIR"

# ---- Step 4: Prepare ICNS icon ----
echo ""
echo "=== Step 4: Preparing app icon ==="
ICONSET_DIR="/tmp/jmcl-iconset.$$"
ICNS_FILE="/tmp/jmcl-$APP_VERSION.icns"
mkdir -p "$ICONSET_DIR"

if [ -f "$ICON_PNG" ]; then
    echo "Converting PNG to ICNS..."
    # Resize to 256x256 and convert to ICNS format using sips
    ICNS_TMP_PNG="/tmp/jmcl-icon-256-$$.png"
    if sips -z 256 256 "$ICON_PNG" --out "$ICNS_TMP_PNG" &>/dev/null \
        && sips -s format icns "$ICNS_TMP_PNG" --out "$ICNS_FILE" &>/dev/null; then
        echo "  ICNS created: $ICNS_FILE"
    else
        echo "WARNING: ICNS conversion failed. jpackage will use a default icon."
        ICNS_FILE=""
    fi
    rm -f "$ICNS_TMP_PNG"
    rm -rf "$ICONSET_DIR"
    echo "Icon: ${ICNS_FILE:-"(default)"}"
else
    echo "WARNING: Icon not found at $ICON_PNG, using default"
    ICNS_FILE=""
fi

# ---- Step 5: jpackage ----
echo ""
echo "=== Step 5: Packaging with jpackage ==="
mkdir -p "$DEST_DIR"

# Collect all --add-opens from the Gradle build config
ADD_OPENS=(
    "--add-opens=java.base/java.lang=ALL-UNNAMED"
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
    "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED"
    "--add-opens=javafx.base/com.sun.javafx.binding=ALL-UNNAMED"
    "--add-opens=javafx.base/com.sun.javafx.event=ALL-UNNAMED"
    "--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED"
    "--add-opens=javafx.base/javafx.beans.property=ALL-UNNAMED"
    "--add-opens=javafx.graphics/javafx.css=ALL-UNNAMED"
    "--add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED"
    "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.scene=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.stage=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.util=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.prism=ALL-UNNAMED"
    "--add-opens=javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED"
    "--add-opens=javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED"
    "--add-opens=javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED"
    "--add-opens=javafx.controls/javafx.scene.control.skin=ALL-UNNAMED"
    "--add-opens=jdk.attach/sun.tools.attach=ALL-UNNAMED"
    "-Djdk.lang.Process.launchMechanism=FORK"
    "-Djvmmcl.offline.auth.restricted=false"
    "-Djvmmcl.dir=$HOME/.jvm-mcl"
    "-Xmx1g"
)

# Build jpackage arguments
JPACKAGE_ARGS=(
    --type dmg
    --name "$APP_NAME"
    --app-version "$APP_VERSION"
    --vendor "$VENDOR"
    --main-jar "$(basename "$JAR_FILE")"
    --main-class "org.Open_code_Studio.jmcl.Main"
    --input "$INPUT_DIR"
    --dest "$DEST_DIR"
    --mac-package-identifier "$IDENTIFIER"
    --mac-package-name "$APP_NAME"
    --runtime-image "$JAVA_HOME"
    --java-options "-Dsun.java2d.metal=true"
)

# Add icon if available
if [ -n "${ICNS_FILE:-}" ] && [ -f "$ICNS_FILE" ]; then
    JPACKAGE_ARGS+=(--icon "$ICNS_FILE")
fi

# Add all JVM options
for opt in "${ADD_OPENS[@]}"; do
    JPACKAGE_ARGS+=(--java-options "$opt")
done

echo "Running jpackage..."
echo "  Output: $DEST_DIR/${APP_NAME}-${APP_VERSION}.dmg"
echo ""

# Retry loop for macOS posix_spawn race condition
MAX_RETRIES=3
RETRY_DELAY=2
JPACKAGE_EXIT=1
for attempt in $(seq 1 "$MAX_RETRIES"); do
    if [ "$attempt" -gt 1 ]; then
        echo "  Retry attempt $attempt/$MAX_RETRIES..."
        sleep "$RETRY_DELAY"
    fi

    if "$JPACKAGE" "${JPACKAGE_ARGS[@]}"; then
        JPACKAGE_EXIT=0
        break
    else
        JPACKAGE_EXIT=$?
        echo "  jpackage failed (exit code $JPACKAGE_EXIT), will retry..."
    fi
done

if [ "$JPACKAGE_EXIT" -ne 0 ]; then
    echo ""
    echo "=== JPACKAGE FAILED after $MAX_RETRIES attempts ==="
    exit "$JPACKAGE_EXIT"
fi

echo ""
echo "=== BUILD SUCCESSFUL ==="
echo "Package: $DEST_DIR/${APP_NAME}-${APP_VERSION}.dmg"
echo ""

# Clean up temp files
rm -f "$ICNS_FILE" 2>/dev/null
rm -rf "$INPUT_DIR" 2>/dev/null