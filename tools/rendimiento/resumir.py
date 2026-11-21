"""Consolida muestras sin extrapolar SQL ni sumar etapas medidas por separado."""
import argparse
import collections
import csv
import gzip
import hashlib
import json
import math
from pathlib import Path
import re
import statistics


def p95(valores):
    return sorted(valores)[math.ceil(.95*len(valores))-1]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('entrada', type=Path)
    parser.add_argument('salida', type=Path)
    args = parser.parse_args()
    args.salida.mkdir(parents=True, exist_ok=True)
    grupos = collections.defaultdict(list); raw = []
    for p in sorted(args.entrada.glob('v[67]-*-?.json')):
        rows = json.loads(p.read_text()); raw.extend(rows)
        for r in rows:
            grupos[r['version'], r['familias'], r['fase']].append(r)
    assert len(grupos) == 40
    resumen = []
    for (v, n, fase), rows in sorted(grupos.items()):
        assert len(rows) == (80 if fase == 'concurrente4' else 60)
        tiempos = [r['ms'] for r in rows]
        picos = [int(re.search(r'VmHWM:\s+(\d+)', p.read_text())[1])*1024
                 for p in args.entrada.glob(f'{v}-{n}-?-vm.txt')]
        fila = dict(version=v, familias=n, fase=fase, muestras=len(rows), medianaMs=statistics.median(tiempos),
                    p95Ms=p95(tiempos), rssMaxMiB=max(picos)/2**20,
                    sqlReal='NO_MEDIDO', llamadasRepositorio='', cpuMediaMs='', asignacionMediaMiB='', heapPicoMiB='')
        if fase != 'concurrente4':
            llamadas = {sum(r['llamadasRepositorio'].values()) for r in rows}
            assert len(llamadas) == 1
            fila.update(llamadasRepositorio=llamadas.pop(), cpuMediaMs=statistics.mean(r['cpuMs'] for r in rows),
                        asignacionMediaMiB=statistics.mean(r['bytesAsignados'] for r in rows)/2**20,
                        heapPicoMiB=max(r['heapPicoBytes'] for r in rows)/2**20)
        resumen.append(fila)
    with (args.salida/'resumen.csv').open('w') as f:
        w=csv.DictWriter(f, fieldnames=list(resumen[0]));w.writeheader();w.writerows(resumen)
    with gzip.open(args.salida/'muestras.jsonl.gz', 'wt') as f:
        for row in raw:
            f.write(json.dumps(row, separators=(',',':'))+'\n')
    if (args.entrada/'interfaz.json').exists():
        ui=json.loads((args.entrada/'interfaz.json').read_text())
        (args.salida/'interfaz.json').write_text(json.dumps(ui, separators=(',',':')))
        for v in ('v6','v7'):
            for n in (10,50,100,500):
                r=[x for x in ui if x['version']==v and x['familias']==n]
                assert len(r)==40
                assert all(collections.Counter(q['url'] for q in x['solicitudes'])=={
                    '/api/scoring/casos':1,'/api/scoring/pesos/programacion':1,'/api/scoring/reevaluaciones-pendientes':1} for x in r)
                print('UI',v,n,'visible',round(statistics.median(x['visibleMs'] for x in r),2),round(p95([x['visibleMs'] for x in r]),2))
    for n in (10,50,100,500):
        a=json.loads((args.entrada/f'v6-{n}-payload.json').read_text())[0]['familias']
        b=json.loads((args.entrada/f'v7-{n}-payload.json').read_text())[0]['familias']
        assert [(f['codigo'],f['puntaje'],f['f']) for f in a] == [(f['codigo'],f['puntaje'],f['f']) for f in b], 'Carga no comparable'
    for nombre in ('diagnostico.json','entorno.json'):
        if (args.entrada/nombre).exists():
            (args.salida/nombre).write_bytes((args.entrada/nombre).read_bytes())
    files=sorted(p for p in args.salida.iterdir() if p.is_file() and p.name!='SHA256SUMS')
    (args.salida/'SHA256SUMS').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in files))
    for r in resumen:
        print(r['version'],r['familias'],r['fase'],round(r['medianaMs'],2),round(r['p95Ms'],2),round(r['rssMaxMiB'],1))


if __name__=='__main__':
    main()
