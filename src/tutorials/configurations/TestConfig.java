package tutorials.configurations;

import java.util.ArrayList;
import java.util.List;

public class TestConfig {
    private String fileName;
    private List<RunConfig> configs;

    public TestConfig(String fileName, List<RunConfig> configs) {
        this.fileName = fileName;
        this.configs = configs;
    }

    public TestConfig() {
        fileName = "";
        configs = new ArrayList<>();
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

    public void setConfigs(List<RunConfig> configs) {
        this.configs = configs;
    }
}
