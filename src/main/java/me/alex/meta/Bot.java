package me.alex.meta;

import me.alex.discord.*;
import me.alex.sql.DatabaseManager;
import me.alex.sql.MessageUpdater;
import me.alex.sql.RoleUpdateQuery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.*;

/**
 * A class designed to making the whole building process easier. Registers listeners, passes around instances accordingly etc.
 * It also gets the main loop started.
 */
public class Bot {
    private Config config = Config.getInstance();
    private static JDA jda;
    private DatabaseManager databaseManager;
    private RoleUpdateQuery roleUpdateQuery;
    private MessageCooldownHandler messageCooldownHandler;
    private MessageUpdater messageUpdater;
    private RoleUpdater roleUpdater;
    private final RetrieveLeaderboard retrieveLeaderboard;


    /**
     * Does the bot making.
     * <b>Do not use any of the getters or setters without calling this function!</b>
     */
    public Bot() {
        EnumSet<GatewayIntent> gatewayIntents = EnumSet.allOf(GatewayIntent.class);
        JDABuilder jdaBuilder = JDABuilder.create(Config.getInstance().getBotToken(), gatewayIntents).setEventManager(new AnnotatedEventManager());
        try {
            jda = jdaBuilder.build();
            jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
        databaseManager = new DatabaseManager();
        roleUpdateQuery = new RoleUpdateQuery(databaseManager);
        roleUpdater = new RoleUpdater(false);
        messageCooldownHandler = new MessageCooldownHandler();
        ForceUpdate forceUpdate = new ForceUpdate(this);
        roleUpdater.addListener(forceUpdate);
        retrieveLeaderboard = new RetrieveLeaderboard();
        roleUpdateQuery.addListener(roleUpdater);
        jda.addEventListener(retrieveLeaderboard);
        roleUpdateQuery.addListener(retrieveLeaderboard);
        messageUpdater = new MessageUpdater(roleUpdateQuery, messageCooldownHandler);
        jda.addEventListener(messageCooldownHandler, new ModPX(),
                new Blacklist(),
                forceUpdate,
                //new CarbonRestImpl(),
                configReloadListener());
        createCommands();
    }
    private void createCommands() {
        jda.updateCommands().addCommands(
        Commands.slash("update","Update commands")
                .addSubcommands(new SubcommandData("info","Info on when the database was last updated."),
                        new SubcommandData("force","Force updates the database.")),
        Commands.slash("mod","The most overpowered command.")
                .addOption(OptionType.USER, "user", "The user.",true, false),
        Commands.slash("leaderboard","Shows the top chatters, or your or someone else's place amongst them!")
                .addSubcommands(new SubcommandData("page", "Show page.").addOption(OptionType.INTEGER, "page","The page of the leaderboard to fetch.", true, false))
                .addSubcommands(new SubcommandData("user", "Shows your or another user's place on the leaderboard.")
                        .addOption(OptionType.USER, "target", "The user", false, false)
                        .addOptions(new OptionData(OptionType.INTEGER, "place", "The position in the leaderboard to look at.", false, false).setRequiredRange(1, Integer.MAX_VALUE))),
                Commands.slash("config", "Work with the config.").addSubcommands(new SubcommandData("reload","Reloads the config"))
        ).queue();
    }
    private Object configReloadListener() {
        return new Object() {
            @SubscribeEvent
            public void onGuildMessage(SlashCommandInteractionEvent e) {
                if (!e.isFromGuild()) return;
                if (!e.getCommandPath().equalsIgnoreCase("config/reload")) return;
                e.deferReply().queue(it -> new Thread(() -> {
                    System.out.println(Objects.requireNonNull(e.getGuild()).getIdLong() != Config.getInstance().getServerId());
                    if (Objects.requireNonNull(e.getGuild()).getIdLong() != Config.getInstance().getServerId()) return;
                    System.out.println(e.getMember() != null);
                    if (e.getMember() != null) {
                        final List<Long> roles = List.of(config.getRolesAllowedToUpdate());
                        if (e.getMember().getRoles().stream().mapToLong(Role::getIdLong).anyMatch(roles::contains)) {
                            System.out.println(1);
                            Config.loadConfig();
                            config = Config.getInstance();
                            e.getHook().sendMessage("Reloaded config file!").queue();
                        } else {
                            System.out.println(2);
                            e.getHook().sendMessageEmbeds(RetrieveLeaderboard.createErrorEmbed("You do not have the permission to execute this command!")).queue();
                        }
                    }
                }).start());
            }
        };
    }

    /**
     * @return Returns the config instance.
     * @see Config
     */
    public Config getConfig() {
        return config;
    }


    /**
     * @return Returns the RoleUpdater instance associated with this method.
     * @see RoleUpdater
     */
    public RoleUpdater getRoleUpdater() {
        return roleUpdater;
    }

    /**
     * @param roleUpdater Sets this classes roleUpdater instance to the instance from the parameter.
     * @see RoleUpdater
     */
    public void setRoleUpdater(RoleUpdater roleUpdater) {
        this.roleUpdater = roleUpdater;
    }

    /**
     * @return The JDA instance in which all discord api calls are done from.
     * @see JDA
     */
    public static JDA getJDA() {
        return jda;
    }

    /**
     * @return An instance of the databaseConnectionManager or null if you haven't called SequenceBuilder#build(). This will have listeners inside.
     * @see Bot
     * @see DatabaseManager
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * @param databaseManager Sets the instance of DatabaseConnectionManager within this class
     * @see DatabaseManager
     */
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * @return Returns an instance of RoleUpdateQuery with the command boolean set to false or null if you haven't called SequenceBuilder#build().
     * @see RoleUpdateQuery
     */
    public RoleUpdateQuery getRoleUpdateQuery() {
        return roleUpdateQuery;
    }

    /**
     * @param roleUpdateQuery Sets the instance of RoleUpdateQuery within this instance of SequenceBuilder.
     * @see RoleUpdateQuery
     */
    public void setRoleUpdateQuery(RoleUpdateQuery roleUpdateQuery) {
        this.roleUpdateQuery = roleUpdateQuery;
    }

    /**
     * @return Returns an instance of MessageCooldownHandler or null if you haven't called SequenceBuilder#build().
     * @see MessageCooldownHandler
     */
    public MessageCooldownHandler getMessageCooldownHandler() {
        return messageCooldownHandler;
    }

    /**
     * @param messageCooldownHandler Sets the instance of MessageCooldownHandler within this instance of SequenceBuilder.
     * @see MessageCooldownHandler
     */
    public void setMessageCooldownHandler(MessageCooldownHandler messageCooldownHandler) {
        this.messageCooldownHandler = messageCooldownHandler;
    }

    /**
     * @return Returns an instance of MessageUpdater or null if you haven't called SequenceBuilder#build()
     */
    public MessageUpdater getMessageUpdater() {
        return messageUpdater;
    }

    /**
     * @param messageUpdater Sets the instance of MessageUpdater within this instance of SequenceBuilder.
     * @see MessageUpdater
     */
    public void setMessageUpdater(MessageUpdater messageUpdater) {
        this.messageUpdater = messageUpdater;
    }

    public RetrieveLeaderboard getRetrieveLeaderboard() {
        return retrieveLeaderboard;
    }
}
