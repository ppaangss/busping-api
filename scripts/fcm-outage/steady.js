// 시험 1 - 약속 검증 (평시 런)
//
// 설계의 약속: "설계 기준(N=1만, 핑 3/s)에서는 FCM 5초 지연 장애가 와도 알람을 전량 배달한다"
// (워커 20 = 유입 3/s × 아픈 날 처리 6초 = 18 반올림 — 이 숫자의 존재 이유를 검증하는 런)
//
// 조건: FCM 랜덤 2~8초(평균 5초) 아픈 상태 + 평시 유입 3/s를 5분간 지속
// 합격선:
//   - alarm_queue_discarded_total 증가 0 (폐기 없음)
//   - alarm_stale_skipped_total 증가 0 (TTL 스킵 없음)
//   - FCM 발송 로그 수 = 핑 수 (전량 배달)
//   - 큐 깊이 한 자릿수, busy 평평, 검색 성공률 100%
//
// 실행: ./scripts/fcm-outage/fire.sh steady.js

import http from 'k6/http';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';

// k6의 open()은 CWD가 아니라 이 스크립트 파일 위치 기준 - devices.txt는 리포 루트에 있다
const devices = new SharedArray('devices', () =>
  open(__ENV.DEVICES || '../../devices.txt').trim().split('\n'));

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const LAT = 37.4111;
const LNG = 127.1286;

export const options = {
  scenarios: {
    ping_steady: {
      executor: 'constant-arrival-rate',
      rate: 3, // 평시 유입 - 행동 모델 N=1만 기준
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: 10,
      maxVUs: 50,
      gracefulStop: '60s',
      exec: 'ping',
    },
    probe_user: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: 5,
      maxVUs: 30,
      exec: 'probe',
    },
  },
};

/** 평시 핑 - 접수는 ms여야 하고, 평가는 워커가 FCM 지연을 안고도 전량 소화해야 한다 */
export function ping() {
  const device = devices[exec.scenario.iterationInTest % devices.length];
  http.post(
    `${BASE}/api/devices/me/location`,
    JSON.stringify({ latitude: LAT, longitude: LNG }),
    {
      headers: { 'X-Device-Id': device, 'Content-Type': 'application/json' },
      timeout: '10s',
      tags: { name: 'ping' },
    },
  );
}

/** 무관 API 상시 확인 - 장애 중에도 100%여야 한다 */
export function probe() {
  http.get(`${BASE}/api/stations/nearby?latitude=${LAT}&longitude=${LNG}`, {
    headers: { 'X-Device-Id': devices[0] },
    timeout: '5s',
    tags: { name: 'probe_search' },
  });
}
