package tutorials.rank;

import tutorials.configurations.Ranker;
import tutorials.utils.JSONProfile;
import tutorials.utils.ResultDocs;

import java.time.LocalDate;
import java.util.*;

public class RankFusion {
    List<DailyDigest> digests = new ArrayList<>();

    public RankFusion() {
    }

    public void addDigests(List<DailyDigest> digest) {
        digests.addAll(digest);
    }

    public void addDigest(DailyDigest digest) {
        digests.add(digest);
    }


    public List<ResultDocs> Fuse(float k, int top) {
        List<ResultDocs> result = new ArrayList<>();
        Map<LocalDate, Object> dayDigests = new HashMap<>();
        Map<JSONProfile, Object> profileDocs;
        Map<Long, ResultDocs> docScores;

        // Le todas as datas dos digests
        for (DailyDigest digest : digests) {
            LocalDate date = digest.getDate();
            List<ProfileDigest> profileDigests = digest.getDigests();

            if (!dayDigests.containsKey(date)) {
                profileDocs = new HashMap<>();
                dayDigests.put(date, profileDocs);
            }
            else
                profileDocs = (Map<JSONProfile, Object>) dayDigests.get(date);

            // Percorre todos os profiles do dia
            for (ProfileDigest profileDigest : profileDigests) {
                JSONProfile profile = profileDigest.getProfile();
                List<ResultDocs> resultDocs = profileDigest.getResultDocs();

                docScores = (Map<Long, ResultDocs>) profileDocs.get(profile);

                if (docScores == null) {
                    docScores = new HashMap<>();
                    profileDocs.put(profile, docScores);
                }

                // Cria os scores do reciprocal rank fusion
                for (ResultDocs doc : resultDocs) {
                    long docId = doc.getDocId();
                    int rank = doc.getRank();
                    float score = Math.round((1 / (k + rank)) * 1000000f) /1000000f;

                    if(docScores.containsKey(docId))
                    {
                        ResultDocs s = docScores.get(docId);
                        s.setScore(s.getScore() + score);
                    }
                    else
                    {
                        ResultDocs res = new ResultDocs(doc.getQueryId(), doc.getDocId(), score, doc.getDoc(), 1, date);
                        docScores.put(docId, res);
                    }
                }
            }
        }

        // Depois de ter os scores todos criados ordenasse pelos scores e limitasse o número de resultados a 10 por profile
        for (Map.Entry<LocalDate, Object> day : dayDigests.entrySet()) {
            profileDocs = sortByKey((Map<JSONProfile, Object>) day.getValue());
            for (Map.Entry<JSONProfile, Object> profile : profileDocs.entrySet()) {
                docScores = (Map<Long, ResultDocs>) profile.getValue();
                List<Map.Entry<Long, ResultDocs>> list = new LinkedList<>(docScores.entrySet());
                list.sort((o1, o2) -> Float.compare(o2.getValue().getScore(), o1.getValue().getScore()));
                int i = 1;
                for (Map.Entry<Long, ResultDocs> res : list) {
                    if(i == top)
                        break;

                    ResultDocs tmp = res.getValue();
                    tmp.setRank(i++);

                    result.add(tmp);
                }
            }
        }

        return result;
    }


    private Map<JSONProfile, Object> sortByKey(Map<JSONProfile, Object> unsortMap) {

        List<Map.Entry<JSONProfile, Object>> list = new LinkedList<>(unsortMap.entrySet());

        Collections.sort(list, (o1, o2) -> Integer.compare((o2.getKey().getOrder()), o1.getKey().getOrder()));


        Map<JSONProfile, Object> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<JSONProfile, Object> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
