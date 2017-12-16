package tutorials.rank;

import tutorials.configurations.Ranker;
import tutorials.utils.CommandLine;
import tutorials.utils.DataSetTrec;
import tutorials.utils.ResultDocs;

import java.io.*;
import java.util.*;

public class MultiRanker {
    private List<Ranker> rankers;
    private float k;
    private boolean fusion;

    public MultiRanker() {
        rankers = new ArrayList<>();
        k = 1;
    }

    public float getK(){
        return k;
    }

    public void setK(float k){
        this.k = k;
    }

    public List<Ranker> getRankers() {
        return rankers;
    }

    public void addRanker(Ranker config) {
        rankers.add(config);
    }

    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        String name = "";
        for (Ranker ranker : rankers) {
            if(name.length() != 0)
                name += "+";
            name += ranker.toString();
        }
        if (rankers.size() > 1) {
            name = "[" + name + "]";
            if(isFusion())
                name += "(K=" + k + ")";
        }

        return name;
    }

    public DataSetTrec createTrec(String resultsFiles, String expectedResults, List<ResultDocs> resultsDocs, int runID) {
        BufferedWriter out = null;
        String result = "";

        String q = ResultDocs.CONST_QO;
        for (ResultDocs r : resultsDocs) {
            result += r.getQueryId() + " " + q + " " + r.getDocId() + " " + r.getRank() + " " + r.getScore() + " " + runID + "\n";
        }

        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFiles)));
            out.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        CommandLine c = new CommandLine();
        String resultTrec = c.executeCommand("trec_eval.exe " + expectedResults + " " + resultsFiles);

        BufferedReader bufReader = new BufferedReader(new StringReader(resultTrec));

        String l;
        DataSetTrec trec = new DataSetTrec(this.getName(), resultTrec);
        try {
            while ((l = bufReader.readLine()) != null) {

                String[] values = l.split("\\s+");
                String k = values[0];
                if (k.equals("map")) {
                    String[] a = l.split("\\s+");
                    trec.setMap(Float.parseFloat(a[a.length - 1]));
                }

                if (k.contains("iprec")) {
                    trec.addRecall(values[0].split("_")[3], Float.parseFloat(values[values.length - 1]));
                }

                if (k.contains("P_")) {
                    trec.addPrecision(values[0].split("_")[1], Float.parseFloat(values[values.length - 1]));
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return trec;
    }

    public boolean isFusion() {
        return fusion;
    }

    public void setFusion(boolean fusion) {
        this.fusion = fusion;
    }

    public List<ResultDocs> getResults(int top) {
        List<ResultDocs> result = new ArrayList<>();

        for (Ranker ranker : getRankers()) {
            List<DailyDigest> digests = ranker.getDigests();

            for (DailyDigest digest : digests) {
                List<ProfileDigest> profileDigests = digest.getDigests();

                for (ProfileDigest profileDigest : profileDigests) {
                    List<ResultDocs> resultDocs = profileDigest.getResultDocs();
                    Collections.sort(resultDocs);

                    int count = resultDocs.size();
                    if(count >= top)
                        count = top;

                    result.addAll(resultDocs.subList(0, count));
                }
            }
        }
        return result;
    }
}
