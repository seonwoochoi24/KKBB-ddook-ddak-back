#!/usr/bin/env python3
"""Evaluate a running /api/canvas menu selector using predeclared synthetic cases."""
import argparse,json,time,urllib.request,urllib.error,statistics,hashlib
from pathlib import Path
from datetime import datetime,timezone

def main():
 p=argparse.ArgumentParser();p.add_argument('--base-url',default='http://127.0.0.1:18082');p.add_argument('--limit',type=int,default=0);args=p.parse_args()
 root=Path(__file__).resolve().parent;spec=json.loads((root/'cases.json').read_text());cases=spec['cases'][:args.limit or None]
 report={'started_at':datetime.now(timezone.utc).isoformat(),'catalog_sha256':spec['catalog_sha256'],'scope':'synthetic_menu_selection','note':spec['note'],'results':[]}
 path=root/'results.json'
 for case in cases:
  row=dict(case);started=time.monotonic()
  req=urllib.request.Request(args.base_url+'/api/canvas',data=json.dumps({'message':case['message']},ensure_ascii=False).encode(),headers={'Content-Type':'application/json'})
  try:
   with urllib.request.urlopen(req,timeout=75) as response: body=json.load(response)
   row.update(actual_status=body.get('status'),actual_menu_ids=[m['id'] for m in body.get('menus',[])],actual_menu_names=[m['name'] for m in body.get('menus',[])],success=True)
   row['correct']=row['actual_status']==case['expected_status'] and row['actual_menu_ids']==case['expected_menu_ids']
  except urllib.error.HTTPError as e:
   row.update(success=False,correct=False,http_status=e.code)
   try: row['error']=json.loads(e.read()).get('error','Upstream HTTP error')
   except (ValueError,AttributeError): row['error']='HTTP error'
  except (urllib.error.URLError,TimeoutError,ValueError) as e: row.update(success=False,correct=False,error=type(e).__name__)
  row['duration_ms']=round((time.monotonic()-started)*1000);report['results'].append(row)
  path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
  print(json.dumps({k:row.get(k) for k in ['id','correct','actual_status','actual_menu_names','duration_ms','error']},ensure_ascii=False),flush=True)
  if not row['success'] and (len(report['results'])==1 or all(not x['success'] for x in report['results'][-3:])):
   report['stopped_early']=True;break
 rows=report['results'];known=[x for x in rows if x['category']=='specific_menu'];ready=[x for x in known if x.get('actual_status')=='ready']
 report['summary']={'planned':len(cases),'completed':len(rows),'successful':sum(x['success'] for x in rows),'correct':sum(x['correct'] for x in rows),'accuracy_percent':round(100*sum(x['correct'] for x in rows)/len(rows),1),'specific_menu_correct':sum(x['correct'] for x in known),'specific_menu_total':len(known),'specific_menu_selected':len(ready),'specific_menu_precision_percent':round(100*sum(x['correct'] for x in ready)/len(ready),1) if ready else None,'mean_duration_ms':round(statistics.mean(x['duration_ms'] for x in rows)),'by_category':{c:{'correct':sum(x['correct'] for x in rows if x['category']==c),'total':sum(x['category']==c for x in rows)} for c in sorted({x['category'] for x in rows})}}
 path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report['summary'],ensure_ascii=False),flush=True)
if __name__=='__main__':main()
