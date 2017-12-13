package tutorials.rank;

import tutorials.configurations.Ranker;
import tutorials.utils.JSONProfile;
import tutorials.utils.ResultDocs;

import java.time.LocalDate;
import java.util.*;

public class RankFusion {
    public RankFusion() {
    }

    public List<ResultDocs> Fuse(MultiRanker multiRanker) {
        List<ResultDocs> result = new ArrayList<>();
        float k = multiRanker.getK();
        Map<LocalDate, Object> dayDigests = new HashMap<>();
        Map<JSONProfile, Object> profileDocs;
        Map<Long, ResultDocs> docScores;

        // Le todas as datas dos digests
        for (Ranker ranker : multiRanker.getRankers()) {
            List<DailyDigest> digests = ranker.getDigests();

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
                        float score = 1 / (k + rank);

                        if(docScores.containsKey(docId))
                        {
                            ResultDocs s = docScores.get(docId);
                            s.setScore(s.getScore() + score);
                        }
                        else
                        {
                            ResultDocs res = new ResultDocs(doc.getQueryId(), doc.getDocId(), score, doc.getDoc(), 1);
                            docScores.put(docId, res);
                        }
                    }
                }
            }
        }

        // Depois de ter os scores todos criados ordenasse pelos scores e limitasse o número de resultados a 10 por profile
        for (Map.Entry<LocalDate, Object> day : dayDigests.entrySet()) {
            profileDocs = (Map<JSONProfile, Object>) day.getValue();
            for (Map.Entry<JSONProfile, Object> profile : profileDocs.entrySet()) {
                docScores = (Map<Long, ResultDocs>) profile.getValue();
                List<Map.Entry<Long, ResultDocs>> list = new LinkedList<>(docScores.entrySet());
                list.sort((o1, o2) -> Float.compare(o2.getValue().getScore(), o1.getValue().getScore()));
                int i = 0;
                for (Map.Entry<Long, ResultDocs> res : list) {
                    if(i == 10)
                        break;
                    result.add(res.getValue());
                    i++;
                }
            }
        }

        return result;
    }
}
