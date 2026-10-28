#!/usr/bin/env python3
"""Audit actual DEX class definitions, not merely referenced class-name strings."""
import sys,struct,zipfile,xml.etree.ElementTree as E
from pathlib import Path
root=Path(__file__).resolve().parents[1]
apk=Path(sys.argv[1]) if len(sys.argv)>1 else root/'app/build/outputs/apk/debug/app-debug.apk'
def u32(b,p):return struct.unpack_from('<I',b,p)[0]
def classes(b):
 assert b[:4]==b'dex\n','Invalid DEX'
 strings=u32(b,60);types=u32(b,68);count=u32(b,96);base=u32(b,100)
 result=set()
 for i in range(count):
  typeidx=u32(b,base+32*i);stringidx=u32(b,types+4*typeidx);off=u32(b,strings+4*stringidx)
  while b[off]&128:off+=1
  off+=1
  result.add(b[off:b.index(b'\0',off)].decode('utf-8'))
 return result
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None,'Corrupt ZIP'
 found=set().union(*(classes(z.read(n)) for n in z.namelist() if n.endswith('.dex')))
manifest=E.parse(root/'app/src/main/AndroidManifest.xml').getroot();ns='{http://schemas.android.com/apk/res/android}'
expected=[]
for tag in ['activity','service','receiver','provider']:
 for node in manifest.findall('application/'+tag):
  name=node.get(ns+'name')
  if name.startswith('.'):name='com.gathin.tvhome'+name
  expected.append('L'+name.replace('.','/')+';')
missing=sorted(set(expected)-found)
appclasses=sorted(x for x in found if x.startswith('Lcom/gathin/tvhome/') and x!='Lcom/gathin/tvhome/R;' and not x.startswith('Lcom/gathin/tvhome/R$'))
print('APK:',apk.name,'; app class definitions:',len(appclasses),'; manifest components:',len(expected))
if missing:raise SystemExit('FAIL: missing component class definitions: '+', '.join(missing))
assert len(appclasses)>50,'Application bytecode is incomplete'
print('PASS: all declared application components have DEX class definitions')
