#!/usr/bin/env bash
# 로컬 mock TAGO 기동 - WireMock 단일 프로세스 (8081, 저널 off)
# FCM은 측정 대상에서 제외 - 앱의 FakeFcmService(로그 발송)로 대체됨
# 사용: ./run-local.sh        (Ctrl+C로 종료)
set -euo pipefail
cd "$(dirname "$0")"

VER=3.9.1
JAR=".cache/wiremock-standalone-$VER.jar"

if [ ! -f "$JAR" ]; then
  echo ">>> WireMock $VER 다운로드"
  curl -sfL -o "$JAR" "https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/$VER/wiremock-standalone-$VER.jar"
fi

echo ">>> extension 빌드 (지연·에러율 담당)"
(cd extension && ./build.sh)

# -jar 대신 -cp: extension jar를 클래스패스에 같이 올려야 해서 (메인클래스는 wiremock.Run)
echo ">>> mock TAGO :8081 (journal off, tago-world extension)"
exec java -cp "$JAR:extension/tago-world.jar" wiremock.Run \
  --port 8081 --root-dir tago --no-request-journal \
  --extensions com.busping.faketago.TagoWorld \
  --container-threads 200
