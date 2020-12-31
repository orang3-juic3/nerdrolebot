package me.alex.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ModPX extends ListenerAdapter {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
        String message = e.getMessage().getContentRaw();
        if (!message.startsWith("!mod")) return;
        List<Member> mentionedMembers = e.getMessage().getMentionedMembers();
        if (mentionedMembers.size() != 1) return;
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
