import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

export function getSongsTest() {
    const res = http.get(`${BASE_URL}/api/songs?size=20`);

    check(res, {
        'songs status 200': (r) => r.status === 200,
        'has items': (r) => JSON.parse(r.body).items?.length > 0,
        'response < 200ms': (r) => r.timings.duration < 200,
    });

    sleep(0.5);
}

export function getSongsWithCursorTest(cursor) {
    const url = cursor
        ? `${BASE_URL}/api/songs?size=20&cursor=${encodeURIComponent(cursor)}`
        : `${BASE_URL}/api/songs?size=20`;

    const res = http.get(url);

    check(res, {
        'cursor status 200': (r) => r.status === 200,
        'response < 200ms': (r) => r.timings.duration < 200,
    });

    sleep(0.5);
    return JSON.parse(res.body).nextCursor;
}