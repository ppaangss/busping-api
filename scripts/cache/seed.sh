#!/usr/bin/env bash
# 03(캐시) 시드 - 가상 유저 N명을 "정류장 500개, 인기 100개에 80% 쏠림"으로 등록
#
# 유저 1명 = 디바이스 등록 + FCM 토큰 + 폴더 + 즐겨찾기 1개(자기 정류장)
# 알람 완주 조건 2개를 시드가 책임진다:
#   - 좌표: 즐겨찾기 좌표 = 알람 전송 좌표 (500m 필터 통과)
#   - 노선: mock 야탑역 응답에 실존하는 250번(GGB204000048) - FAKE-ROUTE면 대조 단계에서 전멸
#
# 정류장 배정은 난수가 아니라 유저 번호 기반 결정적 규칙 - 돌릴 때마다 같은 분포:
#   - i가 5의 배수가 아니면(5명 중 4명 = 80%) 인기 정류장 STA-001~100에 순환 배정
#   - i가 5의 배수면(20%) 나머지 STA-101~500에 순환 배정
#   - N=1만 기준: 인기 정류장당 80명, 나머지 정류장당 5명
#
# 산출물: devices-cache.csv (한 줄 = "디바이스UUID,정류장ID") - k6가 그대로 읽는다
#
# 사용: 앱 기동 후 리포 루트에서  ./scripts/cache/seed.sh [디바이스수] [워커수] [결과파일]

set -euo pipefail

N=${1:-10000}
WORKERS=${2:-20}
OUT=${3:-devices-cache.csv}
BASE=${BASE_URL:-http://localhost:8080}

# 즐겨찾기 좌표 - 알람도 같은 좌표로 쏜다 (야탑역 부근, 값 자체는 의미 없음)
LAT=37.4111
LNG=127.1286

ROUTE_ID="GGB204000048"
ROUTE_NAME="250"

TOP_STATIONS=100
TAIL_STATIONS=400

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

# i번째 유저의 정류장 ID
station_for() {
  local i=$1
  if [ $((i % 5)) -ne 0 ]; then
    local r=$((i - i / 5))                                  # 인기 그룹 내 순번 1,2,3,...
    printf 'STA-%03d' $(((r - 1) % TOP_STATIONS + 1))
  else
    local t=$((i / 5))                                      # 나머지 그룹 내 순번 1,2,3,...
    printf 'STA-%03d' $((TOP_STATIONS + (t - 1) % TAIL_STATIONS + 1))
  fi
}

# 유저 1명 등록 - 실패하면 에러 로그만 남기고 다음 유저로 (워커 전체를 죽이지 않는다)
seed_one() {
  local i=$1 outfile=$2
  local station device_id folder_id
  station=$(station_for "$i")

  device_id=$(curl -sf -X POST "$BASE/api/devices" \
    | grep -oE '"deviceId":"[0-9a-f-]+"' | cut -d'"' -f4) || true
  if [ -z "${device_id:-}" ]; then
    echo "user=$i step=device" >> "$TMP/errors.log"; return 0
  fi

  curl -sf -o /dev/null -X PATCH "$BASE/api/devices/me/fcm-token" \
    -H "X-Device-Id: $device_id" -H "Content-Type: application/json" \
    -d "{\"fcmToken\":\"fake-token-$i\"}" \
    || { echo "user=$i step=token" >> "$TMP/errors.log"; return 0; }

  folder_id=$(curl -sf -X POST "$BASE/api/favorites/folders" \
    -H "X-Device-Id: $device_id" -H "Content-Type: application/json" \
    -d '{"name":"출근"}' | grep -oE '"id":[0-9]+' | head -1 | cut -d: -f2) || true
  if [ -z "${folder_id:-}" ]; then
    echo "user=$i step=folder" >> "$TMP/errors.log"; return 0
  fi

  curl -sf -o /dev/null -X POST "$BASE/api/favorites/$folder_id/routes" \
    -H "X-Device-Id: $device_id" -H "Content-Type: application/json" \
    -d "{\"stationId\":\"$station\",\"stationName\":\"시드-$station\",\"regionCode\":\"25\",\"latitude\":$LAT,\"longitude\":$LNG,\"routeId\":\"$ROUTE_ID\",\"routeName\":\"$ROUTE_NAME\"}" \
    || { echo "user=$i step=favorite" >> "$TMP/errors.log"; return 0; }

  echo "$device_id,$station" >> "$outfile"
}

# 워커 w는 유저 번호 w, w+WORKERS, w+2*WORKERS, ... 를 맡는다
run_worker() {
  local w=$1
  local outfile="$TMP/w$w.csv"
  : > "$outfile"
  local i
  for ((i = w; i <= N; i += WORKERS)); do
    seed_one "$i" "$outfile"
  done
}

echo "[seed] 시작: 유저 ${N}명, 워커 ${WORKERS}개, 대상 $BASE"

for ((w = 1; w <= WORKERS; w++)); do
  run_worker "$w" &
done

# 진행 상황 - 5초마다 등록 완료 수 출력
(
  while sleep 5; do
    done_count=$(cat "$TMP"/w*.csv 2>/dev/null | wc -l | tr -d ' ')
    echo "[seed] 진행 $done_count/$N"
  done
) &
MONITOR=$!

wait $(jobs -p | grep -v "^$MONITOR$") 2>/dev/null || true
kill "$MONITOR" 2>/dev/null || true
wait "$MONITOR" 2>/dev/null || true

cat "$TMP"/w*.csv > "$OUT"

# 검산 - 의도한 분포대로 들어갔는지 스크립트가 스스로 확인한다
TOTAL=$(wc -l < "$OUT" | tr -d ' ')
UNIQ_STATIONS=$(cut -d, -f2 "$OUT" | sort -u | wc -l | tr -d ' ')
TOP_RATIO=$(awk -F, '{ if (substr($2, 5) + 0 <= 100) top++ } END { printf "%.1f", top * 100 / NR }' "$OUT")
ERRORS=$([ -f "$TMP/errors.log" ] && wc -l < "$TMP/errors.log" | tr -d ' ' || echo 0)

echo "[seed] 완료: 디바이스 ${TOTAL}개 → $OUT"
echo "[seed] 고유 정류장 ${UNIQ_STATIONS}개 (기대 500), 인기 그룹 비중 ${TOP_RATIO}% (기대 80.0), 실패 ${ERRORS}건"
if [ "$ERRORS" != "0" ]; then
  echo "[seed] 실패 내역:"; sort "$TMP/errors.log" | uniq -c | sort -rn | head
fi

echo "[seed] 드라이런 (알람 1발 - 앱 로그에 '[FCM Fake] 발송' 찍히면 알람 경로 완주 OK):"
echo "  curl -s -X POST $BASE/api/devices/me/location -H \"X-Device-Id: \$(head -1 $OUT | cut -d, -f1)\" -H 'Content-Type: application/json' -d '{\"latitude\":$LAT,\"longitude\":$LNG}' -w '\\n%{time_total}s\\n'"
