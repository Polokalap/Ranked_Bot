package mel.Polokalap.Bot.Listeners.Ticket.High;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mel.Polokalap.Bot.Utils.HighTestTicketUtil;
import mel.Polokalap.Bot.Utils.QueueUtil;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
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
import java.util.List;

import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Utils.HighTestTicketUtil.highTicketGamemode;
import static mel.Polokalap.Bot.Utils.QueueUtil.*;

public class HighTicketSelectorButtonListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        JsonObject queue = lang.get("commands").getAsJsonObject().get("high-ticket-panel").getAsJsonObject();

        String componentId = event.getComponentId();
        if (!componentId.equals("high-test-selector")) return;

        event.deferReply(true).queue();

        String selected = event.getValues().get(0);
        int actualId = Integer.parseInt(selected.replace("high-test-selector-", ""));
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

        List<String> highTier = List.of("LT3", "HT3", "LT2", "HT2", "LT1", "HT1");

        if (!highTier.contains(json.get("tiers").getAsJsonObject().get(String.valueOf(actualId + 1)).getAsString())) {

            event.getHook()
                    .sendMessage(
                            queue.get("low-tier").getAsString()
                                    .replace("%gamemode%", emoji + " " + gamemodeName)
                    )
                    .setEphemeral(true)
                    .queue();
            return;

        }

        if (hasQueue.getOrDefault(member, false)) {

            event.getHook().sendMessage(queue.get("has-queue").getAsString().replace("%gamemode%", emoji + " " + gamemodeName)).setEphemeral(true).queue();
            return;

        }

        highTicketGamemode.put(member, actualId);

        event.getHook().sendMessage(
                queue.get("gamemode-selected").getAsString()
                    .replace("%gamemode%", emoji + " " + gamemodeName)
        ).setEphemeral(true).queue();

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject panel = lang.get("commands").getAsJsonObject().get("high-ticket-panel").getAsJsonObject();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (!id.equals("high_ticket_panel_button")) return;

        event.deferReply(true).queue();

        if (!highTicketGamemode.containsKey(member)) {

            event.getHook().sendMessage(panel.get("no-gamemode").getAsString()).setEphemeral(true).queue();
            return;

        }

        int actualId = highTicketGamemode.get(member);
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

        if (response.statusCode() >= 400 && response.statusCode() <= 499) {

            event.getHook()
                    .sendMessage(panel.get("not-registered").getAsString())
                    .setComponents(ActionRow.of(
                            Button.link(
                                    panel.get("register-button").getAsJsonObject().get("link").getAsString(),
                                    panel.get("register-button").getAsJsonObject().get("text").getAsString()
                            )
                    ))
                    .setEphemeral(true)
                    .queue();
            return;

        }

        List<String> highTier = List.of("LT3", "HT3", "LT2", "HT2", "LT1", "HT1");

        if (!highTier.contains(json.get("tiers").getAsJsonObject().get(String.valueOf(actualId + 1)).getAsString())) {

            event.getHook()
                    .sendMessage(
                            panel.get("low-tier").getAsString()
                                    .replace("%gamemode%", emoji + " " + gamemodeName)
                    )
                    .setEphemeral(true)
                    .queue();
            return;

        }

        if (id.equals("high_ticket_panel_button")) {

            Category category = guild.getCategoryById(gamemodes.get(actualId).getAsJsonObject().get("category").getAsLong());
            String channelName = panel.get("ticket").getAsJsonObject().get("prefix").getAsString() + member.getEffectiveName();

            for (Channel channel : category.getChannels()) {

                if (channel.getName().replace(panel.get("ticket").getAsJsonObject().get("prefix").getAsString(), "").contains(member.getEffectiveName().toLowerCase())) {

                    event.getHook().sendMessage(
                            panel.get("has-ticket").getAsString()
                                    .replace("%gamemode%", emoji + " " + gamemodeName)
                    ).setEphemeral(true).queue();
                    return;

                }

            }

            HighTestTicketUtil.createTicket(guild, member, actualId);

            event.getHook().sendMessage(
                    panel.get("ticket-opened").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
            ).setEphemeral(true).queue();

        }

    }

}
