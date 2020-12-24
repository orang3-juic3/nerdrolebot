package me.alex.discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

public class ModPX extends ListenerAdapter {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
        String message = e.getMessage().getContentRaw();
        if (!message.startsWith("!mod ")) return;
        message = message.substring(5);
        if (!message.startsWith("<@!")) return;
        if (!message.endsWith(">")) return;
        message = message.substring(0, message.length() - 1);
        message = message.substring(3);
        if (message.matches("[0-9]")) return;
        User user = e.getJDA().getUserById(message);
        if (user == null) {
            return;
        }
        int rnd = ThreadLocalRandom.current().nextInt(0, 2 + 1);
        switch (rnd) {
            case 0:
                message = "No";
                break;
            case 1:
                message = "Definitely not.";
                break;
            case 2:
                message = "Maybe...";
                break;
        }
        e.getChannel().sendMessage(message).queue();
    }
}
