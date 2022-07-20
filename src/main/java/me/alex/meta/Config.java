package me.alex.meta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is a thread-safe singleton class that reads the configuration JSON file or creates one if one does not exist.
 * Since only one instance can be created, it will appear often in many classes.
 */
public class Config {
    // TODO: 09/05/2021 Phase out keeping references of Config

    /**
     * Mutex for this object.
     */
    private static final Object mutex = new Object();
    /**
     * Whether mutex is needed
     */
    private static boolean mutexNeeded = false;
    /**
     *
     * Returns the only instance of this singleton class.
     */
    private static Config instance;
    /**
     * A list of User IDs that are exempt from being affected by any role changes.
     * @see net.dv8tion.jda.api.entities.User
     */
    private Long[] exemptionList;
    /**
     * A list of Role IDs that can call !update to force update the roles.
     * @see net.dv8tion.jda.api.entities.Role
     */
    private Long[] rolesAllowedToUpdate;
    /**
     * A list of Channel IDs that are ignored when receiving messages for updating the role's holders.
     * @see net.dv8tion.jda.api.entities.Invite.Channel
     * @see me.alex.discord.MessageCooldownHandler#onMessageReceived(MessageReceivedEvent)
     */
    private Long[] ignoredChannels;
    /**
     * The percentage of the most active message senders that should get the role.
     * @see me.alex.discord.RoleUpdater#memberLoadCallback(List)
     */
    private double topPercentage;
    /**
     * An integer that describes how many weeks of data should be considered when changing who has the role.
     */
    private int weeksOfData;
    /**
     * Usually how often the main loop runs in milliseconds to update the roles, but is also used to signify one time use by some classes in order for a thread to terminate immediately
     * @see me.alex.discord.RoleUpdater
     * @see me.alex.discord.ForceUpdate
     */
    private long delay;
    /**
     * The ID of the role being updated
     * @see net.dv8tion.jda.api.JDA#getRoleById
     */
    private long roleId;
    /**
     * The ID of the server being updated
     * @see net.dv8tion.jda.api.JDA#getGuildById
     */
    private long serverId;
    /**
     * The cooldown of messages logged.
     * @see me.alex.discord.MessageCooldownHandler
     */
    private long messageCooldown;
    /**
     * The token of the bot.
     */
    private String botToken;

    /**
     * The command prefix (now redundant)
     */
    private char prefix;
    /**
     * A list of blacklisted strings
     */
    private String[] blacklist;
    private Config() {
    }
    public static void loadConfig() {
        synchronized (mutex) {
            try {
                if (mutexNeeded) {
                    mutex.wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("AAAAAAAAAAAAAA I HATE CHECKED EXCEPTIONS KOTLIN SHELTER ME PLSSSSSS");
            }
            mutexNeeded = true;
            try {
                File f = new File("conf.json");
                if (f.createNewFile()) {
                    instance = new Config();
                    BufferedWriter bufferedWriter = new BufferedWriter(new PrintWriter(f));
                    // bufferedWriter.write("{\"exemptionList\":[479285497487949853, 230336110637744131],\"weeksOfData\":2,\"roleId\":706554375572684860,\"serverId\":679434326282207238}");
                    instance.exemptionList = new Long[]{266279893543682049L, 230336110637744131L};
                    instance.weeksOfData = 2;
                    instance.delay = 120000;
                    instance.roleId = 706554375572684860L;
                    instance.serverId = 679434326282207238L;
                    instance.botToken = "NzExOTk1MjExMzc0Mzk1NDEy.XsLHNg.IZgzg4W7RwkExCYNXa7vmc_L5us";
                    instance.messageCooldown = 60000L;
                    instance.rolesAllowedToUpdate = new Long[] {718236096814645289L,772163720062173214L };
                    instance.ignoredChannels = new Long[] {738006372725293086L};
                    instance.topPercentage = 50;
                    instance.prefix = '!';
                    instance.blacklist = new String[]{"nigga", "nigger","negro", "fag", "faggot", "fagg","udemy.com/course"};
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
                bufferedReader.close();
                Gson gson = new Gson();
                instance = gson.fromJson(config.toString(), Config.class);
                if (instance.topPercentage > 100 || instance.topPercentage< 0) {
                    throw new InvalidConfigurationException("The value top percentage in conf.json cannot be less than 0 or larger than 100!");
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            mutex.notifyAll();
            mutexNeeded = false;
            LogManager.getRootLogger().info("Loaded config file!");
        }
    }

    /**
     * This method reads the configuration and assigns values accordingly. In the case of a configuration not existing, it uses default values and creates a configuration.
     * @return Returns <b>the instance</b> of Config.
     * @see Gson
     */
    public static Config getInstance() {
        if (instance == null) {
            Config.loadConfig();
        }
        return threadSafeGet(instance);
    }

    /**
     * @return The object supplied, after waiting for the mutex.
     */
    @NotNull
    private static <T> T threadSafeGet(T get) {
        synchronized (mutex) {
            try {
                if (mutexNeeded) {
                    mutex.wait();
                }
                return get;
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new NullPointerException("Thread interrupted!");
            }
        }
    }

    public Long[] getExemptionList() {
        return threadSafeGet(Config.getInstance().exemptionList);
    }

    public Long[] getRolesAllowedToUpdate() {
        return threadSafeGet(Config.getInstance().rolesAllowedToUpdate);
    }

    public Long[] getIgnoredChannels() {
        return threadSafeGet(Config.getInstance().ignoredChannels);
    }

    public double getTopPercentage() {
        return threadSafeGet(Config.getInstance().topPercentage);
    }

    public int getWeeksOfData() {
        return threadSafeGet(Config.getInstance().weeksOfData);
    }

    public long getDelay() {
        return threadSafeGet(Config.getInstance().delay);
    }

    public long getRoleId() {
        return threadSafeGet(Config.getInstance().roleId);
    }

    public long getServerId() {
        return threadSafeGet(Config.getInstance().serverId);
    }

    public long getMessageCooldown() {
        return threadSafeGet(Config.getInstance().messageCooldown);
    }

    public String getBotToken() {
        return threadSafeGet(Config.getInstance().botToken);
    }

    @Deprecated(forRemoval = true)
    public char getPrefix() {
        return threadSafeGet(Config.getInstance().prefix);
    }

    public String[] getBlacklist() {
        return threadSafeGet(Config.getInstance().blacklist);
    }
}