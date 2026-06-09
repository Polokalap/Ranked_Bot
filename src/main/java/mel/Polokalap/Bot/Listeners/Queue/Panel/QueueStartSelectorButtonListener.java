package mel.Polokalap.Bot.Listeners.Queue.Panel;

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

public class QueueStartSelectorButtonListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        JsonObject queue = lang.get("commands").getAsJsonObject().get("queue-panel").getAsJsonObject();

        String componentId = event.getComponentId();
        if (!componentId.equals("start-queue-selector")) return;

        String selected = event.getValues().get(0);
        int actualId = Integer.parseInt(selected.replace("queue-selector-", ""));
        int storedId = gamemodes.get(actualId).getAsJsonObject().get("stored").getAsInt();

        String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        Member member = event.getMember();
        Guild guild = event.getGuild();

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

        boolean isTester = false;

        if (
                json.get("tester").getAsJsonObject().get(String.valueOf(storedId)) != null &&
                json.get("tester").getAsJsonObject().get(String.valueOf(storedId)).getAsBoolean()
        ) isTester = true;

        if (!isTester) {

            event.reply(
                    queue.get("not-tester").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
            ).setEphemeral(true).queue();
            return;

        }

        if (queues.containsKey(member)) {

            event.reply(queue.get("has-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
            return;

        }

        selectedGamemode.put(member, actualId);

        event.reply(
                queue.get("gamemode-selected").getAsString()
                    .replace("%gamemode%", emoji + " " + gamemodeName)
        ).setEphemeral(true).queue();

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject queue = lang.get("commands").getAsJsonObject().get("queue-panel").getAsJsonObject();
        Member member = event.getMember();

        if (!id.equals("queue-start-button") && !id.equals("queue-stop-button")) return;

        if (!selectedGamemode.containsKey(member)) {

            event.reply(queue.get("no-gamemode").getAsString()).setEphemeral(true).queue();
            return;

        }

        int actualId = selectedGamemode.get(member);
        int storedId = gamemodes.get(actualId).getAsJsonObject().get("stored").getAsInt();

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

        boolean isTester = false;

        if (
                json.get("tester").getAsJsonObject().get(String.valueOf(storedId)) != null &&
                        json.get("tester").getAsJsonObject().get(String.valueOf(storedId)).getAsBoolean()
        ) isTester = true;

        String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        if (id.equals("queue-start-button")) {

            if (hasQueue.getOrDefault(member, false)) {

                event.reply(queue.get("has-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            if (isQueueActive.getOrDefault(actualId, false)) {

                event.reply(queue.get("queue-already-running").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            queues.put(actualId, new ArrayList<>());
            hasQueue.put(member, true);
            getTester.put(actualId, member);
            event.reply(queue.get("started-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
            QueueUtil.announceQueue(event.getGuild(), member, actualId);

        }

        if (id.equals("queue-stop-button")) {

            if (!hasQueue.getOrDefault(member, true)) {

                event.reply(queue.get("no-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            hasQueue.put(member, false);
            queues.remove(actualId);
            queueMessage.get(actualId).delete().queue();
            isQueueActive.put(actualId, false);
            getTester.remove(actualId);
            event.reply(queue.get("stopped-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();

        }

    }

}
