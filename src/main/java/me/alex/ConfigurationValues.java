package me.alex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Paths;

/**
 * This class is a thread-safe singleton class that reads the configuration JSON file or creates one if one does not exist.
 * Since only one instance can be created, it will appear often in many classes.
 */
public class ConfigurationValues {
    private static ConfigurationValues instance;
    public Long[] exemptionList;
    public Long[] rolesAllowedToUpdate;
    public Long[] ignoredChannels;
    public double topPercentage;
    public int weeksOfData;
    public long roleId;
    public long serverId;
    public long messageCooldown;
    public String botToken;
    private ConfigurationValues() {
    }

    /**
     * This method reads the configuration and assigns values accordingly. In the case of a configuration not existing, it uses default values and creates a configuration.
     * @return Returns <b>the instance</b> of ConfigurationValues.
     * @see Gson
     */
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
                instance.rolesAllowedToUpdate = new Long[] {718236096814645289L,772163720062173214L };
                instance.ignoredChannels = new Long[] {738006372725293086L};
                instance.topPercentage = 50;
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
            if (instance.topPercentage > 100 || instance.topPercentage< 0) {
                throw new InvalidConfigurationException("The value top percentage in conf.json cannot be less than 0 or larger than 100!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return instance;
    }
}