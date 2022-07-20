package me.alex.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;

public class ModPX {
    @SubscribeEvent
    public void onCommand(@Nonnull SlashCommandInteractionEvent e) {
        if (!e.getName().equals("mod")) return;
        int result = (int) (requireNonNull(e.getOption("user")).getAsUser().getIdLong() % 3);
        String message;
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
        e.reply(message).queue();
    }
}
