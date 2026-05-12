const WebSocket = require('ws');
const token = process.argv[2];

if (!token) {
    console.error('❌ EROARE: Te rog introdu un token ca argument!');
    process.exit(1);
}

const ws = new WebSocket(`ws://localhost:8080/locatie?token=${token}`);

ws.on('open', () => console.log('✅ TEST CLIENT: Conectat!'));
ws.on('message', (data) => {
    console.log('📍 DATE PRIMITE:');
    try {
        console.log(JSON.parse(data));
    } catch (e) {
        console.log(data.toString());
    }
});
ws.on('error', (e) => console.error('❌ EROARE:', e.message));
ws.on('close', (code, reason) => console.log(`🔴 CONEXIUNE ÎNCHISĂ: ${code} ${reason}`));
