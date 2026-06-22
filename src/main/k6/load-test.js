import { loginTest } from './scenarios/auth.js';
import { getSongsTest } from './scenarios/songs.js';
import { streamTest } from './scenarios/stream.js';

export let options = {
    scenarios: {
        // Scenario 1: Ramp up dần
        ramp_up: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },  // tăng lên 10 users trong 30s
                { duration: '1m',  target: 50 },  // tăng lên 50 users trong 1 phút
                { duration: '30s', target: 0 },   // giảm về 0
            ],
            gracefulRampDown: '10s',
        },

        // Scenario 2: Constant load
        constant_load: {
            executor: 'constant-vus',
            vus: 20,
            duration: '1m',
            startTime: '2m', // bắt đầu sau ramp_up
        },
    },

    // Thresholds — test fail nếu vượt ngưỡng
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95% request < 500ms
        http_req_failed: ['rate<0.01'],    // error rate < 1%
    },
};

export default function () {
    const scenario = Math.random();

    if (scenario < 0.3) {
        loginTest();
    } else if (scenario < 0.7) {
        getSongsTest();
    } else {
        streamTest();
    }
}