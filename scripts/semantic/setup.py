#!/usr/bin/env python3
"""Create an isolated runtime and download the pinned public model; sends no catalog data."""
import hashlib,json,subprocess,sys,venv
from pathlib import Path
root=Path(__file__).resolve().parents[2];runtime=root/'.semantic-runtime';venvdir=runtime/'venv';modeldir=runtime/'model';modeldir.mkdir(parents=True,exist_ok=True)
if not (venvdir/'bin/python').exists():venv.EnvBuilder(with_pip=True).create(venvdir)
subprocess.run([str(venvdir/'bin/python'),'-m','pip','install','-r',str(Path(__file__).with_name('requirements.txt'))],check=True)
manifest=json.loads(Path(__file__).with_name('model_manifest.json').read_text())
for name,expected in manifest['files'].items():
 destination=modeldir/name
 if destination.exists() and hashlib.sha256(destination.read_bytes()).hexdigest()==expected:continue
 remote='onnx/model_qint8_avx512_vnni.onnx' if name=='model.onnx' else name
 url='https://huggingface.co/'+manifest['model']+'/resolve/'+manifest['revision']+'/'+remote
 temporary=destination.with_suffix(destination.suffix+'.download')
 subprocess.run(['curl','-fLsS',url,'-o',str(temporary)],check=True)
 if hashlib.sha256(temporary.read_bytes()).hexdigest()!=expected:raise SystemExit('Model checksum mismatch: '+name)
 temporary.replace(destination)
print('Ready. Start local search: '+str(venvdir/'bin/python')+' scripts/semantic/server.py --model-dir '+str(modeldir)+' --catalog src/main/resources/kb_starbanking_menu_structure.json')
