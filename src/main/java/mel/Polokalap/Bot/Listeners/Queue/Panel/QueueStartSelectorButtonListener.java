package mel.Polokalap.Bot.Listeners.Queue.Panel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mel.Polokalap.Bot.Utils.QueueUtil;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
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

        event.deferReply(true).queue();

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

        if (response.statusCode() >= 400 && response.statusCode() <= 499) {

            event.getHook()
                    .sendMessage(queue.get("not-registered").getAsString())
                    .setComponents(ActionRow.of(
                            Button.link(
                                    queue.get("register-button").getAsJsonObject().get("link").getAsString(),
                                    queue.get("register-button").getAsJsonObject().get("text").getAsString()
                            )
                    ))
                    .setEphemeral(true)
                    .queue();
            return;

        }

        boolean isTester = false;

        if (
                json.get("tester").getAsJsonObject().get(String.valueOf(storedId)) != null &&
                json.get("tester").getAsJsonObject().get(String.valueOf(storedId)).getAsBoolean()
        ) isTester = true;

        if (!isTester) {

            event.getHook().sendMessage(
                    queue.get("not-tester").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
            ).setEphemeral(true).queue();
            return;

        }

        if (testing.containsKey(member)) {

            event.getHook().sendMessage(queue.get("has-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
            return;

        }

        selectedGamemode.put(member, actualId);

        event.getHook().sendMessage(
                queue.get("gamemode-selected").getAsString()
                    .replace("%gamemode%", emoji + " " + gamemodeName)
        ).setEphemeral(true).queue();

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject queue = lang.get("commands").getAsJsonObject().get("queue-panel").getAsJsonObject();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (!id.equals("queue-start-button") && !id.equals("queue-stop-button") && !id.equals("queue-next-player")) return;

        event.deferReply(true).queue();

        if (!selectedGamemode.containsKey(member)) {

            event.getHook().sendMessage(queue.get("no-gamemode").getAsString()).setEphemeral(true).queue();
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

                event.getHook().sendMessage(queue.get("has-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            if (isQueueActive.getOrDefault(actualId, false)) {

                event.getHook().sendMessage(queue.get("queue-already-running").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            queues.put(actualId, new ArrayList<>());
            hasQueue.put(member, true);
            getTester.put(actualId, member);
            event.getHook().sendMessage(queue.get("started-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
            QueueUtil.announceQueue(event.getGuild(), member, actualId);

        } else if (id.equals("queue-stop-button")) {

            if (!hasQueue.getOrDefault(member, false)) {

                event.getHook().sendMessage(queue.get("no-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            hasQueue.put(member, false);
            queues.remove(actualId);
            queueMessage.get(actualId).delete().queue();
            isQueueActive.put(actualId, false);
            getTester.remove(actualId);
            event.getHook().sendMessage(queue.get("stopped-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();

        } else if (id.equals("queue-next-player")) {

            if (!hasQueue.getOrDefault(member, false)) {

                event.getHook().sendMessage(queue.get("no-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
                return;

            }

            if (queues.get(actualId).isEmpty()) {

                event.getHook().sendMessage(queue.get("queue-empty").getAsString()).setEphemeral(true).queue();
                return;

            }

            event.getHook().sendMessage(queue.get("next-player").getAsString().replace("%player%", queues.get(actualId).getFirst().getAsMention())).setEphemeral(true).queue();

            QueueUtil.newCycle(guild, actualId);

        }

    }

}
