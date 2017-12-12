package tutorials.rank;

import tutorials.configurations.Ranker;
import tutorials.utils.CommandLine;
import tutorials.utils.DataSetTrec;
import tutorials.utils.ResultDocs;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiRanker {
    private List<Ranker> rankers;

    public MultiRanker() {
        rankers = new ArrayList<>();
    }

    public List<Ranker> getRankers() {
        return rankers;
    }

    public void addRanker(Ranker config) {
        rankers.add(config);
    }

    @Override
    public String toString() {
        StringBuilder name = new StringBuilder();
        for (Ranker ranker : rankers) {
            if(name.length() > 0)
                name.append(";");
            name.append(ranker.getName());
            if (ranker.getExpand().isExpand()) {
                name.append(" (alfa=").append(Math.round(ranker.getExpand().getWeight() * 100f) / 100f).append(")");
            }
        }

        return name.toString();
    }

    public DataSetTrec createTrec(String resultsFiles, List<ResultDocs> resultsDocs) {
        String tmp = this.toString();
        DataSetTrec t = new DataSetTrec(tmp);

        BufferedWriter out = null;
        StringBuilder result = new StringBuilder();
        Collections.sort(resultsDocs);

        String q = ResultDocs.CONST_QO;
        String run = ResultDocs.CONST_RUN;
        for (ResultDocs r : resultsDocs) {
            result.append(r.getQueryId()).append(" ").append(q).append(" ").append(r.getDocId()).append(" ").append(r.getRank()).append(" ").append(r.getScore()).append(" ").append(run).append("\n");
        }

        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultsFiles)));
            out.write(result.toString());
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
        String resultTrec = c.executeCommand("trec_eval.exe qrels.offline.txt " + resultsFiles);

        BufferedReader bufReader = new BufferedReader(new StringReader(resultTrec));

        String l;
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

        return t;
    }

}
