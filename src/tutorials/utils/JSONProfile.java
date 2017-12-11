package tutorials.utils;

public class JSONProfile {
    private String topicID;

    public String getTopicID() {
        return topicID;
    }

    public void setTopicID(String topicID) {
        this.topicID = topicID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    private String title;
    private String description;
    private String narrative;

    public JSONProfile(String topicID, String title, String description, String narrative) {
        this.topicID = topicID;
        this.title = title;
        this.description = description;
        this.narrative = narrative;
    }
}
