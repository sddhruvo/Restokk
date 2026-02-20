#!/bin/bash
# Build helper — one-shot build with clean output
# Usage: bash scripts/build.sh [clean]

export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
P="/e/UNIVERSITY/python/Inventory/android"

TASK="assembleDebug"
if [ "${1:-}" = "clean" ]; then
    TASK="clean assembleDebug"
fi

# --console=plain prevents Gradle rich console (ANSI codes break pipes on Windows)
# --quiet suppresses task list, only shows errors and final result
"$P/gradlew" -p "$P" $TASK --console=plain --quiet 2>&1
