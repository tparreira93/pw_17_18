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

import tutorials.clustering.Jaccard;
import tutorials.clustering.KMeans;
import tutorials.configurations.Expand;
import tutorials.configurations.Ranker;
import tutorials.rank.DailyDigest;
import tutorials.rank.ProfileDigest;
import tutorials.utils.*;
import twitter4j.Status;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TwitterIndexer {
    private final static int QUERYEXPASION_SEARCH = 10;
    private final static int QUERYEXPASION_TOPDOCS = 5;
    private final static int SEARCH_RESULTS = 25;

    private Map<LocalDate, IndexWriter> indexes = new HashMap<>();
    private List<String> createdIndexes = new ArrayList<>();
    private LocalDate startDate;
    private LocalDate endDate;
    private DateTimeFormatter formatter;

    public TwitterIndexer(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = startDate;

        while (date.isBefore(endDate) || date.isEqual(endDate)) {
            indexes.put(date, null);
            date = date.plusDays(1);
        }
    }

    private String getPath(Ranker config, LocalDate date) {
        return config.getIndexPath() + "\\" + date.format(formatter);
    }

    public boolean openIndex(Ranker config) {
        if (createdIndexes.contains(config.getIndexPath())) {
            return false;
        }

        try {
            indexes.clear();
            LocalDate date = startDate;
            while (date.isBefore(endDate) || date.isEqual(endDate)) {
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
                createdIndexes.add(config.getIndexPath());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
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
            e.printStackTrace();
        }
    }

    private void addOrUpdate(IndexWriter idx, Document doc, long id) throws IOException {
        // ====================================================
        // Add the document to the index
        if (idx.getConfig().getOpenMode() == OpenMode.CREATE) {
            idx.addDocument(doc);

        } else {
            idx.updateDocument(new Term("Id", ((Long) id).toString()), doc);
        }
    }

    // ====================================================
    // ANNOTATE THIS METHOD YOURSELF
    public void indexSearch(Ranker ranker, List<JSONProfile> profiles) {
        Set<LocalDate> dates = indexes.keySet();

        for (LocalDate date : dates) {
            DailyDigest digest = new DailyDigest(date);

            for (JSONProfile profile : profiles) {
                ProfileDigest results = searchProfile(ranker, date, profile);
                digest.addProfileDigest(results);
            }
            ranker.addDailyDigest(digest);
        }
    }

    private ProfileDigest searchProfile(Ranker ranker, LocalDate date, JSONProfile profile) {
        Analyzer analyzer = ranker.getAnalyzer();
        Similarity similarity = ranker.getSimilarityConfiguration();
        IndexReader reader = null;
        IndexSearcher searcher;
        List<ResultDocs> resultsDocs = new LinkedList<>();
        try {
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(getPath(ranker, date))));
            searcher = new IndexSearcher(reader);

            searcher.setSimilarity(similarity);
            Query query;
            try {
                query = createQuery(profile.getTitle(), date, ranker.getExpand(), analyzer, searcher, reader);
                int resultsSize = SEARCH_RESULTS;
                if(ranker.getClustering().isCluster())
                    resultsSize = ranker.getClustering().getNumClusteringDocs();
                System.out.println("QUERY FINAL:" + query.toString());
                TopDocs results = searcher.search(query, resultsSize);

                List<ScoreDoc> documentResults = new ArrayList<>(Arrays.asList(results.scoreDocs));

                if(ranker.getJaccard().isJaccard())
                    documentResults = nearDuplicateDetection(documentResults,analyzer,searcher);
               
                if(ranker.getClustering().isCluster()){
                	
                	documentResults = KMeans.clusterData(analyzer,documentResults, searcher, ranker.getClustering());
                }
                    

                resultsDocs = parseScoreDocs(searcher, documentResults, profile, date);
            } catch (org.apache.lucene.queryparser.classic.ParseException e) {
                System.out.println("Error parsing query string.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        return new ProfileDigest(profile, resultsDocs);
    }

    private List<ScoreDoc> nearDuplicateDetection(List<ScoreDoc> scoreDocs, Analyzer analyzer, IndexSearcher searcher) throws IOException {
    	
        return Jaccard.process(analyzer,searcher,scoreDocs);
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

    private Query createQuery(String queryText, LocalDate date, Expand expand, Analyzer analyzer, IndexSearcher searcher, IndexReader reader) throws ParseException, IOException {
        Query query;
        QueryParser parser = new QueryParser("Body", analyzer);

        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.add(new TermQuery(new Term("CreationDate", formatter.format(date))), BooleanClause.Occur.MUST);
        if (expand.isExpand())
            queryBuilder.add(expandQuery(queryText, searcher, reader, analyzer, expand), BooleanClause.Occur.SHOULD);
        else {
            queryBuilder.add(parser.parse(queryText), BooleanClause.Occur.SHOULD);
        }
        query = queryBuilder.build();

        return query;
    }

    private List<String> getQueryTerms(String query, Analyzer analyzer) throws IOException {
        List<String> queryTerms = new ArrayList<>();
        String newQuery = query.replaceAll("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
        TokenStream stream = analyzer.tokenStream("field", new StringReader(newQuery));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        try {
            stream.reset();
            while (stream.incrementToken()) {
                queryTerms.add(termAtt.toString());
                stream.end();
            }
        } finally {
            stream.close();
        }

        return queryTerms;
    }

    private Query expandQuery(String line, IndexSearcher searcher, IndexReader reader, Analyzer analyzer, Expand expand)
            throws org.apache.lucene.queryparser.classic.ParseException, IOException {
        Query query;
        String field = "Body";
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        List<String> queryTerms = getQueryTerms(line, analyzer);
        List<ExpansionTerm> expansionTerms = getExpansionTerms(searcher, reader, line, expand.getNumExpansionDocs(), analyzer);

        for (String term : queryTerms) {
            BoostQuery boost = new BoostQuery(new TermQuery(new Term(field, term)), (float) expand.getWeight());
            queryBuilder.add(boost, BooleanClause.Occur.SHOULD);
        }

        int idx = 0;
        Collections.sort(expansionTerms);
        for (ExpansionTerm entry : expansionTerms) {
            if (queryTerms.contains(entry.getTerm())) {
                continue;
            }
            /*int docFreq = reader.docFreq(new Term(field, term));
            int numDocs = reader.numDocs();

            float idf = (float)Math.log(numDocs / (docFreq + 1));*/
            BoostQuery boost = new BoostQuery(new TermQuery(new Term(field, entry.getTerm())), (1.0f - (float) expand.getWeight()));
            queryBuilder.add(boost, BooleanClause.Occur.SHOULD);

            if (idx == expand.getNumTerms() - 1)
                break;
            idx++;
        }
        query = queryBuilder.build();
        //query = parser.parse(line);

        
        return query;
    }


    private List<ResultDocs> parseScoreDocs(IndexSearcher searcher, List<ScoreDoc> scores) {
        return parseScoreDocs(searcher, scores, null, null);
    }

    private List<ResultDocs> parseScoreDocs(IndexSearcher searcher, List<ScoreDoc> scores, JSONProfile profile, LocalDate date) {
        List<ResultDocs> resultsDocs = new LinkedList<>();
        int i = 1;
        for (ScoreDoc c : scores) {
            try {
                Document doc = searcher.doc(c.doc);
                Long Id = doc.getField("Id").numericValue().longValue();
                resultsDocs.add(new ResultDocs(profile != null ? profile.getTopicID() : "", Id, c.score, doc, i++, date));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(resultsDocs);

        return resultsDocs;
    }

    private List<ExpansionTerm> getExpansionTerms(IndexSearcher searcher, IndexReader reader, String queryString, int numExpDocs,
                                                   Analyzer analyzer) throws IOException {
        List<ExpansionTerm> expansionTerms = new ArrayList<>();
        Map<String, ExpansionTerm> topTerms = new HashMap<>();

        QueryParser parser = new QueryParser("Body", analyzer);
        Query query;
        try {
            query = parser.parse(queryString);
            //query = createQuery(queryString, date, new Expand(), analyzer, searcher, reader);
        } catch (org.apache.lucene.queryparser.classic.ParseException e) {
            System.out.println("Error parsing query string.");
            return null;
        }


        TopDocs results = searcher.search(query, numExpDocs);
        ScoreDoc[] hits = results.scoreDocs;

        List<ResultDocs> resultDocs = parseScoreDocs(searcher, Arrays.asList(hits));

        for (int j = 0; j < resultDocs.size(); j++) {
            Document doc = resultDocs.get(j).getDoc();
            String answer = doc.get("Body");

            List<String> terms = getQueryTerms(answer, analyzer);
            List<String> docFreq = new ArrayList<>();
            for (String term : terms) {
                ExpansionTerm expTerm = topTerms.get(term);
                if (j < numExpDocs) {
                    if (expTerm == null) {
                        expTerm = new ExpansionTerm();
                        expTerm.setTerm(term);
                        expTerm.setDocFreq(1);
                        expTerm.setTermFreq(1);
                        docFreq.add(term);
                        expansionTerms.add(expTerm);
                        topTerms.put(term, expTerm);
                    } else {
                        if (!docFreq.contains(term)) {
                            docFreq.add(term);
                            expTerm.setDocFreq(expTerm.getDocFreq() + 1);
                        }
                        expTerm.setTermFreq(expTerm.getTermFreq() + 1);
                    }
                }
            }
        }

        for (ExpansionTerm entry : expansionTerms) {
            //int docFreq = reader.docFreq(new Term("Body", entry.getKey()));
            //int numDocs = reader.numDocs();
            int docFreq = entry.getDocFreq();
            int numDocs = topTerms.size();

            Float idf = (float)Math.log((float)numDocs / (docFreq + 1));
            //topTerms.put(term, --termCount);
            //entry.setScore(entry.getTermFreq() * idf);
            entry.setScore(entry.getTermFreq());
        }
        return expansionTerms;
    }
}
