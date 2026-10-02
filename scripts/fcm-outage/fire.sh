#!/usr/bin/env bash
# 시작 스토리(02) k6 실행 래퍼 - 클라이언트 관점 메트릭을 Prometheus로 밀어넣는다
# (서버 메트릭의 사각지대 보완: http_server_requests 타이머는 스레드가 요청을 집어든 뒤에 시작되므로
#  블랙아웃 중 "스레드를 못 받고 줄 서는 시간"은 클라이언트에서만 보인다)
#
# 사용: 시드 완료(devices.txt) + 앱·Prometheus 기동 상태에서
#   ./scripts/fcm-outage/fire.sh              # 기본: 폭풍(storm.js) - 스트레스 테스트
#   ./scripts/fcm-outage/fire.sh steady.js    # 평시 런 - 약속 검증
#   PING_RATE=10 ./scripts/fcm-outage/fire.sh # 파라미터는 각 js의 환경변수 참고

set -euo pipefail
cd "$(dirname "$0")/../.."   # 리포 루트에서 실행 - devices.txt 상대경로 기준점 고정

# k6 → Prometheus remote write (obs-prometheus가 --web.enable-remote-write-receiver로 수신)
export K6_PROMETHEUS_RW_SERVER_URL=${K6_PROMETHEUS_RW_SERVER_URL:-http://localhost:9090/api/v1/write}
export K6_PROMETHEUS_RW_TREND_STATS="p(95),p(99),max,avg"  # k6_http_req_duration_p95 등으로 노출
export K6_PROMETHEUS_RW_PUSH_INTERVAL=1s                   # 10초 이벤트를 초 단위로 보려면 1초 푸시

# 인자로 스크립트 선택: fire.sh steady.js (기본: 폭풍 storm.js)
k6 run --out experimental-prometheus-rw "scripts/fcm-outage/${1:-storm.js}"
