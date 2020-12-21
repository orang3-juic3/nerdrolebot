package me.alex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

/**
 * This class is a thread-safe singleton class that reads the configuration JSON file or creates one if one does not exist.
 * Since only one instance can be created, it will appear often in many classes.
 */
public class ConfigurationValues {
    /**
     * Returns the only instance of this singleton class.
     */
    private static ConfigurationValues instance;
    /**
     * A list of User IDs that are exempt from being affected by any role changes.
     * @see net.dv8tion.jda.api.entities.User
     */
    public Long[] exemptionList;
    /**
     * A list of Role IDs that can call !update to force update the roles.
     * @see net.dv8tion.jda.api.entities.Role
     */
    public Long[] rolesAllowedToUpdate;
    /**
     * A list of Channel IDs that are ignored when receiving messages for updating the role's holders.
     * @see net.dv8tion.jda.api.entities.Invite.Channel
     * @see me.alex.discord.MessageCooldownHandler#onMessageReceived(MessageReceivedEvent)
     */
    public Long[] ignoredChannels;
    /**
     * The percentage of the most active message senders that should get the role.
     * @see me.alex.discord.RoleUpdater#memberLoadCallback(List)
     */
    public double topPercentage;
    /**
     * An integer that describes how many weeks of data should be considered when changing who has the role.
     */
    public int weeksOfData;
    /**
     * Usually how often the main loop runs to update the roles, but is also used to signify one time use by some classes in order for a thread to terminate immediately
     * @see me.alex.discord.RoleUpdater
     * @see me.alex.discord.ForceUpdate
     */
    public long delay;
    /**
     * The ID of the role being updated
     * @see net.dv8tion.jda.api.JDA#getRoleById
     */
    public long roleId;
    /**
     * The ID of the server being updated
     * @see net.dv8tion.jda.api.JDA#getGuildById
     */
    public long serverId;
    /**
     * The cooldown of messages logged.
     * @see me.alex.discord.MessageCooldownHandler
     */
    public long messageCooldown;
    /**
     * The token of the bot.
     */
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
                instance.delay = 120000;
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