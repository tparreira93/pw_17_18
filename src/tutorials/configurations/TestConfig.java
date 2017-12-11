package tutorials.configurations;

import java.util.ArrayList;
import java.util.List;

public class TestConfig {
    private String fileName;
    private List<RunConfig> configs;
    private String query;

    public TestConfig() {
        fileName = "";
        configs = new ArrayList<>();
        query = "";
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<RunConfig> getConfigs() {
        return configs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
