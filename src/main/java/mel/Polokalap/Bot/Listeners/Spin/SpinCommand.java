package mel.Polokalap.Bot.Listeners.Spin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static mel.Polokalap.Bot.Main.*;

public class SpinCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject command = lang.get("commands").getAsJsonObject().get("spin").getAsJsonObject();
        String name = command.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        event.deferReply().queue();

        OptionMapping gamemodeOption = event.getOption("gamemode");
        OptionMapping tierOption = event.getOption("tier");

        String gamemode = gamemodeOption.getAsString();
        String tier = tierOption.getAsString();

        if (!List.of("LT5", "HT5", "LT4", "HT4", "LT3", "HT3", "LT2", "HT2", "LT1", "HT1").contains(tier.toUpperCase())) {

            event.getHook().sendMessage(
                    command.get("invalid-tier").getAsString()
            ).queue();
            return;

        }

        if (!gamemodeNames.contains(gamemode.toLowerCase())) {

            event.getHook().sendMessage(
                    command.get("invalid-gamemode").getAsString()
            ).queue();
            return;

        }

        String tierCategory = "";

        switch (tier.toLowerCase()) {

            case "ht1", "lt1" -> tierCategory = "t1";
            case "ht2", "lt2" -> tierCategory = "t2";
            case "ht3", "lt3" -> tierCategory = "t3";
            case "ht4", "lt4" -> tierCategory = "t4";
            case "ht5", "lt5" -> tierCategory = "t5";

        }

        if (tierCategory.isEmpty()) return;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ranked.hu/v1/leaderboard?gamemode=" + gamemode + "&from=0&count=-1"))
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

        ArrayList<String> players = new ArrayList<>();

        for (JsonElement player : json.get(tierCategory).getAsJsonArray()) {

            if (player.getAsJsonObject().get("tier").getAsString().toLowerCase().equals(tier.toLowerCase())) players.add("<@" + player.getAsJsonObject().get("discord_id").getAsString() + ">");

        }

        EmbedBuilder embed = new EmbedBuilder();
        JsonObject embedJson = command.get("embed").getAsJsonObject();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement element : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        int gamemodeId = -1;
        JsonArray gamemodesArray = data.getAsJsonArray("gamemodes");

        for (int i = 0; i < gamemodesArray.size(); i++) {
            JsonObject gm = gamemodesArray.get(i).getAsJsonObject();
            if (gm.get("name").getAsString().equalsIgnoreCase(gamemode)) {
                gamemodeId = i;
                break;
            }
        }

        if (gamemodeId == -1) return;

        System.out.println("data keys: " + data.keySet());
        System.out.println("data content: " + data);

        String gamemodeEmoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String tierLeft = Emoji.fromCustom(
                data.get("tiers").getAsJsonObject().get(tier.toLowerCase()).getAsJsonArray()
                        .get(0).getAsJsonObject().get("name").getAsString(),
                Long.parseLong(data.get("tiers").getAsJsonObject().get(tier.toLowerCase()).getAsJsonArray()
                        .get(0).getAsJsonObject().get("id").getAsString()),
                false
        ).getAsMention();

        String tierRight = Emoji.fromCustom(
                data.get("tiers").getAsJsonObject().get(tier.toLowerCase()).getAsJsonArray()
                        .get(1).getAsJsonObject().get("name").getAsString(),
                Long.parseLong(data.get("tiers").getAsJsonObject().get(tier.toLowerCase()).getAsJsonArray()
                        .get(1).getAsJsonObject().get("id").getAsString()),
                false
        ).getAsMention();

        String playersText = "";
        Random random = new Random();

        for (String player : players) playersText = playersText + player + "\n";

        String description = descriptionBuilder
                .toString()
                        .replace("%tier%", tierLeft + tierRight)
                        .replace("%gamemode%", gamemodeEmoji)
                        .replace("%count%", String.valueOf(players.size()))
                        .replace("%player%", players.get(random.nextInt(players.size() - 1)))
                        .replace("%players%", playersText);

        JsonArray colors = embedJson.get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        embed.setTitle(embedJson.get("title").getAsString());
        embed.setDescription(description);
        embed.setColor(color);
        embed.setFooter(embedJson.get("footer").getAsString());

        event.getHook().sendMessageEmbeds(embed.build()).queue();

    }

}
