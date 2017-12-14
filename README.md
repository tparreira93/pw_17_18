# Twitter project

## Exemplo de configuração

**Mudar os paths nas nos parâmetros abaixo**

-f "E:\datasets\rts2016-qrels-tweets2016.jsonl"
-q "E:\datasets\TREC2016-RTS-topics.json"
-e "E:\datasets\rts2016-qrels.txt"
-fuse
60
-r
-index
"BM25"
-bm25
1.2
0.75
-r
-index
"LMD"
-lmd
2000
-r
"Classic"
-classic

O -f é o path do ficheiro com os tweets.

O -q é o path do ficheiro com os tópicos(as queries).

-fuse 60 para fazer rank fusion com k = 60, todos -r que vierem depois fazer parte do fuse

-r "BM25" -bm25 1.2 0.75 faz um ranking com nome "BM25" a similarity a utilizar é o bm25 e k1=1.2 e b=0.75

-r "LMD" -lmd 2000 faz um ranking com nome "LMD" a similarity a utilizar é o lmd emuk1=2000

-r "Classic" -classic faz um ranking com o vector space model