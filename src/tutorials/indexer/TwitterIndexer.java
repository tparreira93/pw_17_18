package tutorials.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import tutorials.configurations.Expand;
import tutorials.configurations.Ranker;
import tutorials.utils.*;
import twitter4j.Status;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TwitterIndexer {
	public final static int TOP = 100;
	private final static int TOPDOCS = 25;
	public final static int NUMDOCS = 10;
	private final static int NUM_TOP_WORDS = 5;

	private Map<LocalDate, IndexWriter> indexes = new HashMap<>();
	private LocalDate startDate;
	private LocalDate endDate;
	private DateTimeFormatter formatter;
	public TwitterIndexer(LocalDate startDate, LocalDate endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	}

	private String getPath(Ranker config, LocalDate date) {
		return config.getIndexPath() + "\\" + date.format(formatter);
	}

	public void openIndex(Ranker config) {
		try {

			indexes.clear();
			LocalDate date = startDate;
			while (date.isBefore(endDate) || date.isEqual(endDate))
			{
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
				IndexWriterConfig iwc = new IndexWriterConfig(config.getAnalyzer());
				if (config.createIndex()) {
					// Create a new index, removing any
					// previously indexed documents:
					iwc.setOpenMode(OpenMode.CREATE);
				} else {
					// Add new documents to an existing index:
					iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
				}

				Directory dir = FSDirectory.open(Paths.get(getPath(config, date)));
				IndexWriter idx = new IndexWriter(dir, iwc);
				indexes.put(date, idx);
				date = date.plusDays(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void indexTweets(List<Status> tweets) {
		for (Status tweet : tweets) {
			indexDoc(tweet);
		}
	}

	private void indexDoc(Status status) {

		Document doc = new Document();

		try {
			doc.add(new LongPoint("Id", status.getId()));
			doc.add(new StoredField("Id", status.getId()));
			doc.add(new LongPoint("UserId", status.getUser().getId()));
			doc.add(new StringField("CreationDate", formatter.format(status.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()), Field.Store.YES));
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
	public void indexSearch(Ranker config, List<JSONProfile> profiles) {
		Set<LocalDate> dates = indexes.keySet();
		for (LocalDate date : dates) {
			search(config, date, profiles);
		}
	}

	private void search(Ranker config, LocalDate date, List<JSONProfile> profiles) {
		Analyzer analyzer = config.getAnalyzer();
		Similarity similarity = config.getSimilarityConfiguration();
		IndexReader reader = null;
		IndexSearcher searcher;
		try {
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(getPath(config, date))));
			searcher = new IndexSearcher(reader);
			List<ResultDocs> resultsDocs = new LinkedList<>();

			searcher.setSimilarity(similarity);

			for (JSONProfile profile : profiles) {
				Query query;
				try {
					query = createQuery(profile.getTitle(), date, config.getExpand(), analyzer, searcher);
					TopDocs results = searcher.search(query, NUMDOCS);

					List<ScoreDoc> documentResults = nearDuplicateDetection(new ArrayList<>(Arrays.asList(results.scoreDocs)));

					documentResults = reorderTweets(documentResults);

					for (ScoreDoc sc : documentResults) {
						Document doc = searcher.doc(sc.doc);
						Integer Id = doc.getField("Id").numericValue().intValue();
						resultsDocs.add(new ResultDocs(profile.getTopicID(), Id, sc.score, doc));
					}

					config.setResults(resultsDocs);
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					System.out.println("Error parsing query string.");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private List<ScoreDoc> nearDuplicateDetection(List<ScoreDoc> scoreDocs) {
		return scoreDocs;
	}

	private List<ScoreDoc> reorderTweets(List<ScoreDoc> scoreDocs) {
		return scoreDocs;
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


	private Query createQuery(String queryText, LocalDate date, Expand expand, Analyzer analyzer, IndexSearcher searcher) throws ParseException {
		Query query;
		QueryParser parser;

		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		queryBuilder.add(new TermQuery(new Term("CreationDate", formatter.format(date))), BooleanClause.Occur.MUST);
		if (expand.isExpand())
			queryBuilder.add(expandQuery(queryText, searcher, analyzer, expand.getWeight()), BooleanClause.Occur.SHOULD);
		else {
			parser = new QueryParser("Body", analyzer);
			queryBuilder.add(parser.parse(queryText), BooleanClause.Occur.SHOULD);
		}

		query = queryBuilder.build();

		return query;
	}

	private Query expandQuery(String line, IndexSearcher searcher, Analyzer analyzer, double weight)
			throws org.apache.lucene.queryparser.classic.ParseException {
		Query query;
		QueryParser parser = new QueryParser("Body", analyzer);
		BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
		Map<String, Integer> expansionTerms = getExpansionTerms(searcher, line, TOP, analyzer);

		Query q = parser.parse(line);
		queryBuilder.add(q, BooleanClause.Occur.SHOULD);

		int idx = 0;
		expansionTerms = sortByValue(expansionTerms);
		StringBuilder lineBuilder = new StringBuilder(line);
		for (Map.Entry<String, Integer> entry : expansionTerms.entrySet()) {
			BoostQuery boost = new BoostQuery(new TermQuery(new Term(entry.getKey())), (float)weight);
			queryBuilder.add(boost, BooleanClause.Occur.SHOULD);
			lineBuilder.append(" ").append(entry.getKey()).append("^").append(weight);
			if(idx == NUM_TOP_WORDS)
				break;
			idx++;
		}
		line = lineBuilder.toString();
		//query = queryBuilder.build();
        query = parser.parse(line);


		return query;
	}

	private Map<String, Integer> sortByValue(Map<String, Integer> unsortMap) {

		// 1. Convert Map to List of Map
		List<Map.Entry<String, Integer>> list = new LinkedList<>(unsortMap.entrySet());

		list.sort((o1, o2) -> (o2.getValue()).compareTo(o1.getValue()));

		Map<String, Integer> sortedMap = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	private List<ResultDocs> parseScoreDocs(IndexSearcher searcher, ScoreDoc[] scores)
	{
		List<ResultDocs> resultsDocs = new LinkedList<>();

		for (ScoreDoc c : scores) {
			try {
				Document doc = searcher.doc(c.doc);
				Integer Id = doc.getField("Id").numericValue().intValue();
				resultsDocs.add(new ResultDocs("1", Id, c.score, doc));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Collections.sort(resultsDocs);

		return resultsDocs;
	}

	private Map<String, Integer> getExpansionTerms(IndexSearcher searcher, String queryString, int numExpDocs,
												   Analyzer analyzer) {

		Map<String, Integer> topTerms = new HashMap<>();

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
			Map<String, Integer> queryTerms = new HashMap<>();
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
