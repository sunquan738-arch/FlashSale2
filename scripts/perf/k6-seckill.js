import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '60s', target: 200 },
    { duration: '30s', target: 0 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05']
  }
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'alice';
const PASSWORD = __ENV.PASSWORD || '123456';
const ACTIVITY_ID = __ENV.ACTIVITY_ID || '2';

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginRes, {
    'login success': (r) => r.status === 200 && r.json('code') === 200
  });

  return {
    token: loginRes.json('data.token')
  };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`
  };

  const seckillRes = http.post(`${BASE_URL}/api/seckill/do/${ACTIVITY_ID}`, null, { headers });
  check(seckillRes, {
    'seckill accepted or rejected': (r) => r.status === 200
  });

  const resultRes = http.get(`${BASE_URL}/api/seckill/result/${ACTIVITY_ID}`, { headers });
  check(resultRes, {
    'result query success': (r) => r.status === 200 && r.json('code') === 200
  });

  sleep(0.2);
}
