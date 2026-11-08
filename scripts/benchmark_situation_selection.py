#!/usr/bin/env python3
"""Paired real JEV benchmark; one request per mode/case. Requires running application instances."""
import argparse,json,time,urllib.request,urllib.error,statistics,math,hashlib
from pathlib import Path

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--cases',type=Path,default=Path(__file__).with_name('situation-cases.json'))
    p.add_argument('--modes',nargs='+',default=['previous=http://127.0.0.1:18085','situation=http://127.0.0.1:18086'])
    p.add_argument('--output',type=Path,required=True);p.add_argument('--limit',type=int,default=0);args=p.parse_args()
    if args.limit<0:p.error('limit cannot be negative')
    modes=[tuple(x.split('=',1)) for x in args.modes];cases=json.loads(args.cases.read_text())['cases'][:args.limit or None]
    report={'case_sha256':hashlib.sha256(args.cases.read_bytes()).hexdigest(),'results':[],'summary':{}}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    for i,case in enumerate(cases):
        for mode,url in modes[i%len(modes):]+modes[:i%len(modes)]:
            row={**case,'mode':mode};started=time.monotonic()
            req=urllib.request.Request(url.rstrip('/')+'/api/canvas',data=json.dumps({'message':case['message']},ensure_ascii=False).encode(),headers={'Content-Type':'application/json'})
            try:
                with urllib.request.urlopen(req,timeout=40) as response:body=json.load(response)
                row.update(success=True,actual_status=body.get('status'),actual_menu_ids=[m['id'] for m in body.get('menus',[])],metrics=body.get('selection_metrics'))
                row['correct']=row['actual_status']==case['expected_status'] and set(row['actual_menu_ids'])==set(case['expected_menu_ids'])
            except (urllib.error.URLError,TimeoutError,ValueError) as ex:row.update(success=False,correct=False,error=type(ex).__name__)
            row['duration_ms']=round((time.monotonic()-started)*1000);report['results'].append(row)
            args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(case['id'],mode,row['correct'],row['duration_ms'],flush=True)
            if not row['success']:raise SystemExit('API failed; incomplete run is not an accuracy estimate')
            if mode=='situation' and (row.get('metrics') or {}).get('retrievalMode')!='semantic_only':raise SystemExit('Unexpected non-semantic retrieval; invalid comparison')
    for mode,_ in modes:
        rows=[r for r in report['results'] if r['mode']==mode];latencies=sorted(r['duration_ms'] for r in rows)
        report['summary'][mode]={'correct':sum(r['correct'] for r in rows),'total':len(rows),'mean_ms':round(statistics.mean(latencies),1),'p95_ms':latencies[math.ceil(len(rows)*.95)-1]}
    args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print(json.dumps(report['summary'],ensure_ascii=False))
if __name__=='__main__':main()
