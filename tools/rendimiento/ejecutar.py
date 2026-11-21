"""Reproduce la sonda manual aislada. No modifica código productivo ni conecta PostgreSQL.
JAVA_HOME=<jdk17> python3 tools/rendimiento/ejecutar.py /tmp/nueva-medicion [--chromium /ruta/chromium]
"""
import argparse
import json
import os
from pathlib import Path
import platform
import subprocess
import time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('salida', type=Path)
    parser.add_argument('--chromium')
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    os.chdir(repo)
    out = args.salida.resolve(); out.mkdir(exist_ok=False, parents=True)
    java = Path(os.environ['JAVA_HOME'])/'bin'
    assert '17.' in subprocess.check_output([str(java/'java'), '-version'], stderr=subprocess.STDOUT).decode()
    assert subprocess.check_output(['git', 'branch', '--show-current']).decode().strip() == 'mejora/sprint9'
    assert not subprocess.check_output(['git', 'diff', 'HEAD', '--', 'src/main']).strip(), 'Cambios productivos sin commit'
    commits = dict(v6=subprocess.check_output(['git', 'rev-parse', '78a2ee0']).decode().strip(),
                   v7=subprocess.check_output(['git', 'rev-parse', 'HEAD']).decode().strip())
    permitted = {'src/main/java/com/senniaf/match/service/'+x+'.java'
                 for x in ['ScoringService', 'AnalizadorTextoCompatibilidad']}
    changed = set(subprocess.check_output(['git', 'diff', '--name-only', commits['v6'], commits['v7'], '--', 'src/main/java']).decode().splitlines())
    assert changed <= permitted, 'La comparación requiere compilar otras diferencias productivas'
    cpfile = out/'dependencias.txt'
    subprocess.run(['mvn', '-q', '-DskipTests', 'test-compile', 'dependency:build-classpath',
                    '-Dmdep.outputFile='+str(cpfile)], check=True)
    cp = str(repo/'target/classes')+':'+cpfile.read_text().strip()
    for v, commit in commits.items():
        d = out/v; d.mkdir()
        for cls in ['ScoringService', 'AnalizadorTextoCompatibilidad']:
            (d/(cls+'.java')).write_bytes(subprocess.check_output(['git', 'show', commit+':src/main/java/com/senniaf/match/service/'+cls+'.java']))
        subprocess.run([str(java/'javac'), '-cp', cp, '-d', str(d), *map(str,d.glob('*.java')),
                        'tools/rendimiento/MedirRanking.java', 'tools/rendimiento/DiagnosticarRanking.java'], check=True)
    meta = dict(commits=commits, inicio=time.strftime('%Y-%m-%dT%H:%M:%S%z'), sistema=platform.platform(),
                cpu=Path('/proc/cpuinfo').read_text().split('model name')[1].splitlines()[0],
                memoria=Path('/proc/meminfo').read_text(), opcionesJvm=['-Xms128m','-Xmx512m','-XX:+UseG1GC'],
                calentamiento=12, repeticionesPorFork=30, forks=2, concurrentes=4,
                lotesConcurrentes=12, lotesConcurrentesDescartados=2)
    (out/'entorno.json').write_text(json.dumps(meta, indent=2))
    with (out/'ejecucion.log').open('w') as log:
        for fork in (1, 2):
            for n in (10, 50, 100, 500):
                for v in (('v6','v7') if fork == 1 else ('v7','v6')):
                    subprocess.run([str(java/'java'), *meta['opcionesJvm'], '-cp', str(out/v)+':'+cp,
                                    'com.senniaf.match.service.MedirRanking', str(n), v, str(out), str(fork)],
                                   stdout=log, stderr=log, check=True)
                    print('FIN', v, n, fork, flush=True)
        subprocess.run([str(java/'java'), *meta['opcionesJvm'], '-cp', str(out/'v7')+':'+cp,
                        'com.senniaf.match.service.DiagnosticarRanking', str(out)], stdout=log, stderr=log, check=True)
    if args.chromium:
        subprocess.run(['python3', 'tools/rendimiento/medir_interfaz.py', str(out), '--chromium', args.chromium], check=True)


if __name__ == '__main__':
    main()
