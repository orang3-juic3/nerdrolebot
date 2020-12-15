package me.alex;

import java.io.*;
import java.nio.file.Paths;

public class ConfigurationValues {
    private static ConfigurationValues instance;
    public long[] exemptionList;
    public int weeksOfData;
    public long roleId;
    public long serverId;
    private ConfigurationValues() {
        try {
            String workingDir = Paths.get("").toAbsolutePath().toString();
            File f = new File(workingDir + "\\conf.json");
            BufferedWriter bufferedWriter = new BufferedWriter(new PrintWriter(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}