import * as THREE from 'three';
import {GLTFLoader} from 'three/addons/loaders/GLTFLoader.js';
import {RoomEnvironment} from 'three/addons/environments/RoomEnvironment.js';
const renderer=new THREE.WebGLRenderer({canvas:document.querySelector('#scene'),antialias:true,preserveDrawingBuffer:true});
renderer.setPixelRatio(1);renderer.setSize(innerWidth,innerHeight);renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.12;renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;
const scene=new THREE.Scene();scene.background=new THREE.Color('#10151b');scene.fog=new THREE.Fog('#10151b',65,130);
const camera=new THREE.PerspectiveCamera(34,innerWidth/innerHeight,.1,200);camera.position.set(28,19,49);camera.lookAt(0,5.5,0);
const env=new THREE.PMREMGenerator(renderer);scene.environment=env.fromScene(new RoomEnvironment(),.04).texture;
scene.add(new THREE.HemisphereLight(0xc2deee,0x4c4140,2));
function light(color,intensity,pos){const l=new THREE.DirectionalLight(color,intensity);l.position.set(...pos);scene.add(l);return l;}
const key=light(0xe8f4ff,4,[-15,30,25]);key.castShadow=true;key.shadow.mapSize.set(2048,2048);Object.assign(key.shadow.camera,{left:-34,right:34,top:28,bottom:-25,near:1,far:100});key.shadow.bias=-.00015;key.shadow.normalBias=.05;
light(0x69d4ed,2,[15,16,-18]);light(0xf0ba79,2,[-22,12,-10]);
const floor=new THREE.Mesh(new THREE.PlaneGeometry(250,250),new THREE.MeshStandardMaterial({color:'#10171d',roughness:.88,metalness:.12}));floor.rotation.x=-Math.PI/2;floor.position.y=-.045;floor.receiveShadow=true;scene.add(floor);
const grid=new THREE.GridHelper(90,30,0x354852,0x26363e);grid.position.y=-.03;grid.material.transparent=true;grid.material.opacity=.35;scene.add(grid);
const loader=new GLTFLoader();const bots=[];
for(const [file,x,h] of [['atlas-09',-16,12],['aether-02',16,12]]){
 const gltf=await loader.loadAsync('./assets/'+file+'.glb');const model=gltf.scene;model.updateMatrixWorld(true);const box=new THREE.Box3().setFromObject(model);const size=box.getSize(new THREE.Vector3());const scale=h/size.y;
 const wrapper=new THREE.Group();scene.add(wrapper);model.scale.setScalar(scale);model.position.y=-box.min.y*scale;wrapper.add(model);wrapper.position.x=x;
 model.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;o.frustumCulled=false;if(o.material)o.material.envMapIntensity=.9;}});
 const mixer=new THREE.AnimationMixer(model);const actions={};for(const clip of gltf.animations){actions[clip.name]=mixer.clipAction(clip);actions[clip.name].play();actions[clip.name].setEffectiveWeight(0);}
 bots.push({name:file,model,wrapper,mixer,actions,clips:gltf.animations,scale,initialX:x,baseY:model.position.y,muzzle:model.getObjectByName('Muzzle_R')});
}
function apply(b,name,time,weight=1){const a=b.actions[name];if(!a)return;a.enabled=true;a.setEffectiveWeight(weight);a.time=Math.min(time,b.clips.find(c=>c.name===name).duration-.00001);}
const captions=[['01 / Siluet & material','Dua model 3D ber-rig. Gerakan datang daripada animasi tulang, bukan sprite yang digerakkan.'],['02 / Larian berartikulasi','Kaki, pinggul, torso dan lengan bergerak melalui rig. Kelajuan akar masih perlu penalaan kontak kaki.'],['03 / Aim · recoil · recovery','Klip KneelFire asal: merendah, membidik dan pulih. Kajian gerakan, bukan koreografi pertempuran siap.'],['04 / Evasion & weight','AETHER menguji DodgeRoll; ATLAS kembali ke Sentinel. Ujian arah, kontak lantai dan armor.'],['05 / Semakan visual dahulu','Belum dimasukkan ke Android. Prestasi TV, peralihan aksi dan penerimaan visual masih perlu diuji.']];
let current=0;
window.renderAt=(t,draw=true)=>{
 current=t;
 const distance=Math.min(...[2,5,13,16].map(c=>Math.abs(t-c)));renderer.domElement.style.opacity=Math.min(1,distance/.15);
 for(const b of bots){for(const a of Object.values(b.actions))a.setEffectiveWeight(0);b.wrapper.position.set(b.initialX,0,0);b.wrapper.rotation.y=.15;b.model.position.y=b.baseY;
  if(t<2){apply(b,'Sentinel',t);b.wrapper.rotation.y=b.initialX<0?.35:-.35;}
  else if(t<5){const u=(t-2)/3;b.wrapper.position.x=b.initialX*(1-u*.45);b.wrapper.rotation.y=b.initialX<0?Math.PI/2:-Math.PI/2;const dur=b.clips.find(c=>c.name==='Run').duration;apply(b,'Run',(t-2)%dur);}
  else if(t<13){b.wrapper.position.x=b.initialX*.55;b.wrapper.rotation.y=b.initialX<0?Math.PI/2:-Math.PI/2;apply(b,'KneelFire',t-5);}
  else if(t<16){b.wrapper.position.x=b.initialX*.55;b.wrapper.rotation.y=b.initialX<0?Math.PI/2:-Math.PI/2;if(b.name==='aether-02'){apply(b,'DodgeRoll',(t-13)/3*2.8666);}else apply(b,'Sentinel',t-13);}
  else{b.wrapper.position.x=b.initialX*.7;b.wrapper.rotation.y=b.initialX<0?.35:-.35;apply(b,'Sentinel',t-16);}
  if(innerWidth<600)b.wrapper.position.x*=.65;
  b.mixer.update(0);b.wrapper.updateMatrixWorld(true);
 }
 const p=t<2?0:t<5?1:t<13?2:t<16?3:4;document.querySelector('#phase').textContent=captions[p][0];document.querySelector('#description').textContent=captions[p][1];document.querySelector('#progress').style.width=`${Math.min(100,t/18*100)}%`;
 camera.aspect=innerWidth/innerHeight;scene.fog.near=innerWidth<600?90:65;scene.fog.far=innerWidth<600?170:130;camera.fov=innerWidth<600?42:34;camera.position.set(innerWidth<600?0:28,innerWidth<600?20:19,innerWidth<600?90:49);camera.lookAt(0,5.5,0);camera.updateProjectionMatrix();if(draw)renderer.render(scene,camera);return {time:t,phase:p,drawCalls:renderer.info.render.calls,triangles:renderer.info.render.triangles};
};
window.bots=bots;window.THREE=THREE;window.camera=camera;window.renderer=renderer;
window.inspectRig=()=>bots.map(b=>({name:b.name,bones:(()=>{let a=[];b.model.traverse(x=>{if(x.isBone)a.push(x.name)});return a;})(),clips:b.clips.map(c=>({name:c.name,duration:c.duration,tracks:c.tracks.length})),skinnedMeshes:(()=>{let n=0;b.model.traverse(x=>{if(x.isSkinnedMesh)n++});return n;})(),bounds:new THREE.Box3().setFromObject(b.model).getSize(new THREE.Vector3()).toArray()}));
window.ready=true;document.querySelector('#loading').style.display='none';const q=new URLSearchParams(location.search);window.renderAt(Number(q.get('t')||0));
if(q.has('play')){let start;function frame(now){if(!start)start=now;window.renderAt((now-start)/1000%18);requestAnimationFrame(frame)}requestAnimationFrame(frame);}
window.addEventListener('resize',()=>{renderer.setSize(innerWidth,innerHeight);window.renderAt(current)});
