# Local semantic search

The multilingual E5-small ONNX model runs locally; menu text and questions are not sent to an embedding API. JEV still receives the selected menu candidates for final selection.

Setup once (Python 3.9; platform wheel availability may vary):

```sh
python3 scripts/semantic/setup.py
```

Start this process before the Spring application:

```sh
sh scripts/semantic/start.sh
```

The process binds only to 127.0.0.1:18083. Menu embeddings are cached by catalog SHA-256 and model revision. Model changes require a different cache revision. Document embeddings use `passage:`, query embeddings use `query:`, attention-mask mean pooling and L2 normalization. The public model and tokenizer are pinned and checksum verified. The model uses int8 ONNX, CPU execution, and two inference threads.

The canvas uses semantic search only, after JEV has inferred up to three needs. There is no lexical fallback: model outages return HTTP503. The original hybrid retrieval classes remain deprecated for historical experiments and are not registered as Spring services. MENU_SEMANTIC_URL defaults to http://127.0.0.1:18083/search and must be a loopback HTTP address.

Situation needs are defined in src/main/resources/situation_needs.json. JEV chooses applicable needs before this service is queried. A single need retrieves40 menus; multiple needs retrieve12 each and are resolved in one batched JEV decision. Recommendations are tagged explicit/suggested and never execute financial actions.

Sources:
- https://huggingface.co/intfloat/multilingual-e5-small
- https://www.elastic.co/docs/solutions/search/hybrid-search
