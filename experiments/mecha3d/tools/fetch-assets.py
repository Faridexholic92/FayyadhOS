"""Fetch immutable upstream assets; reject bytes that differ from the reviewed manifest."""
from pathlib import Path
import urllib.request,hashlib,json
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'assets-manifest.json').read_text())
for item in manifest['files']:
    target=root/item['path'];target.parent.mkdir(parents=True,exist_ok=True)
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()==item['sha256']:
        print('Verified cache:',item['path']);continue
    request=urllib.request.Request(item['source'],headers={'User-Agent':'FayyadhOS-asset-review'})
    data=urllib.request.urlopen(request,timeout=90).read()
    if len(data)!=item['bytes'] or hashlib.sha256(data).hexdigest()!=item['sha256']:
        raise RuntimeError('Pinned SHA-256 / size mismatch: '+item['path'])
    temp=target.with_suffix(target.suffix+'.tmp');temp.write_bytes(data);temp.replace(target)
    print('Fetched and SHA-256 verified:',item['path'])
