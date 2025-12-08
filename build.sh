#!/usr/bin/env bash

chmod +x ./gradlew
./gradlew build

if [ "${?}" -eq 0 ]; then
./install.sh
fi