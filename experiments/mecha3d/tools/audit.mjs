import {chromium} from 'playwright';
import {mkdir,writeFile} from 'node:fs/promises';
import {fileURLToPath,pathToFileURL} from 'node:url';
import {execFileSync} from 'node:child_process';
import path from 'node:path';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..'),out=path.join(root,'output');await mkdir(out,{recursive:true});
const executablePath=process.env.CHROMIUM_PATH||execFileSync('which',['chromium'],{encoding:'utf8'}).trim();
const browser=await chromium.launch({executablePath,headless:true,args:['--no-sandbox','--allow-file-access-from-files','--enable-unsafe-swiftshader','--use-gl=angle','--use-angle=swiftshader']});
const page=await browser.newPage({viewport:{width:1280,height:720}}),errors=[];page.on('pageerror',e=>errors.push(e.message));
try{
 await page.goto(pathToFileURL(path.join(root,'index.html')).href);await page.waitForFunction(()=>window.ready,null,{timeout:90000});
 const audit=await page.evaluate(()=>{
  const T=window.THREE,rows=[];
  for(let t=0;t<18;t+=.25){window.renderAt(t,false);rows.push({time:t,bots:window.bots.map(b=>{const box=new T.Box3().setFromObject(b.model,true);let left=Infinity,right=-Infinity,top=Infinity,bottom=-Infinity;for(let i=0;i<8;i++){const v=new T.Vector3(i&1?box.max.x:box.min.x,i&2?box.max.y:box.min.y,i&4?box.max.z:box.min.z).project(window.camera);left=Math.min(left,(v.x+1)*640);right=Math.max(right,(v.x+1)*640);top=Math.min(top,(1-v.y)*360);bottom=Math.max(bottom,(1-v.y)*360);}return {name:b.name,minY:box.min.y,maxY:box.max.y,screen:{left,right,top,bottom}}})});}
  const motion=[];for(const t of [2.2,2.45,2.7]){window.renderAt(t,false);motion.push({time:t,models:window.bots.map(b=>({name:b.name,shinQuaternion:b.model.getObjectByName('shinL').quaternion.toArray()}))});}
  return {rig:window.inspectRig(),samples:rows,boneMotion:motion};
 });
 audit.errors=errors;const boxes=audit.samples.flatMap(s=>s.bots);audit.checks={rigsHave18Bones:audit.rig.every(x=>x.bones.length===18),allModelsSkinned:audit.rig.every(x=>x.skinnedMeshes>0),sampledModelsStayInViewport:boxes.every(x=>x.screen.left>=0&&x.screen.right<=1280&&x.screen.top>=150&&x.screen.bottom<=610),groundPenetrationBelowPointOne:boxes.every(x=>x.minY>=-.1),shinRotationChanges:audit.rig.every((_,i)=>JSON.stringify(audit.boneMotion[0].models[i].shinQuaternion)!==JSON.stringify(audit.boneMotion[1].models[i].shinQuaternion)),noPageErrors:errors.length===0};
 await writeFile(path.join(out,'motion-audit.json'),JSON.stringify(audit,null,2));console.log(JSON.stringify(audit.checks));if(!Object.values(audit.checks).every(Boolean))throw Error('Audit check failed; inspect output/motion-audit.json');
 await page.close();await browser.close();process.exit(0);
}catch(e){console.error(e);await page.close().catch(()=>{});await browser.close().catch(()=>{});process.exit(1)}
