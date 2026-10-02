#!/usr/bin/env bash
# 시작 스토리(02) 시드 - FCM 장애 실험용 디바이스 N개 준비
# 핑 1발이 FCM까지 도달하는 조건 5개를 전부 충족시킨다:
#   ① alarmEnabled=true (등록 기본값이라 별도 콜 없음)
#   ② 핑 좌표 = 즐겨찾기 좌표 (500m 필터 자동 통과)
#   ③ 유니크 디바이스 N개 (쿨다운이 디바이스별이라 재사용 시 2발째부터 스킵됨)
#   ④ routeId=FAKE-ROUTE (dev FakeTagoArrivalClient 응답과 매칭 - 안 맞으면 FCM 호출 자체가 스킵)
#   ⑤ fcmToken 등록 (null이면 발송 스킵)
# 산출물: devices.txt (한 줄 = 디바이스 UUID 1개) - 발사 스크립트가 그대로 읽는다
#
# 사용: 앱 기동 후 리포 루트에서  ./scripts/fcm-outage/seed.sh [디바이스수] [결과파일]

set -euo pipefail

N=${1:-200}
OUT=${2:-devices.txt}
BASE=${BASE_URL:-http://localhost:8080}

# 즐겨찾기 좌표 - 핑도 같은 좌표로 쏜다 (야탑역 부근, 값 자체는 의미 없음)
LAT=37.4111
LNG=127.1286

> "$OUT"

for i in $(seq 1 "$N"); do
  # 1. 디바이스 등록 (유일한 무인증 API) - 응답의 deviceId(UUID)가 이후 모든 호출의 인증 토큰
  DEVICE_ID=$(curl -sf -X POST "$BASE/api/devices" \
    | grep -oE '"deviceId":"[0-9a-f-]+"' | cut -d'"' -f4)
  [ -n "$DEVICE_ID" ] || { echo "[seed] 디바이스 $i 등록 실패"; exit 1; }

  # 2. FCM 토큰 등록
  curl -sf -o /dev/null -X PATCH "$BASE/api/devices/me/fcm-token" \
    -H "X-Device-Id: $DEVICE_ID" -H "Content-Type: application/json" \
    -d "{\"fcmToken\":\"fake-token-$i\"}"

  # 3. 폴더 생성 - 즐겨찾기를 담을 곳
  FOLDER_ID=$(curl -sf -X POST "$BASE/api/favorites/folders" \
    -H "X-Device-Id: $DEVICE_ID" -H "Content-Type: application/json" \
    -d '{"name":"출근"}' | grep -oE '"id":[0-9]+' | head -1 | cut -d: -f2)
  [ -n "$FOLDER_ID" ] || { echo "[seed] 디바이스 $i 폴더 생성 실패"; exit 1; }

  # 4. 즐겨찾기 등록 - routeId는 반드시 FAKE-ROUTE
  curl -sf -o /dev/null -X POST "$BASE/api/favorites/$FOLDER_ID/routes" \
    -H "X-Device-Id: $DEVICE_ID" -H "Content-Type: application/json" \
    -d "{\"stationId\":\"SEED-001\",\"stationName\":\"시드정류장\",\"regionCode\":\"25\",\"latitude\":$LAT,\"longitude\":$LNG,\"routeId\":\"FAKE-ROUTE\",\"routeName\":\"77\"}"

  echo "$DEVICE_ID" >> "$OUT"

  if [ $((i % 50)) -eq 0 ]; then echo "[seed] $i/$N"; fi
done

echo "[seed] 완료: $(wc -l < "$OUT" | tr -d ' ')개 → $OUT"
echo "[seed] 드라이런 (1발 쏴서 FCM 지연만큼 걸리고 앱 로그에 '[FCM Fake] 발송' 찍히면 체인 전체 OK):"
echo "  curl -s -X POST $BASE/api/devices/me/location -H \"X-Device-Id: \$(head -1 $OUT)\" -H 'Content-Type: application/json' -d '{\"latitude\":$LAT,\"longitude\":$LNG}' -w '\\n%{time_total}s\\n'"
