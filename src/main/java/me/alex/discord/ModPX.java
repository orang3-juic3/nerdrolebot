package me.alex.discord;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.List;

public class ModPX extends ListenerAdapter {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
        String message = e.getMessage().getContentRaw();
        if (!message.startsWith("!mod")) return;
        List<Member> mentionedMembers = e.getMessage().getMentionedMembers();
        if (mentionedMembers.size() != 1) return;
        int result = (int) (mentionedMembers.get(0).getIdLong() % 3);
        switch (result) {
            case 0:
                message = "No";
                break;
            case 1:
                message = "Definitely not.";
                break;
            case 2:
                message = "Maybe...";
                break;
            default:
                return;
        }
        e.getChannel().sendMessage(message).queue();
    }
}
