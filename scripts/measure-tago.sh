#!/usr/bin/env bash
# 진짜 TAGO 도착정보 API 지연 실측
# - 목적 1: fake TAGO가 흉내낼 지연 분포(p50/p95/p99)를 얻는다
# - 목적 2: 도착정보 SLO의 앞항 상수(TAGO 실측 p95)를 확정한다
# - 목적 3: 응답 본문 샘플을 보존한다 (fake가 재현할 스키마 재료)
# 앱과 동일한 호출(getSttnAcctoArvlPrearngeInfoList)을 순차 반복한다.
#
# 사용: TAGO_API_KEY=... ./measure-tago.sh [횟수] [간격초] [결과디렉토리]
#   키는 반드시 디코딩(원본) 키 - URL 인코딩은 curl이 대신 한다

# bash 안전벨트: 명령 실패 즉시 중단(-e), 미선언 변수 에러(-u), 파이프 중간 실패도 실패(pipefail)
# - 측정이 조용히 잘못 돌면 틀린 숫자가 SLO 상수가 되므로 시끄럽게 죽는 쪽을 택한다
set -euo pipefail

N=${1:-300}       # 호출 횟수 - 인자 없으면 300 (드라이런은 ./measure-tago.sh 5)
SLEEP=${2:-0.2}   # 호출 간격(초) - 부하가 아니라 평상시 지연을 재는 것이므로 몰아붙이지 않는다
OUT=${3:-../measure/tago-measure-$(date +%m%d-%H%M)}  # 결과는 리포 루트 measure/ 아래로 모은다
KEY="${TAGO_API_KEY:?TAGO_API_KEY 환경변수 필요 (디코딩 키)}"  # 키를 하드코딩하지 않는 장치
KEEP=${KEEP_BODIES:-3}  # 본문 보존 개수 - 기본 3개(스키마 재료), all이면 전부 (응답 내용 검증용)

# fake 검증 시 TAGO_MEASURE_BASE=http://localhost:8081 로 오버라이드 - 같은 도구로 실측과 fake를 대조한다
BASE="${TAGO_MEASURE_BASE:-http://apis.data.go.kr/1613000/ArvlInfoInqireService}/getSttnAcctoArvlPrearngeInfoList"
CITY="${TAGO_CITY:-25}"             # 대전 (TAGO 문서 샘플 지역)
NODE="${TAGO_NODE:-DJB8001793}"     # 대전 정류소 샘플 - 드라이런에서 데이터가 비면 교체

mkdir -p "$OUT/bodies"
echo "대상: cityCode=$CITY nodeId=$NODE, ${N}회 (간격 ${SLEEP}s, 본문 보존 $KEEP)"

# ---- 1. 측정 루프: 호출하고 코드·시간을 기록한다 ----
for i in $(seq 1 "$N"); do
  # -G + --data-urlencode: 파라미터를 인코딩해서 GET 쿼리스트링으로 - 키 속 특수문자(+,/,=)가 깨지지 않는다
  # -o: 응답 본문은 파일로 / -w: 요청이 끝난 뒤 HTTP 코드와 총 소요시간(초)을 출력
  # time_total = DNS 조회부터 본문 수신 완료까지의 전체 왕복 - 우리가 재려는 그 숫자
  res=$(curl -sG "$BASE" \
    --data-urlencode "serviceKey=$KEY" \
    --data-urlencode "cityCode=$CITY" \
    --data-urlencode "nodeId=$NODE" \
    --data-urlencode "numOfRows=10" \
    --data-urlencode "_type=json" \
    -o "$OUT/bodies/body_$i.json" -w '%{http_code} %{time_total}' \
    --max-time 10 || echo "000 10")
  # || echo "000 10": curl 자체 실패(타임아웃·연결거부)도 스크립트를 죽이지 않고 데이터로 남긴다
  echo "$i $res" >> "$OUT/times.txt"   # 한 줄 = "순번 코드 시간(초)"

  # 진행 상황 출력 - "몇 번째 / 코드 / 몇 ms"
  code=$(echo "$res" | cut -d' ' -f1)
  ms=$(echo "$res" | awk '{printf "%.0f", $2*1000}')
  printf '%4d/%d  %s  %sms\n' "$i" "$N" "$code" "$ms"

  # 응답 샘플 보존 - 기본 3개(fake 스키마 재료로 충분), KEEP_BODIES=all이면 전부 남긴다
  if [ "$KEEP" != "all" ] && [ "$i" -gt "$KEEP" ]; then rm -f "$OUT/bodies/body_$i.json"; fi
  sleep "$SLEEP"
done

# ---- 2. 검증: 진짜 성공인지 확인한다 ----
echo ""
echo "== HTTP 코드 분포 =="
# 2번째 칸(코드)만 뽑아 코드별 개수 집계 - 정상이면 "300 200" 한 줄
awk '{print $2}' "$OUT/times.txt" | sort | uniq -c

# TAGO의 함정: 인증 실패·쿼터 초과도 HTTP 200으로 오고 에러는 본문 resultCode에 있다
# HTTP 코드만 믿으면 전부 성공으로 속으므로 본문을 직접 확인한다 (정상 = "00")
echo "== 샘플 resultCode (00이어야 정상) =="
grep -o '"resultCode":"[^"]*"' "$OUT/bodies/body_1.json" 2>/dev/null || head -c 300 "$OUT/bodies/body_1.json"

# ---- 3. 집계: 지연 분포를 낸다 ----
echo ""
echo "== 지연 분포 (ms, HTTP 200만) =="
# 성공 응답만 골라 초를 ms로 환산하고 숫자순 정렬 - 백분위수 계산의 전제
awk '$2==200 {printf "%.0f\n", $3*1000}' "$OUT/times.txt" | sort -n > "$OUT/lat_ms.txt"
count=$(wc -l < "$OUT/lat_ms.txt" | tr -d ' ')
if [ "$count" -eq 0 ]; then echo "성공 응답 없음"; exit 1; fi

# p95 = 정렬된 목록의 95% 지점 값. 몇 번째 줄인지 계산해서 그 줄을 꺼내면 끝
# (count*p+99)/100 은 정수 나눗셈 올림 관용구 - 300개면 p95는 285번째 줄
pct() { local idx=$(( (count * $1 + 99) / 100 )); [ "$idx" -lt 1 ] && idx=1; sed -n "${idx}p" "$OUT/lat_ms.txt"; }
echo "n=$count  min=$(head -1 "$OUT/lat_ms.txt")  p50=$(pct 50)  p90=$(pct 90)  p95=$(pct 95)  p99=$(pct 99)  max=$(tail -1 "$OUT/lat_ms.txt")"
echo ""
echo "결과: $OUT (times.txt, lat_ms.txt, bodies/)"
