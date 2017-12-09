package tutorials.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import tutorials.configurations.Expand;
import tutorials.configurations.RunConfig;
import tutorials.utils.CSVUtils;
import tutorials.utils.CommandLine;
import tutorials.utils.DataSetTrec;
import tutorials.utils.ResultDocs;
import twitter4j.Status;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TwitterIndexer {
	String queriesPath = "queries.offline.txt";
	IndexWriterConfig iwc = null;

	public final static int TOP = 100;
	public final static int TOPDOCS = 25;
	public final static int NUMDOCS = 10;
	public final static int NUM_TOP_WORDS = 5;

	private Map<LocalDate, IndexWriter> indexes = new HashMap<>();
	private List<DataSetTrec> trecs;
	private LocalDate startDate;
	private LocalDate endDate;

	boolean create = true;

	public TwitterIndexer(List<DataSetTrec> t, LocalDate startDate, LocalDate endDate) {
		this.trecs = t;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	private IndexWriter idx;

	public void openIndex(Analyzer analyzer, String indexPath) {
		try {

			indexes.clear();
			LocalDate date = startDate;
			while (date.isBefore(endDate) || date.isEqual(endDate))
			{
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String path = indexPath + "\\" + date.format(formatter);
				// ====================================================
				// Select the data analyser to tokenise document data

				// ====================================================
				// Configure the index to be created/opened
				//
				// IndexWriterConfig has many options to be set if needed.
				//
				// Example: for better indexing performance, if you
				// are indexing many documents, increase the RAM
				// buffer. But if you do this, increase the max heap
				// size to the JVM (eg add -Xmx512m or -Xmx1g):
				//
				// iwc.setRAMBufferSizeMB(256.0);
				IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
				if (create) {
					// Create a new index, removing any
					// previously indexed documents:
					iwc.setOpenMode(OpenMode.CREATE);
				} else {
					// Add new documents to an existing index:
					iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				}

				Directory dir = FSDirectory.open(Paths.get(path));
				IndexWriter idx = new IndexWriter(dir, iwc);
				indexes.put(date, idx);
				date = date.plusDays(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void indexTweets(List<Status> tweets) {
		for(int i = 0; i < tweets.size(); i++) {
			indexDoc(tweets.get(i));
		}
	}

	public void indexDoc(Status status) {

		Document doc = new Document();

		try {
			doc.add(new LongPoint("Id", status.getId()));
			doc.add(new StoredField("Id", status.getId()));
			doc.add(new LongPoint("UserId", status.getUser().getId()));

			doc.add(new LongPoint("CreationDate", status.getCreatedAt().getTime()));

			doc.add(new TextField("Body", status.getText(), Field.Store.YES));
			LocalDate date = status.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			for (Map.Entry<LocalDate, IndexWriter> entry : indexes.entrySet()) {
				LocalDate indexDate = entry.getKey();
				IndexWriter idx = entry.getValue();
				if (date.isBefore(indexDate) || date.isEqual(indexDate)) {
					addOrUpdate(idx, doc, status.getId());
				}
			}
		} catch (IOException e) {
			System.out.println("Error adding document " + status.getId());
		} catch (Exception e) {
			System.out.println("Error parsing document " + status.getId());
		}
	}

	private void addOrUpdate(IndexWriter idx, Document doc, long id) throws IOException {
		// ====================================================
		// Add the document to the index
		if (idx.getConfig().getOpenMode() == OpenMode.CREATE) {
			idx.addDocument(doc);

		} else {
			idx.updateDocument(new Term("Id", ((Long)id).toString()), doc);
		}
	}

	// ====================================================
	// ANNOTATE THIS METHOD YOURSELF
	public void indexSearch(RunConfig config, String indexPath) {
		IndexReader reader = null;
		String resultsFiles = "results_" + indexPath + ".txt";
		String line;
		Analyzer analyzer = config.getAnalyzer();
		Similarity similarity = config.getSimilarityConfiguration();
		Expand expand = config.getExpand();
		try {
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(queriesPath), StandardCharsets.UTF_8));
			List<ResultDocs> resultsDocs = new LinkedList<ResultDocs>();
			while ((line = in.readLine()) != null) {
				String qId = line.split(":")[0];
				if (line == null || line.length() == -1) {
					break;
				}

				line = line.trim();
				if (line.length() == 0) {
					break;
				}

				Query query;
				try {
					if (expand.isExpand())
						query = expandQuery(line, searcher, analyzer, expand.getWeight(), reader);
					else {
						QueryParser parser = new QueryParser("Body", analyzer);
						query = parser.parse(line);
					}
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					System.out.println("Error parsing query string.");
					continue;
				}

				TopDocs results = searcher.search(query, NUMDOCS);
				ScoreDoc[] hits = results.scoreDocs;

				for (int j = 0; j < hits.length; j++) {
					ScoreDoc c = hits[j];
					Document doc = searcher.doc(c.doc);
					Integer Id = doc.getField("Id").numericValue().intValue();
					resultsDocs.add(new ResultDocs(qId, Id, c.score, doc));
				}

				if (line.equals("")) {
					break;
				}
			}

			in.close();
			reader.close();

			AddTREAC(analyzer, similarity, resultsFiles, resultsDocs, expand);


		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	void AddTREAC(Analyzer analyzer, Similarity similarity, String resultsFiles,
				  List<ResultDocs> resultsDocs,
				  Expand expand) {
		BufferedWriter out = null;
		String result = "";
		Collections.sort(resultsDocs);

		String q = ResultDocs.CONST_QO;
		String run = ResultDocs.CONST_RUN;
		for (ResultDocs r : resultsDocs) {
			result += r.getQueryId() + " " + q + " " + r.getDocId() + " " + r.getRank() + " " + r.getScore() + " "
					+ run + "\n";
		}

		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFiles)));
			out.write(result);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		CommandLine c = new CommandLine();
		String resultTrec = c.executeCommand("trec_eval.exe qrels.offline.txt " + resultsFiles);

		BufferedReader bufReader = new BufferedReader(new StringReader(resultTrec));

		String l = null;
		String tmp = analyzer.toString() + "@" + similarity.toString();
		if(expand.isExpand())
			tmp += " with expansion (alfa=" + Math.round(expand.getWeight()*100f)/100f + ")";
		else
			tmp += " no expansion";
		DataSetTrec t = new DataSetTrec(tmp);
		System.out.println(tmp);
		System.out.println(resultTrec);
		try {
			while ((l = bufReader.readLine()) != null) {

                String[] values = l.split("\\s+");
                String k = values[0];
                if (k.equals("map")) {
                    String[] a = l.split("\\s+");
                    t.setMap(Float.parseFloat(a[a.length - 1]));
                }

                if (k.contains("iprec")) {
                    t.addRecall(values[0].split("_")[3], Float.parseFloat(values[values.length - 1]));
                }

                if (k.contains("P_")) {
                    t.addPage(values[0].split("_")[1], Float.parseFloat(values[values.length - 1]));
                }

            }
		} catch (IOException e) {
			e.printStackTrace();
		}

		trecs.add(t);
	}

	public void close() {
		try {
			for (Map.Entry<LocalDate, IndexWriter> entry : indexes.entrySet()) {
				IndexWriter idx = entry.getValue();
				idx.close();
			}
		} catch (IOException e) {
			System.out.println("Error closing the index.");
		}
	}

	public static void printUsage() {
		System.out.println("web search - Project 1\n");
		System.out.println("Usage example:");
		System.out.println("[(-h|--help)] [(filter) string]");
		System.out.println("Options:");
		System.out.println("-h or --help: Help.");
		System.out.println("Filters:");
		System.out.println("-sf: Stop Filter.");
		System.out.println("-shf: Shingle Filter");
		System.out.println("\t MinShingleSize integer");
		System.out.println("\t MaxShingleSize integer");
		System.out.println("-snf: Snowball Filter");
		System.out.println("-stdf: Standard Filter");
		System.out.println("-lcf: Lower Case Filter");
		System.out.println("-ngtf: N-Gram Token Filter");
		System.out.println("\t MinGram integer");
		System.out.println("\t MaxGram integer");
		System.out.println("-cgf: Common Grams Filter");
		System.out.println("-edgtf: Edge N-Gram Token Filter");
		System.out.println("\t MinGram integer");
		System.out.println("\t MaxGram integer");
		System.out.println("-classic: Classic similarity (vector space model)");
		System.out.println("-bm25: BM25 similarity");
		System.out.println("\t k1 float");
		System.out.println("\t b float");
		System.out.println("-lmd: LM Dirichlet smoothing");
		System.out.println("\t mu float");
		System.out.println("-lmjm: LM Jelineck-Mercer");
		System.out.println("\t lambda float");
	}

	public Query expandQuery(String line, IndexSearcher searcher, Analyzer analyzer, double weight, IndexReader reader)
			throws org.apache.lucene.queryparser.classic.ParseException {
		Query query;
		QueryParser parser = new QueryParser("Body", analyzer);
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		Map<String, Integer> expansionTerms = getExpansionTerms(searcher, line, TOP, analyzer, reader);

		Query q = parser.parse(line);
		queryBuilder.add(q, BooleanClause.Occur.SHOULD);

		int idx = 0;
		expansionTerms = sortByValue(expansionTerms);
		for (Map.Entry<String, Integer> entry : expansionTerms.entrySet()) {
			BoostQuery boost = new BoostQuery(new TermQuery(new Term(entry.getKey())), (float)weight);
			queryBuilder.add(boost, BooleanClause.Occur.SHOULD);
			line += " " + entry.getKey() + "^" + weight;
			if(idx == NUM_TOP_WORDS)
				break;
			idx++;
		}
		query = queryBuilder.build();
        query = parser.parse(line);


		return query;
	}

	public void writeCSV(List<DataSetTrec> t, String csvFile) throws IOException {
		FileWriter writer = new FileWriter(csvFile);

		CSVUtils.writeLine(writer, Arrays.asList("Filter", "map"));

		// custom separator + quote
		for (DataSetTrec d : t) {
			CSVUtils.writeLine(writer, Arrays.asList(d.getFilter(), String.valueOf(d.getMap())));
		}

		writer.flush();
		writer.close();
	}

	public List<DataSetTrec> getTrecs() {
		return trecs;
	}

	private Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

		// 1. Convert Map to List of Map
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());

		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Map.Entry<String, Integer> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public List<ResultDocs> parseScoreDocs(IndexSearcher searcher, ScoreDoc[] scores)
	{
		List<ResultDocs> resultsDocs = new LinkedList<ResultDocs>();

		for (int j = 0; j < scores.length; j++) {
			ScoreDoc c = scores[j];
			Document doc = null;
			try {
				doc = searcher.doc(c.doc);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Integer Id = doc.getField("Id").numericValue().intValue();
			resultsDocs.add(new ResultDocs("1", Id, c.score, doc));
		}

		Collections.sort(resultsDocs);

		return resultsDocs;
	}

	public Map<String, Integer> getExpansionTerms(IndexSearcher searcher, String queryString, int numExpDocs,
			Analyzer analyzer, IndexReader reader) {

		Map<String, Integer> topTerms = new HashMap<String, Integer>();

		try {
			QueryParser parser = new QueryParser("Body", analyzer);
			Query query;
			try {
				query = parser.parse(queryString);
			} catch (org.apache.lucene.queryparser.classic.ParseException e) {
				System.out.println("Error parsing query string.");
				return null;
			}

			TopDocs results = searcher.search(query, numExpDocs);
			ScoreDoc[] hits = results.scoreDocs;

			System.out.println(queryString);
			List<ResultDocs> resultDocs = parseScoreDocs(searcher, hits);

			TokenStream stream = analyzer.tokenStream("field", new StringReader(queryString));
			CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
			Map<String, Integer> queryTerms = new HashMap<String, Integer>();
			try {
				stream.reset();
				while (stream.incrementToken()) {
					String term = termAtt.toString();
					Integer termCount = topTerms.get(term);
					if (termCount == null)
						topTerms.put(term, 1);
					else
						topTerms.put(term, ++termCount);
					stream.end();
				}
				} finally {
					stream.close();
				}


			int numTotalHits = results.totalHits;
			System.out.println(numTotalHits + " total matching documents");


			for (int j = 0; j < resultDocs.size(); j++) {
				Document doc = resultDocs.get(j).getDoc();
				String answer = doc.get("Body");
				Integer AnswerId = doc.getField("Id").numericValue().intValue();

				stream = analyzer.tokenStream("field", new StringReader(answer));

				// get the CharTermAttribute from the TokenStream
				termAtt = stream.addAttribute(CharTermAttribute.class);

				try {
					stream.reset();
                    while (stream.incrementToken()) {
                        String term = termAtt.toString();
                        Integer termCount = topTerms.get(term);
                        if(j < TOPDOCS && queryTerms.get(term) == null) {
                            if (termCount == null)
                                topTerms.put(term, 1);
                            else
                                topTerms.put(term, ++termCount);
                        } else if (TOP - TOPDOCS > 0 && j > (TOP - TOPDOCS) && termCount != null) {
							/*int docFreq = reader.docFreq(new Term("Body", term));
							int numDocs = reader.numDocs();

							double idf = Math.log(numDocs / (docFreq + 1));*/
							topTerms.remove(term);
							//topTerms.put(term, (int)(termCount * idf));
                        }
                    }
					stream.end();
				} finally {
					stream.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return topTerms;
	}
}
