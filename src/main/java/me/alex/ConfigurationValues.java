package me.alex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Paths;

public class ConfigurationValues {
    private static ConfigurationValues instance;
    public Long[] exemptionList;
    public int weeksOfData;
    public long roleId;
    public long serverId;
    public long messageCooldown;
    public String botToken;
    private ConfigurationValues() {
    }

    public static synchronized ConfigurationValues getInstance() {
        if(instance == null){
            instance = new ConfigurationValues();
        }
        try {
            String workingDir = Paths.get("").toAbsolutePath().toString();
            File f = new File(workingDir + "/conf.json");
            if (f.createNewFile()) {
                BufferedWriter bufferedWriter = new BufferedWriter(new PrintWriter(f));
                // bufferedWriter.write("{\"exemptionList\":[479285497487949853, 230336110637744131],\"weeksOfData\":2,\"roleId\":706554375572684860,\"serverId\":679434326282207238}");
                instance.exemptionList = new Long[]{266279893543682049L, 230336110637744131L};
                instance.weeksOfData = 2;
                instance.roleId = 706554375572684860L;
                instance.serverId = 679434326282207238L;
                instance.botToken = "NzExOTk1MjExMzc0Mzk1NDEy.XsLHNg.IZgzg4W7RwkExCYNXa7vmc_L5us";
                instance.messageCooldown = 6000L;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                bufferedWriter.write(gson.toJson(instance));
                bufferedWriter.close();
                throw new InvalidConfigurationException("Created new configuration file, please edit before restarting the program!");
            }
            BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
            String line;
            StringBuilder config = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                config.append(line);
            }
            Gson gson = new Gson();
            instance = gson.fromJson(config.toString(), instance.getClass());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return instance;
    }
}