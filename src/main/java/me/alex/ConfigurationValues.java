package me.alex;

import com.google.gson.Gson;

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
            if (f.createNewFile()) {
                BufferedWriter bufferedWriter = new BufferedWriter(new PrintWriter(f));
                exemptionList = new long[]{266279893543682049L, 230336110637744131L};
                weeksOfData = 2;
                roleId = 706554375572684860L;
                serverId = 679434326282207238L;
                Gson gson = new Gson();
                bufferedWriter.write(gson.toJson(this));
                throw new InvalidConfigurationException("Created new configuration file, please edit before restarting the program!");
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            String line;
            StringBuilder config = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                config.append(line);
            }
            Gson gson = new Gson();
            gson.fromJson(config.toString(), this.getClass());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized ConfigurationValues getInstance(){
        if(instance == null){
            instance = new ConfigurationValues();
        }
        return instance;
    }
}