import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

export function loginTest() {
    const res = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
        email: 'minhlewrk@gmail.com',
        password: '123456789',
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    check(res, {
        'login status 200': (r) => r.status === 200,
        'has accessToken': (r) => JSON.parse(r.body).tokens?.accessToken !== undefined,
        'response < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1);
    return JSON.parse(res.body).tokens?.accessToken;
}