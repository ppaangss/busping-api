#!/usr/bin/env bash
# 03(캐시) k6 실행 래퍼 - 클라이언트 관점 메트릭을 Prometheus로 밀어넣는다
# (서버 메트릭의 사각지대 보완: 서버 타이머는 요청을 집어든 뒤에 시작되므로
#  포화 구간의 "문앞에서 줄 서는 시간"과 "버려진 요청"은 클라이언트에서만 보인다)
#
# 사용: 시드 완료(devices-cache.csv) + 앱·mock TAGO·Prometheus 기동 상태에서
#   ./scripts/cache/fire.sh               # 기본: N=10000, 10분
#   N=1000 ./scripts/cache/fire.sh        # 스윕 1단계
#   N=40000 ./scripts/cache/fire.sh       # 스윕 3단계 (톰캣 한계 직전)
#   DURATION=3m ./scripts/cache/fire.sh   # 로컬 동작 확인용 단축

set -euo pipefail
cd "$(dirname "$0")/../.."   # 리포 루트에서 실행 - devices-cache.csv 상대경로 기준점 고정

export K6_PROMETHEUS_RW_SERVER_URL=${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}
export K6_PROMETHEUS_RW_TREND_STATS="p(95),p(99),max,avg,med"  # p50(med)도 - 캐시 글은 p50이 정직한 지표
export K6_PROMETHEUS_RW_PUSH_INTERVAL=1s

k6 run --out experimental-prometheus-rw "scripts/cache/steady.js"
