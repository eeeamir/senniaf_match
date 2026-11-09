#!/usr/bin/env python3
"""Sube únicamente un dump remoto mediante identidad administrada; nunca imprime tokens."""
import datetime,email.utils,hashlib,json,os,pathlib,sys,urllib.parse,urllib.request

def main():
    p=pathlib.Path(sys.argv[1]).resolve()
    if p.parent!=pathlib.Path('/var/backups/senniaf') or p.suffix!='.dump' or not p.is_file():raise ValueError('Ruta de respaldo no autorizada')
    account=os.environ['AZURE_BACKUP_ACCOUNT'];container=os.environ.get('AZURE_BACKUP_CONTAINER','backups')
    if not account.isalnum() or not container.replace('-','').isalnum():raise ValueError('Configuración de almacenamiento inválida')
    op=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    u='http://169.254.169.254/metadata/identity/oauth2/token?'+urllib.parse.urlencode({'api-version':'2018-02-01','resource':'https://storage.azure.com/'})
    with op.open(urllib.request.Request(u,headers={'Metadata':'true'}),timeout=15) as r:token=json.load(r)['access_token']
    data=p.read_bytes();sha=hashlib.sha256(data).hexdigest()
    blob='postgres/'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%d')+'/'+p.name
    u='https://'+account+'.blob.core.windows.net/'+container+'/'+blob
    headers={'Authorization':'Bearer '+token,'x-ms-version':'2023-11-03','x-ms-date':email.utils.formatdate(usegmt=True),'x-ms-blob-type':'BlockBlob','x-ms-meta-sha256':sha,'If-None-Match':'*','Content-Type':'application/octet-stream'}
    with op.open(urllib.request.Request(u,data=data,method='PUT',headers=headers),timeout=60) as r:
        if r.status!=201:raise RuntimeError('Carga no confirmada')
    print('Copia privada confirmada; SHA256='+sha)
if __name__=='__main__':
    try:main()
    except Exception as e:
        print('No se completó la copia externa: '+type(e).__name__,file=sys.stderr);sys.exit(1)
