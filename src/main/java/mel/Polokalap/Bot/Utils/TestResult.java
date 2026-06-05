package mel.Polokalap.Bot.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static mel.Polokalap.Bot.Main.*;

public class TestResult {

    public static void anounceTest(Member tester, Member player, Guild guild, int gamemodeId, Tiers tier) {

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ranked.hu/v1/player?discord_id=" + player.getId()))
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

        if (response.statusCode() >= 400 && response.statusCode() <= 500) {

            return;

        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        TextChannel channel = guild.getTextChannelById(data.get("tests-channel").getAsLong());
        JsonObject resultJson = lang
                .get("test-result").getAsJsonObject()
                .get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder description = new StringBuilder();

        for (JsonElement line : resultJson.get("description").getAsJsonArray()) {

            description.append(line.getAsString() + "\n");

        }

        JsonArray colors = resultJson.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        JsonObject tiers = data.get("tiers").getAsJsonObject();
        JsonArray tierArray = tiers.get(tier.name().toLowerCase()).getAsJsonArray();

        String tierEmojis = Emoji.fromCustom(
                tierArray.get(0).getAsJsonObject().get("name").getAsString(),
                tierArray.get(0).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention().concat(Emoji.fromCustom(
                tierArray.get(1).getAsJsonObject().get("name").getAsString(),
                tierArray.get(1).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention());

        embed.setTitle(resultJson.get("title").getAsString());
        embed.setDescription(
                description
                        .toString()
                        .replace("%tester%", "<@" + tester.getId() + ">")
                        .replace("%player%", "<@" + player.getId() + ">")
                        .replace("%username%", json.get("name").getAsString())
                        .replace("%tier%", tierEmojis)
                        .replace("%gamemode%",
                                Emoji.fromCustom(
                                        data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("name").getAsString(),
                                        data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("id").getAsLong(),
                                        false)
                                        .getAsMention()
                                + " " +
                                gamemodes.get(gamemodeId).getAsJsonObject().get("html").getAsString())
        );

        embed.setThumbnail("https://nmsr.jgj52.hu/bust/" + json.get("uuid").getAsString() + "?w=128");

        embed.setColor(color);
        embed.setFooter(resultJson.get("footer").getAsString());

        channel
                .sendMessageEmbeds(embed.build())
                .queue(
                        message -> {

                            for (JsonElement element : resultJson.get("reaction-emojis").getAsJsonArray()) {

                                message.addReaction(Emoji.fromUnicode(element.getAsString())).queue();

                            }

                        }
                );

    }

}
