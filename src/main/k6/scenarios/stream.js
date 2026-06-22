import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = 'http://localhost:8080';

// Thay bằng songId thực tế trong DB
const SONG_IDS = [
    'f469897f-a785-403e-a947-2b20b8e6eaee',
    'bc233949-d662-448b-ac26-f9027c54b914',
];

export function streamTest() {
    const songId = SONG_IDS[Math.floor(Math.random() * SONG_IDS.length)];

    // Test master.m3u8
    const masterRes = http.get(`${BASE_URL}/api/songs/stream/${songId}/master.m3u8`);
    check(masterRes, {
        'master.m3u8 status 200': (r) => r.status === 200,
        'is m3u8 content': (r) => r.body.includes('#EXTM3U'),
        'response < 300ms': (r) => r.timings.duration < 300,
    });

    sleep(1);
}