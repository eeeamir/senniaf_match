"""Render real del Comité con respuestas sintéticas locales; no conecta al backend ni a Azure.
Requiere Chromium con CDP, sin dependencias Python. Resultado separado del tiempo del endpoint.
"""
import argparse
import base64
import http.server
import json
import os
from pathlib import Path
import socket
import struct
import subprocess
import tempfile
import threading
import time
import urllib.parse
import urllib.request


class CDP:
    def __init__(self, url):
        u = urllib.parse.urlsplit(url)
        self.s = socket.create_connection((u.hostname, u.port), timeout=120)
        key = base64.b64encode(os.urandom(16)).decode()
        self.s.sendall((f"GET {u.path} HTTP/1.1\r\nHost: {u.netloc}\r\nUpgrade: websocket\r\n"
                        f"Connection: Upgrade\r\nSec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n\r\n").encode())
        header = b""
        while not header.endswith(b"\r\n\r\n"):
            header += self.s.recv(1)
        assert b"101" in header, header
        self.n = 0

    def read(self, n):
        data = b""
        while len(data) < n:
            part = self.s.recv(n-len(data))
            if not part:
                raise EOFError()
            data += part
        return data

    def call(self, method, **params):
        self.n += 1
        msg = json.dumps(dict(id=self.n, method=method, params=params)).encode()
        mask = os.urandom(4)
        length = len(msg)
        header = bytes([0x81, 0x80 | length]) if length < 126 else bytes([0x81, 0xfe])+struct.pack('!H', length)
        self.s.sendall(header+mask+bytes(b ^ mask[i % 4] for i, b in enumerate(msg)))
        while True:
            a, b = self.read(2)
            length = b & 127
            if length == 126:
                length = struct.unpack('!H', self.read(2))[0]
            elif length == 127:
                length = struct.unpack('!Q', self.read(8))[0]
            data = json.loads(self.read(length))
            if data.get('id') == self.n:
                if 'error' in data:
                    raise RuntimeError(data)
                return data['result']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('resultados', type=Path)
    parser.add_argument('--chromium', required=True)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    static = repo/'src/main/resources/static'
    page = (static/'match.html').read_text().split('<script src="vinculaciones.js">')[0]
    page += "<script>sessionStorage.setItem('senniaf.sesion',JSON.stringify({rol:'COMITE',id:'SINTETICO'}));window.probeReady=true;</script></body></html>"

    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *a, **kw):
            super().__init__(*a, directory=str(static), **kw)

        def log_message(self, *a):
            pass

        def do_GET(self):
            path = urllib.parse.urlsplit(self.path).path
            ref = urllib.parse.parse_qs(urllib.parse.urlsplit(self.headers.get('Referer', '')).query)
            if path == '/probe':
                body = page.encode(); tipo = 'text/html; charset=utf-8'
            elif path == '/api/scoring/casos':
                v = ref.get('version', ['v7'])[0]; n = int(ref.get('n', ['10'])[0])
                body = (args.resultados/f'{v}-{n}-payload.json').read_bytes(); tipo = 'application/json'
            elif path == '/api/scoring/pesos/programacion':
                body = b'{"pendiente":null,"versionVigente":1}'; tipo = 'application/json'
            elif path == '/api/scoring/reevaluaciones-pendientes':
                body = b'[]'; tipo = 'application/json'
            else:
                return super().do_GET()
            self.send_response(200); self.send_header('Content-Type', tipo)
            self.send_header('Content-Length', str(len(body))); self.end_headers(); self.wfile.write(body)

    server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    rows = []
    with tempfile.TemporaryDirectory(prefix='senniaf-cdp-') as profile:
        browser = subprocess.Popen([args.chromium, '--headless', '--no-sandbox', '--disable-gpu',
                                    '--remote-debugging-port=0', '--remote-allow-origins=*',
                                    '--user-data-dir='+profile, 'about:blank'], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        try:
            for _ in range(100):
                active = Path(profile)/'DevToolsActivePort'
                if active.exists():
                    break
                time.sleep(.1)
            port = active.read_text().splitlines()[0]
            target = json.load(urllib.request.urlopen(f'http://127.0.0.1:{port}/json/list'))[0]
            cdp = CDP(target['webSocketDebuggerUrl'])
            for fork in (1, 2):
                for n in (10, 50, 100, 500):
                    for v in (('v6', 'v7') if fork == 1 else ('v7', 'v6')):
                        url = f'http://127.0.0.1:{server.server_port}/probe?vista=compatibilidad&version={v}&n={n}'
                        cdp.call('Page.navigate', url=url)
                        for _ in range(100):
                            ready = cdp.call('Runtime.evaluate', expression='window.probeReady===true', returnByValue=True)
                            if ready.get('result', {}).get('value'):
                                break
                            time.sleep(.05)
                        expression = """(async()=>{
                          const rows=[];
                          for(let i=0;i<25;i++){
                            performance.clearResourceTimings();
                            const t=performance.now();
                            await Comite.iniciar({rol:'COMITE',id:'SINTETICO'});
                            const dom=performance.now()-t;
                            await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));
                            if(document.querySelectorAll('.pro-compatibilidad-familia').length!==NUMBER)throw Error('Tarjetas incompletas');
                            if(i>=5)rows.push({domMs:dom,visibleMs:performance.now()-t,
                              solicitudes:performance.getEntriesByType('resource').filter(x=>x.name.includes('/api/')).map(x=>({url:new URL(x.name).pathname,ms:x.duration,bytes:x.decodedBodySize}))});
                          }
                          return rows;
                        })()""".replace('NUMBER', str(n))
                        result = cdp.call('Runtime.evaluate', expression=expression, awaitPromise=True, returnByValue=True)
                        if 'exceptionDetails' in result:
                            raise RuntimeError(result)
                        for x in result['result']['value']:
                            rows.append(dict(version=v, familias=n, fork=fork, **x))
                        print('UI', v, n, fork, flush=True)
            (args.resultados/'interfaz.json').write_text(json.dumps(rows))
        finally:
            browser.terminate(); browser.wait(timeout=10); server.shutdown()


if __name__ == '__main__':
    main()
