REAL-TIME SUMMARIZATION OF SOCIAL-MEDIA INFORMATION STREAMS - Web Search 2017/2018
----------------------------------------------------------------------------------

### Twitter Daily Digest ###

# Autores #
---------
José Castanheira - 41659
Thales Parreira - 41835
Tiago Santo - 41658


# Exemplo de configuração #
-----------------------------

**Mudar os paths nos parâmetros abaixo**

-f "E:\datasets\rts2016-qrels-tweets2016.jsonl"
-q "E:\datasets\TREC2016-RTS-topics.json"
-e "E:\datasets\rts2016-qrels.txt"
-fusion	60
-r
"BM25"	-index	
-bm25	1.2	0.75
-r
"LMD"	-index	
-lmd	2000
-r
"Classic"
-classic

-r	uma run de um ranking, com as especificações seguintes descritas à frente

-f  <tweetsfile>	path do ficheiro com os tweets.

-q	<topicsfile>	path do ficheiro com os tópicos(as queries).

-fusion <k>	para fazer rank fusion com k (=60 no exemplo), todos -r que vierem depois fazem parte do fuse

-r "BM25" -index -bm25 <k1> <b> faz um ranking com o BM25 e parâmetros k1 e b	(=1.2 e =0.75 no exemplo, respetivamente)

-r "LMD" -index -lmd <mu> faz um ranking com o Language Model with Dirichlet Smoothing e o parâmetro mu	(=2000 no exemplo)

-r "Classic" -classic faz um ranking com o Vector Space Model

# Output #
----------

"tmp.txt" - rankings de todos os dias para cada interest profile, com o formato utilizado pelo trec_eval:	topic_id Q0 tweet_id rank score runtag
"results<runID>.txt" - rankings de todos os dias para cada interest profile, da run runID, com o formato pedido no enunciado:		YYYYMMDD topic_id Q0 tweet_id rank score runtag
"digests<runID>.txt" - rankings de todos os dias para cada interest profile, da run runID, com um formato mais familiar para o utilizador: 	YYYYMMDD topic_id Q0 tweet_id rank runtag tweet_body
