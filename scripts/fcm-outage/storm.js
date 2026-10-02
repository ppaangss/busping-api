// 시작 스토리(02) - 핑 부하가 서서히 차오르다 톰캣 200을 넘는 순간, 무관한 API가 절벽처럼 죽는다
//
// 절벽의 산식: busy ≈ 핑 유입률 × FCM 평균 지연(5s = 랜덤 2~8s). 유입 40/s 돌파 순간 busy=200.
// 유입을 5/s → 50/s로 서서히 올려 "조금씩 → 갑자기"를 그리고, 마지막은 페이드아웃으로
// 회복도 비스듬히 녹아내리게 한다. 랜덤 지연이 busy 선의 출렁임과 회복 꼬리를 만든다.
//
// 시나리오 3개:
//   ping_storm - 핑 유입 램프 5→50/s (2분) + 50/s 유지 (1분) + 페이드아웃 (30초). FCM은 내내 아픔
//   probe_user - 검색 1/s, 타임아웃 5초(유저 인내심) → "정류장 검색 API 성공률" 그래프용
//   probe_raw  - 검색 0.5/s, 타임아웃 120초 → 요청~응답 전 구간(줄서기 포함) 지연의 민낯
//
// 시드: 3,000개면 충분 - 피크 50/s에서 디바이스 한 바퀴(3,000발) 도는 데 60초 > 쿨다운 30초
//
// 실행: ./scripts/fcm-outage/fire.sh  (환경변수: PING_RATE, STORM, FIRE_AT, DURATION, BASE_URL, DEVICES)

import http from 'k6/http';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';

// k6의 open()은 CWD가 아니라 이 스크립트 파일 위치 기준 - devices.txt는 리포 루트에 있다
const devices = new SharedArray('devices', () =>
  open(__ENV.DEVICES || '../../devices.txt').trim().split('\n'));

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// 즐겨찾기 시드와 동일 좌표 - 500m 필터 통과 조건
const LAT = 37.4111;
const LNG = 127.1286;

// 폭풍 4분 + 느림보(포화 중 갇힌 요청) 배수 ~1분 + 회복 관찰 2분
// 주의: 지연은 완료 시점에 기록되므로, 회복이 그래프에 보이려면 느림보가 다 빠진 뒤에도
// 빠른 샘플이 쌓일 시간이 필요하다 - 5m으로 돌렸더니 12초에서 시계열이 끊겨 회복이 안 보였음
const TOTAL = '7m';

export const options = {
  scenarios: {
    ping_storm: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      stages: [
        { target: 5, duration: '30s' },   // 평시 - 가벼운 핑
        { target: 50, duration: '120s' }, // 서서히 조여온다 - 40/s 넘는 순간(약 2분 지점)에 장애 진입
        { target: 50, duration: '30s' },  // 꼭대기 잠깐 유지 (길수록 지연 산이 높아짐)
        { target: 5, duration: '120s' },  // 서서히 풀린다 - 40/s 밑으로 내려오는 순간 회복 시작 (대칭 산)
        { target: 0, duration: '15s' },   // 마무리
      ],
      preAllocatedVUs: 500,
      maxVUs: 5000, // 대기 수십 초 × 30/s를 버틸 만큼
      gracefulStop: '180s', // 시나리오 종료 후에도 매달린 핑을 끊지 않고 끝까지 기록
      exec: 'ping',
    },
    probe_user: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '1s',
      duration: TOTAL,
      preAllocatedVUs: 5,
      maxVUs: 30,
      exec: 'probeUser',
    },
    probe_raw: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '2s', // 0.5/s - 진실 기록용이라 부하는 최소로
      duration: TOTAL,
      preAllocatedVUs: 5,
      maxVUs: 100, // 응답까지 수십 초 매달리는 VU가 쌓인다
      gracefulStop: '120s',
      exec: 'probeRaw',
    },
  },
};

/** 유저 관점 피해자 - 5초(인내심) 안에 못 받으면 실패로 집계 → 성공률 그래프 */
export function probeUser() {
  http.get(`${BASE}/api/stations/nearby?latitude=${LAT}&longitude=${LNG}`, {
    headers: { 'X-Device-Id': devices[0] },
    timeout: '5s',
    tags: { name: 'probe_user' },
  });
}

/** 지연의 민낯 - 끊지 않고 끝까지 기다려 요청~응답 전 구간(스레드풀 줄서기 포함)을 기록 */
export function probeRaw() {
  http.get(`${BASE}/api/stations/nearby?latitude=${LAT}&longitude=${LNG}`, {
    headers: { 'X-Device-Id': devices[1] },
    timeout: '120s',
    tags: { name: 'probe_raw' },
  });
}

/** 핑 - 시나리오 내 고유 번호로 디바이스 순환 배정 (한 바퀴 100초 > 쿨다운 30초라 중복 무해) */
export function ping() {
  const device = devices[exec.scenario.iterationInTest % devices.length];
  http.post(
    `${BASE}/api/devices/me/location`,
    JSON.stringify({ latitude: LAT, longitude: LNG }),
    {
      headers: { 'X-Device-Id': device, 'Content-Type': 'application/json' },
      timeout: '180s', // 깊은 포화의 대기까지 끝까지 기록
      tags: { name: 'ping' },
    },
  );
}
