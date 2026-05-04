const WebSocket = require('ws');
const ws = new WebSocket('ws://localhost:8081/locatie');
ws.on('open', () => console.log('✅ TEST CLIENT: Conectat!'));
ws.on('message', (data) => {
    console.log('📍 DATE PRIMITE:');
    console.log(JSON.parse(data));
});
ws.on('error', (e) => console.error('❌ EROARE:', e.message));
