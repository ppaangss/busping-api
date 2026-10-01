#!/usr/bin/env bash
# fake TAGO extension 빌드 - 산출물: fake/extension/tago-world.jar
# WireMock jar를 컴파일 클래스패스로 쓰므로 run-local.sh를 한 번 실행해 .cache를 채워둬야 한다
set -euo pipefail
cd "$(dirname "$0")"

WM=../.cache/wiremock-standalone-3.9.1.jar
[ -f "$WM" ] || { echo "WireMock jar 없음 - fake/run-local.sh를 먼저 한 번 실행해 다운로드"; exit 1; }

rm -rf out
javac -encoding UTF-8 -cp "$WM" -d out src/com/busping/faketago/TagoWorld.java
jar cf tago-world.jar -C out .
rm -rf out
echo "OK: $(pwd)/tago-world.jar"
