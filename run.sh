#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

JAR_FILE="build/libs/Gomoku-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Building application..."
    ./gradlew jar
fi

echo "Starting Nazuna Gomoku..."
java -Xms64m -Xmx256m -jar "$JAR_FILE"
