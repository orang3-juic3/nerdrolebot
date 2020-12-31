package me.alex.discord;

import com.google.gson.Gson;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CarbonRestImpl extends ListenerAdapter {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
        if (!e.getMessage().getContentRaw().startsWith("!carbon")) return;
        String message = e.getMessage().getContentRaw().substring(7);
        Gson gson = new Gson();
        if (message.length() == 1) {
            return;
        }
        if (message.startsWith("\n")) {
            message = message.substring(1);
        }
        if (message.equalsIgnoreCase("``````")) return;
        String pattern = "```.*([\\s\\S]*[^`])```";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(message);

        if (m.find()) {
            message = m.group(0);
            message = message.replaceAll("\"", "\\\"");
            message = message.replaceAll("```", "");
        } else {
            return;
        }
        e.getChannel().sendMessage("Retrieving carbon image..").queue();
        StringBuilder backgroundColour = new StringBuilder("rgba(");
        for (int i = 0; i < 3; i++) {
            backgroundColour.append(ThreadLocalRandom.current().nextInt(0, 255 + 1)).append(",");
        }
        backgroundColour.append("1)");
        ImageSettings imageSettings = new ImageSettings("seti", message, backgroundColour.toString(), null, "auto", null, null);
        imageSettings.replaceNulls();
        try {
            String json = gson.toJson(imageSettings);
            Request request = Request.Post("https://carbonara.now.sh/api/cook");
            request.bodyByteArray(json.getBytes(), ContentType.APPLICATION_JSON);
            HttpResponse httpResponse = request.execute().returnResponse();
            System.out.println(httpResponse.getStatusLine());
            if (httpResponse.getEntity() != null) {
                e.getChannel().sendFile(EntityUtils.toByteArray(httpResponse.getEntity()), e.getMessageId() + ".png").queue(message1 -> {
                    e.getChannel().sendMessage(String.format("<@!%s>", e.getAuthor().getId())).queue();
                    System.out.println("Done sending carbon screenshot.");
                });
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            e.getChannel().sendMessage("Could not execute this request due to an IO exception!").queue();
        }
    }

    private static class ImageSettings {
        public String theme;
        public String code;
        public String backgroundColor;
        public String fontFamily;
        public String language;
        public String paddingHorizontal;
        public String paddingVertical;

        public ImageSettings(String theme, String code, String backgroundColor, String fontFamily, String language, String paddingHorizontal, String paddingVertical) {
            this.theme = theme;
            this.code = code;
            this.backgroundColor = backgroundColor;
            this.fontFamily = fontFamily;
            this.language = language;
            this.paddingHorizontal = paddingHorizontal;
            this.paddingVertical = paddingVertical;
        }
        public void replaceNulls() {
            if (theme == null) {
                theme = "f";
            }
            if (code == null) code = "f";
            if (backgroundColor == null) backgroundColor = "f";
            if (fontFamily == null) fontFamily = "f";
            if (language == null) language = "f";
            if (paddingHorizontal == null) paddingHorizontal = "25px";
            if (paddingVertical == null) paddingVertical = "25px";
        }
    }
}
