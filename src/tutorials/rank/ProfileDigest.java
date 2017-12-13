package tutorials.rank;

import tutorials.utils.JSONProfile;
import tutorials.utils.ResultDocs;

import java.util.List;

public class ProfileDigest {
    private JSONProfile profile;
    private List<ResultDocs> resultDocs;

    public ProfileDigest(JSONProfile profile, List<ResultDocs> results) {
        this.profile = profile;
        this.resultDocs = results;
    }

    public List<ResultDocs> getResultDocs() {
        return resultDocs;
    }

    public JSONProfile getProfile() {
        return profile;
    }
}
