package mel.Polokalap.Bot.Listeners.Queue.Panel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mel.Polokalap.Bot.Utils.QueueUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Utils.QueueUtil.*;

public class QueueJoinLeaveButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject queue = lang.get("commands").getAsJsonObject().get("queue-panel").getAsJsonObject();
        Member member = event.getMember();

        if (id.startsWith("join-queue-")) {

            int actualId = Integer.parseInt(id.replace("join-queue-", ""));

            if (getTester.get(actualId).equals(member)) {

                event.reply(queue.get("queue-host").getAsString()).setEphemeral(true).queue();
                return;

            }

            if (queues.get(actualId).size() >= lang.get("queue").getAsJsonObject().get("limit").getAsInt()) {

                event.reply(queue.get("queue-full").getAsString()).setEphemeral(true).queue();
                return;

            }

            String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
            String emoji = Emoji.fromCustom(
                    data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                    data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention();

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.ranked.hu/v1/player?discord_id=" + member.getId()))
                    .GET()
                    .build();

            HttpResponse<String> response = null;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (response.statusCode() == 400) {

                event.reply(queue.get("not-registered").getAsString()).setEphemeral(true).queue();
                return;

            }

            JsonElement cooldownEl = json.get("elos").getAsJsonObject().get("last_tested");
            long lastTest = 0L;

            if (cooldownEl != null && cooldownEl.isJsonObject()) {

                JsonElement idEl = cooldownEl.getAsJsonObject().get(String.valueOf(actualId));

                if (idEl != null) {

                    lastTest = idEl.getAsLong();

                }

            }

            if (System.currentTimeMillis() - lastTest <= queue.get("cooldown").getAsLong()) {

                event.reply(queue.get("on-cooldown").getAsString().replace("%date%", "<t:" + lastTest + queue.get("cooldown").getAsLong() / 1000 + ":R>")).setEphemeral(true).queue();
                return;

            }

            if (queues.get(actualId).contains(member)) {

                event.reply(queue.get("in-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            event.reply(queue.get("joined-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
            QueueUtil.addToQueue(member, actualId);

        }

        if (id.startsWith("leave-queue-")) {

            int actualId = Integer.parseInt(id.replace("leave-queue-", ""));

            String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
            String emoji = Emoji.fromCustom(
                    data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                    data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention();

            if (!queues.get(actualId).contains(member)) {

                event.reply(queue.get("not-in-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            QueueUtil.removeFromQueue(member, actualId);
            event.reply(queue.get("left-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();

        }

    }

}
