// 03(캐시) 측정 런 - 행동 모델 그대로 조회 + 알람을 동시에 쏜다
//
// 손잡이는 N(피크 1시간 유저) 하나:
//   조회 = N x 12 / 3600 (세션당 12회 자동 갱신)
//   알람 = N x 1  / 3600 (세션당 1회)
// 유저 수 스윕은 N만 바꿔 재실행: N=1000 / 10000(기본) / 40000
//
// 속도는 "3600초당 건수"로 선언 - 정수라서 어떤 N에도 반올림 왜곡이 없다
//
// 유저 뽑기:
//   조회 - 시드 CSV를 순서대로 순환. 정류장 쏠림(인기 100개 80%)은 시드가 유저 구성에
//          박아놨으므로 고르게 뽑기만 하면 분포가 따라온다
//   알람 - 같은 풀을 자기 카운터로 순환. 한 바퀴 = 풀크기/알람RPS = 10000/2.8 ~ 3571초
//          > 쿨다운 600초라 같은 디바이스가 쿨다운 안에 재사용될 수 없다
//
// 주의: mock TAGO가 실측 재현으로 2.7% 에러를 섞는다 - 조회 실패율의 배경 소음
//
// 실행: ./scripts/cache/fire.sh            (N=10000, 10분)
//       N=40000 ./scripts/cache/fire.sh    (스윕 단계)

import http from 'k6/http';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';

// k6의 open()은 CWD가 아니라 이 스크립트 파일 위치 기준 - CSV는 리포 루트에 있다
const users = new SharedArray('users', () =>
  open(__ENV.DEVICES || '../../devices-cache.csv').trim().split('\n')
    .map(line => {
      const [deviceId, stationId] = line.split(',');
      return { deviceId, stationId };
    }));

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const N = Number(__ENV.N || 10000);
const DURATION = __ENV.DURATION || '10m';
const CITY_CODE = '25';

// 좌표 - 시드의 즐겨찾기 좌표와 동일해야 500m 필터를 통과한다
const LAT = 37.4111;
const LNG = 127.1286;

const lookupPerSec = (N * 12) / 3600;
const alarmPerSec = N / 3600;

export const options = {
  scenarios: {
    lookup: {
      executor: 'constant-arrival-rate',
      rate: N * 12,
      timeUnit: '3600s',
      duration: DURATION,
      preAllocatedVUs: Math.ceil(lookupPerSec * 3) + 10,
      maxVUs: Math.ceil(lookupPerSec * 10) + 20,
      gracefulStop: '30s',
      exec: 'lookup',
    },
    alarm: {
      executor: 'constant-arrival-rate',
      rate: N,
      timeUnit: '3600s',
      duration: DURATION,
      preAllocatedVUs: Math.ceil(alarmPerSec) + 5,
      maxVUs: Math.ceil(alarmPerSec * 10) + 20, // 접수는 ms지만 포화 구간엔 알람도 문앞에 줄을 선다
      gracefulStop: '30s',
      exec: 'alarm',
    },
  },
};

/** 조회 - 유저가 앱 화면에서 자기 정류장 도착정보를 봄 (30초 자동 갱신 1회분) */
export function lookup() {
  const user = users[exec.scenario.iterationInTest % users.length];
  http.get(
    `${BASE}/api/stations/${user.stationId}/realtime?cityCode=${CITY_CODE}`,
    {
      headers: { 'X-Device-Id': user.deviceId },
      timeout: '15s',
      tags: { name: 'lookup' },
    },
  );
}

/** 알람 - 지오펜스 진입 시 위치 전송 (접수는 ms, 평가는 워커가 수행) */
export function alarm() {
  const user = users[exec.scenario.iterationInTest % users.length];
  http.post(
    `${BASE}/api/devices/me/location`,
    JSON.stringify({ latitude: LAT, longitude: LNG }),
    {
      headers: { 'X-Device-Id': user.deviceId, 'Content-Type': 'application/json' },
      timeout: '10s',
      tags: { name: 'alarm' },
    },
  );
}
