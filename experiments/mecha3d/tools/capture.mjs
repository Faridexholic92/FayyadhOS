import {chromium} from 'playwright';
import {mkdir,writeFile} from 'node:fs/promises';
import {fileURLToPath,pathToFileURL} from 'node:url';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const out=path.join(root,'output');await mkdir(path.join(out,'frames'),{recursive:true});
const executablePath=process.env.CHROMIUM_PATH||execFileSync('which',['chromium'],{encoding:'utf8'}).trim();
const browser=await chromium.launch({executablePath,headless:true,args:['--no-sandbox','--allow-file-access-from-files','--enable-unsafe-swiftshader','--use-gl=angle','--use-angle=swiftshader']});
const page=await browser.newPage({viewport:{width:1280,height:720},deviceScaleFactor:1});
const errors=[];page.on('pageerror',e=>errors.push(e.message));page.on('requestfailed',r=>errors.push(r.url()+': '+r.failure().errorText));
try{
 await page.goto(pathToFileURL(path.join(root,'index.html')).href);await page.waitForFunction(()=>window.ready,null,{timeout:90000});
 const rig=await page.evaluate(()=>window.inspectRig());await writeFile(path.join(out,'rig-runtime.json'),JSON.stringify(rig,null,2));
 const fps=24,seconds=18,frames=[];
 for(let i=0;i<fps*seconds;i++){
  const t=i/fps;const info=await page.evaluate(t=>window.renderAt(t),t);
  const f=String(i).padStart(5,'0')+'.jpg';const bytes=await page.screenshot({path:path.join(out,'frames',f),type:'jpeg',quality:94,timeout:90000});
  frames.push({...info,file:f,sha256:createHash('sha256').update(bytes).digest('hex')});
  if(i%48===0)console.log(`Frame ${i}/${fps*seconds}`);
 }
 if(errors.length)throw Error(errors.join('\n'));
 await writeFile(path.join(out,'capture-report.json'),JSON.stringify({kind:'offline desktop WebGL capture, not a TV benchmark',fps,seconds,width:1280,height:720,frames,errors},null,2));
 await page.close();await browser.close();
 const output=path.join(out,'FayyadhOS-Mecha3D-Desktop-Prototype.mp4');
 execFileSync('ffmpeg',['-hide_banner','-loglevel','error','-y','-framerate',String(fps),'-i',path.join(out,'frames','%05d.jpg'),'-c:v','libx264','-preset','fast','-crf','19','-pix_fmt','yuv420p','-movflags','+faststart','-an',output],{stdio:'inherit'});
 execFileSync('ffmpeg',['-v','error','-i',output,'-f','null','-'],{stdio:'inherit'});
 console.log('Export complete:',output);process.exit(0);
}catch(e){console.error(e);await page.close().catch(()=>{});await browser.close().catch(()=>{});process.exit(1)}
