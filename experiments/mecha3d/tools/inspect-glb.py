from pathlib import Path
import struct,json
root=Path(__file__).resolve().parents[1];result={}
for p in sorted((root/'assets').glob('*.glb')):
 b=p.read_bytes();magic,version,total=struct.unpack_from('<III',b);assert magic==0x46546c67 and version==2 and total==len(b)
 size,typ=struct.unpack_from('<II',b,12);g=json.loads(b[20:20+size]);nodes=g.get('nodes',[]);access=g.get('accessors',[])
 meshes=[]
 for m in g.get('meshes',[]):
  meshes.append({'name':m.get('name'),'primitives':[{'vertices':access[x['attributes']['POSITION']]['count'],'indices':access[x['indices']]['count'] if 'indices' in x else None,'joints':'JOINTS_0' in x['attributes']} for x in m['primitives']]})
 clips=[]
 for a in g.get('animations',[]):
  duration=max((max(access[s['input']].get('max',[0])) for s in a['samplers']),default=0)
  clips.append({'name':a.get('name'),'duration':duration,'channels':len(a['channels']),'targets':sorted(set(c['target']['path'] for c in a['channels']))})
 info={'file_bytes':len(b),'nodes':len(nodes),'meshes':meshes,'skins':[{'name':s.get('name'),'bones':len(s['joints']),'joint_names':[nodes[j].get('name') for j in s['joints']]} for s in g.get('skins',[])],'animations':clips,'materials':[m.get('name') for m in g.get('materials',[])],'images':len(g.get('images',[]))};result[p.name]=info
 print(p.name,'meshes',len(meshes),'skins',len(info['skins']),'animations',clips)
(root/'asset-inventory.json').write_text(json.dumps(result,indent=2)+'\n')
