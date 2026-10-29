from pathlib import Path
from PIL import Image
import hashlib,json,base64,sys,shutil
root=Path(__file__).resolve().parents[1];frames=root/'output/launcher-bake';dest=Path(sys.argv[1]);dest.mkdir(parents=True,exist_ok=True)
w,h=112,128;specs=[];issues=[]
for phase,n,cols in [('run',32,8),('fire',64,16),('recover',32,8)]:
 atlas=Image.new('RGBA',(cols*w,((n*2+cols-1)//cols)*h));bounds=[]
 for row in range(2):
  for i in range(n):
   im=Image.open(frames/f'{phase}-{row}-{i:03d}.png').convert('RGBA').resize((w,h),Image.Resampling.LANCZOS);box=im.getchannel('A').point(lambda x:255 if x>16 else 0).getbbox();bounds.append(box)
   if box is None or box[0]<1 or box[1]<1 or box[2]>w-1 or box[3]>h-1:issues.append((phase,row,i,box))
   index=row*n+i;atlas.paste(im,((index%cols)*w,(index//cols)*h))
 webp=frames/(phase+'.webp');atlas.save(webp,'WEBP',quality=84,method=6,exact=True);data=webp.read_bytes();encoded=base64.b64encode(data).decode();chunks=[encoded[i:i+30000] for i in range(0,len(encoded),30000)]
 for i,chunk in enumerate(chunks):(dest/f'{phase}-{i:02d}.b64').write_text(chunk+'\n')
 spec={'name':phase,'frames':n,'columns':cols,'width':atlas.width,'height':atlas.height,'parts':len(chunks),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()};specs.append(spec);print(spec)
manifest={'schema':1,'kind':'pre-rendered rigged 3D; not real-time 3D','tileWidth':w,'tileHeight':h,'models':['atlas-09','aether-02'],'upstreamCommit':'1b8fb9d54160215c071c5a29a49b1c36dc01f0df','atlases':specs}
(dest/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n');(frames/'pack-audit.json').write_text(json.dumps({'clipping_issues':issues,'tiles':256,'decoded_argb_bytes':sum(x['width']*x['height']*4 for x in specs)},indent=2)+'\n')
print('Clipping issues',len(issues),issues[:12]);print('Decoded bytes',sum(x['width']*x['height']*4 for x in specs));
if issues:raise SystemExit('Correct framing before using assets')
