#!/usr/bin/env python3
"""Local E5 retrieval. Requests and catalog stay on this computer; no remote inference."""
import argparse, hashlib, json, threading, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

REVISION = '614241f622f53c4eeff9890bdc4f31cfecc418b3'
MODEL = 'intfloat/multilingual-e5-small'

def main():
    p=argparse.ArgumentParser();p.add_argument('--model-dir',type=Path,required=True);p.add_argument('--catalog',type=Path,required=True);p.add_argument('--port',type=int,default=18083);args=p.parse_args()
    raw=args.catalog.read_bytes();digest=hashlib.sha256(raw).hexdigest();menus=json.loads(raw)['menus'];ids=[m['id'] for m in menus]
    tok=Tokenizer.from_file(str(args.model_dir/'tokenizer.json'));tok.enable_truncation(max_length=256);tok.enable_padding(pad_id=1,pad_token='<pad>')
    opts=ort.SessionOptions();opts.intra_op_num_threads=2;opts.inter_op_num_threads=1
    session=ort.InferenceSession(str(args.model_dir/'model.onnx'),sess_options=opts,providers=['CPUExecutionProvider'])
    lock=threading.Lock();inputs={x.name for x in session.get_inputs()}
    def encode(texts):
        with lock:
            enc=tok.encode_batch(texts);tokens=np.array([x.ids for x in enc],dtype=np.int64);mask=np.array([x.attention_mask for x in enc],dtype=np.int64)
            feed={'input_ids':tokens,'attention_mask':mask}
            if 'token_type_ids' in inputs:feed['token_type_ids']=np.zeros_like(tokens)
            output=session.run(None,feed)[0];weight=mask[...,None]
            pooled=(output*weight).sum(axis=1)/np.maximum(weight.sum(axis=1),1)
            return (pooled/np.maximum(np.linalg.norm(pooled,axis=1,keepdims=True),1e-12)).astype(np.float32)
    cache=args.model_dir/('menus-'+digest+'-'+REVISION+'.npz')
    if cache.exists():
        with np.load(cache,allow_pickle=False) as saved: vectors=saved['vectors']
        if vectors.shape != (len(menus),384):raise ValueError('Invalid cached embedding shape')
    else:
        texts=['passage: '+m['name']+'. '+m['description']+' '+' '.join(m['select_when']) for m in menus]
        parts=[]
        for i in range(0,len(texts),16):parts.append(encode(texts[i:i+16]))
        vectors=np.vstack(parts);np.savez_compressed(cache,vectors=vectors)
    class Handler(BaseHTTPRequestHandler):
        def log_message(self,*args):pass
        def respond(self,code,obj):
            content=json.dumps(obj,ensure_ascii=False).encode();self.send_response(code);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(content)));self.end_headers();self.wfile.write(content)
        def do_GET(self):
            self.respond(200 if self.path=='/health' else 404,{'model':MODEL,'revision':REVISION,'catalog_sha256':digest,'menus':len(menus)})
        def do_POST(self):
            if self.path!='/search':return self.respond(404,{'error':'not_found'})
            try:
                size=int(self.headers.get('Content-Length','0'))
                if size<1 or size>20000:return self.respond(400,{'error':'invalid_size'})
                body=json.loads(self.rfile.read(size));q=body.get('message','');limit=body.get('limit',80)
                if not isinstance(q,str) or not q.strip() or len(q)>2000 or not isinstance(limit,int) or not 1<=limit<=100:return self.respond(400,{'error':'invalid_request'})
                if body.get('catalog_sha256')!=digest:return self.respond(409,{'error':'catalog_mismatch'})
                started=time.perf_counter();query=encode(['query: '+q])[0];scores=vectors@query
                order=np.argsort(-scores,kind='stable')[:limit]
                self.respond(200,{'ids':[ids[i] for i in order],'duration_ms':round((time.perf_counter()-started)*1000,2),'catalog_sha256':digest})
            except (ValueError,TypeError,KeyError) as ex:
                print('semantic_error '+type(ex).__name__+': '+str(ex)[:160],flush=True)
                self.respond(400,{'error':'invalid_request'})
    print(json.dumps({'status':'ready','model':MODEL,'menus':len(menus),'port':args.port}),flush=True)
    ThreadingHTTPServer(('127.0.0.1',args.port),Handler).serve_forever()
if __name__=='__main__':main()
