package tutorials.rank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DailyDigest {
    private LocalDate date;
    private List<ProfileDigest> digests;

    public DailyDigest(LocalDate date) {
        this.date = date;
        digests = new ArrayList<>();
    }

    public List<ProfileDigest> getDigests() {
        return digests;
    }

    public void addProfileDigest(ProfileDigest p) {
        digests.add(p);
    }

    public LocalDate getDate() {
        return date;
    }
}
