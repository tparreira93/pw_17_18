package tutorials;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.jfree.ui.RefineryUtilities;
import tutorials.clustering.Jaccard;
import tutorials.configurations.*;
import tutorials.indexer.TwitterIndexer;
import tutorials.rank.MultiRanker;
import tutorials.rank.RankFusion;
import tutorials.utils.*;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {

	public static void main(String[] args) {
		TestConfig testConfig = parseConfig(args);
		if (testConfig == null || !testConfig.verboseValidate()) {
			printUsage();
			return;
		}

		try {
			LocalDate startDate = LocalDate.parse("2016-08-02");
			LocalDate endDate = LocalDate.parse("2016-08-11");
			List<MultiRanker> multiRankers = testConfig.getMultiRankers();
			List<DataSetTrec> trecs = new LinkedList<>();
			TwitterIndexer indexer = new TwitterIndexer(startDate, endDate);

			try {
				if (testConfig.isLoadTweets()) {
					System.out.println("Loading tweets...");
					testConfig.loadTweets();
					System.out.println(testConfig.getTweets().size() + " tweets loaded!");
				}
				System.out.println("Loading interest profiles...");
				testConfig.loadProfiles();
				System.out.println(
						(testConfig.getTestData().size() + testConfig.getTrainData().size()) + " profiles loaded!");
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			System.out.println("");

			int run = 0;
			for (MultiRanker multiRanker : multiRankers) {
				for (Ranker ranker : multiRanker.getRankers()) {
					if (ranker.createIndex()) {
						if (indexer.openIndex(ranker)) {
							System.out.println("Indexing " + ranker.getIndexName() + "...");
							indexer.indexTweets(testConfig.getTweets());
							indexer.close();
						}
					}
					if (testConfig.isTrain()) {
						System.out.println("Creating train digests for " + ranker.toString() + "...");
						System.out.println("");
						indexer.indexSearch(ranker, testConfig.getTrainData());
					} else if (testConfig.isTest()) {
						System.out.println("Creating test digests for " + ranker.toString() + "...");
						System.out.println("");
						indexer.indexSearch(ranker, testConfig.getTestData());
					}
				}
				List<ResultDocs> resultDocs;
				if (multiRanker.isFusion()) {
					System.out.println("");
					System.out.println("Fusing scores of " + multiRanker.toString() + "...");

					RankFusion fusion = new RankFusion();
					resultDocs = fusion.Fuse(multiRanker, 10);
				} else {
					resultDocs = multiRanker.getResults(10);
				}

				DataSetTrec t = multiRanker.createTrec("tmp.txt", testConfig.getEvaluation(), resultDocs, run);
				trecs.add(t);

				multiRanker.createResults("results" + run + ".txt", testConfig.getEvaluation(), resultDocs, run);

				multiRanker.createDigests("digests" + run + ".txt", testConfig.getEvaluation(), resultDocs, run);

				run++;
				System.out.println("");
				System.out.println("");
				System.out.println(t.getName());
				System.out.println(t.getRawText());
			}

			plotTrecs(trecs);
			writeCSV(trecs, "ranking_results.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void writeCSV(List<DataSetTrec> trecList, String csvFile) throws IOException {
		FileWriter writer = new FileWriter(csvFile);

		CSVUtils.writeLine(writer, Arrays.asList("Filter", "map", "P@5", "P@10"));

		// custom separator + quote
		for (DataSetTrec trec : trecList) {
			Float p5 = trec.getPrecisions().get("5");
			Float p10 = trec.getPrecisions().get("10");
			String tmp5 = "", tmp10 = "";
			if (p5 != null)
				tmp5 = p5.toString();
			if (p10 != null)
				tmp10 = p10.toString();
			CSVUtils.writeLine(writer,
					Arrays.asList("\"" + trec.getName() + "\"", String.valueOf(trec.getMap()), tmp5, tmp10));
		}

		writer.flush();
		writer.close();
	}

	private static void plotTrecs(List<DataSetTrec> trecs) {
		plot("Precision x Documents", "Precision x Documents", String.valueOf(10), trecs, 'p');

		plot("Precision x Recall", "Precision x Recall", String.valueOf(10), trecs, 'r');
	}

	private static void plot(String applicationTitle, String chartTitle, String sub, List<DataSetTrec> trecs,
			char type) {
		LineChart lineChart = new LineChart(applicationTitle, chartTitle, sub, trecs, type);
		lineChart.pack();
		RefineryUtilities.centerFrameOnScreen(lineChart);
		lineChart.setVisible(true);
	}

	private static void printUsage() {
		System.out.println("Web Search - Project\n");
		System.out.println("Usage example:");
		System.out.println("[(-h|--help)] -f name -q name -e evaluation (-fusion | -nofusion) "
				+ "-r [index (filter string | nothing(defaults to english analyzer))] .."
				+ " -r [index (filter string)] (-fusion | -nofusion) -r indexName (-index) [filters similarity expansion kmeans] ....");
		System.out.println("-h or --help: Help.");
		System.out.println("-f: Twitter JSON");
		System.out.println("\t filename");
		System.out.println("-q: Queries JSON");
		System.out.println("\t filename");
		System.out.println("-e: Evaluation");
		System.out.println(
				"-split: Split profiles in even and odd profile lists. It is need to specify if -train and\\or -test");
		System.out.println("-test: Use test profile");
		System.out.println("-train: Use train profile");
		System.out.println("\t filename");
		System.out.println("-fusion: Rank fusion following ranks");
		System.out.println("-nofusion: No fusion for following ranks");
		System.out.println("Rank: -r indexName [index (filter string)]:");
		System.out.println("-index: Create index");
		System.out.println("Filters:");
		System.out.println("Default filter\\analyzer is EnglishAnalyzer");
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
		System.out.println("Similarities:");
		System.out.println("-classic: Classic similarity (vector space model)");
		System.out.println("-bm25: BM25 similarity");
		System.out.println("\t k1 float");
		System.out.println("\t b float");
		System.out.println("-lmd: LM Dirichlet smoothing");
		System.out.println("\t mu float");
		System.out.println("-lmjm: LM Jelineck-Mercer");
		System.out.println("\t lambda float");
		System.out.println("-expand: Pseudo-relevant feedback expansion");
		System.out.println("\t numTerms int");
		System.out.println("\t numRelevantDOcs int");
		System.out.println("\t alfa float");
		System.out.println("-kmeans: KMeans clustering");
		System.out.println("\t clusters int");
		System.out.println("\t numClusteringDocs int");
		System.out.println("-jaccard: Jaccard duplicate detection");
		//System.out.println("\t threshold float");
	}

	private static TestConfig parseConfig(String[] args) {
		TestConfig testConfig = new TestConfig();
		Ranker ranker = null;
		MultiRanker multiRanker = new MultiRanker();
		Boolean fuse = false;
		try {
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
				case "-f":
					if (args.length - i >= 1) {
						testConfig.setFileName(args[++i]);
					}
					break;
				case "-q":
					if (args.length - i >= 1) {
						testConfig.setQuery(args[++i]);
					}
					break;
				case "-e":
					if (args.length - i >= 1) {
						testConfig.setEvaluation(args[++i]);
					}
					break;
				case "-split":
					testConfig.setSplitData(true);
					break;
				case "-test":
					testConfig.setTest(true);
					break;
				case "-train":
					testConfig.setTrain(true);
					break;
				case "-fusion":
					fuse = true;
					multiRanker = new MultiRanker();
					if (args.length - i >= 1) {
						multiRanker.setFusion(true);
						multiRanker.setK(Float.parseFloat(args[++i]));
					}
					testConfig.addRanker(multiRanker);
					break;
				case "-kmeans":
					ClusteringOptions cluster = new ClusteringOptions();
					
					if (ranker != null) {
						if (args.length - i >= 2) {
								
							cluster.setCluster(true);
							cluster.setNumClusters(Integer.parseInt(args[++i]));
							cluster.setNumClusteringDocs(Integer.parseInt(args[++i]));
							
						}
						ranker.setClustering(cluster);
					}
					break;
				case "-nofusion":
					fuse = false;
					break;
				case "-r":
					if (args.length - i >= 1) {
						ranker = new Ranker(args[++i]);
						if (fuse) {
							multiRanker.addRanker(ranker);
						} else {
							multiRanker = new MultiRanker();
							multiRanker.addRanker(ranker);
							testConfig.addRanker(multiRanker);
						}
					}
					break;
				case "-index":
					if (ranker != null) {
						ranker.setIndex(true);
					}
					break;
				case "-sf":
					if (ranker != null) {
						ranker.getAnalyzerConfiguration().getFilters()
								.add(AnalyzerConfiguration.AnalyzerFilters.StopFilter);
					}
					break;
				case "-shf":
					if (args.length - i - 1 >= 2) {
						if (ranker != null) {
							ranker.getAnalyzerConfiguration().setMinShingleSize(Integer.parseInt(args[++i]));
							ranker.getAnalyzerConfiguration().setMaxShingleSize(Integer.parseInt(args[++i]));
							ranker.getAnalyzerConfiguration().getFilters()
									.add(AnalyzerConfiguration.AnalyzerFilters.ShingleFilter);
						}
					}
					break;
				case "-snf":
					if (ranker != null) {
						ranker.getAnalyzerConfiguration().getFilters()
								.add(AnalyzerConfiguration.AnalyzerFilters.SnowballFilter);
					}
					break;
				case "-stdf":
					if (ranker != null) {
						ranker.getAnalyzerConfiguration().getFilters()
								.add(AnalyzerConfiguration.AnalyzerFilters.StandardFilter);
					}
					break;
				case "-lcf":
					if (ranker != null) {
						ranker.getAnalyzerConfiguration().getFilters()
								.add(AnalyzerConfiguration.AnalyzerFilters.LowerCaseFilter);
					}
					break;
				case "-ngtf":
					if (args.length - i >= 2) {
						if (ranker != null) {
							ranker.getAnalyzerConfiguration().setMinGram(Integer.parseInt(args[++i]));
							ranker.getAnalyzerConfiguration().setMaxGram(Integer.parseInt(args[++i]));
							ranker.getAnalyzerConfiguration().getFilters()
									.add(AnalyzerConfiguration.AnalyzerFilters.NGramTokenFilter);
						}
					}
					break;
				case "-cgf":
					if (ranker != null) {
						ranker.getAnalyzerConfiguration().getFilters()
								.add(AnalyzerConfiguration.AnalyzerFilters.CommonGramsFilter);
					}
					break;
				case "-wt":
					if (ranker != null) {
						ranker.getAnalyzerConfiguration().getFilters()
								.add(AnalyzerConfiguration.AnalyzerFilters.WhitespaceTokenizer);
					}
					break;
				case "-edgtf":
					if (args.length - i >= 2) {
						if (ranker != null) {
							ranker.getAnalyzerConfiguration().setMinGram(Integer.parseInt(args[++i]));
							ranker.getAnalyzerConfiguration().setMaxGram(Integer.parseInt(args[++i]));
							ranker.getAnalyzerConfiguration().getFilters()
									.add(AnalyzerConfiguration.AnalyzerFilters.EdgeNGramTokenFilter);
						}
					}
					break;
				case "-classic":
					if (ranker != null) {
						ranker.setSimilarityConfigurations(new ClassicSimilarity());
					}
					break;
				case "-bm25":
					float k1 = 1.2f;
					float b = 0.75f;
					if (args.length - i >= 2) {
						k1 = Float.parseFloat(args[++i]);
						b = Float.parseFloat(args[++i]);
					}
					if (ranker != null) {
						ranker.setSimilarityConfigurations(new BM25Similarity(k1, b));
					}
					break;
				case "-lmd":
					float mu = 2000f;
					if (args.length - i >= 1) {
						mu = Float.parseFloat(args[++i]);
					}
					if (ranker != null) {
						ranker.setSimilarityConfigurations(new LMDirichletSimilarity(mu));
					}
					break;
				case "-lmjm":
					float lambda = 0.5f;
					if (args.length - i >= 1) {
						lambda = Float.parseFloat(args[++i]);
					}
					if (ranker != null) {
						ranker.setSimilarityConfigurations(new LMJelinekMercerSimilarity(lambda));
					}
					break;
				case "-expand":
					Expand exp = new Expand(0.5, true);
					if (args.length - 1 >= 3) {
						exp.setNumTerms(Integer.parseInt(args[++i]));
						exp.setNumExpansionDocs(Integer.parseInt(args[++i]));
						exp.setWeight(Float.parseFloat(args[++i]));
						if (ranker != null) {
							ranker.setExpand(exp);
						}
					}
					break;
				case "-jaccard":
					JaccardConfiguration jacc = new JaccardConfiguration();
					jacc.setJaccard(true);
					if (ranker != null) {
						ranker.setJaccard(jacc);
					}
					if (args.length - 1 >= 1) {
						jacc.setSimilarity(Float.parseFloat(args[++i]));
						if (ranker != null) {
							ranker.setJaccard(jacc);
						}
					}
					break;
				case "-h":
				case "--help":
					return null;
				default:
					return null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return testConfig;
	}
}
