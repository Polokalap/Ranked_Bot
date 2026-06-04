package mel.Polokalap.Bot.Listeners.Profile;

import com.google.gson.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static mel.Polokalap.Bot.Main.*;

public class ProfileCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("profile").getAsJsonObject();
        String name = profile.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        Member player = event.getOption("player").getAsMember();

        if (player == null) {

            event.reply(profile.get("fail").getAsString()).setEphemeral(true).queue();
            return;

        }

        JsonObject profileEmbed = commands.get("profile").getAsJsonObject().get("embed").getAsJsonObject();

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

            event.reply(profile.get("fail").getAsString()).setEphemeral(true).queue();
            return;

        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        // Building embed

        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement line : profileEmbed.get("description").getAsJsonArray()) {

            descriptionBuilder.append(line.getAsString() + "\n");

        }
        String description = descriptionBuilder
                .toString()
                .replace("%discord%", "<@" + player.getId() + ">")
                .replace("%position%", json.get("position").getAsString())
                .replace("%score%", json.get("points").getAsString());

        JsonArray alertColors = profileEmbed.getAsJsonObject().get("color").getAsJsonArray();
        Color alertColor = new Color(
                alertColors.get(0).getAsInt(),
                alertColors.get(1).getAsInt(),
                alertColors.get(2).getAsInt()
        );

        embed.setTitle(profileEmbed.get("title").getAsString().replace("%player%", json.get("name").getAsString()));
        embed.setDescription(description);

        int gamemodeId = 0;

        for (JsonElement emoji : data.get("gamemodes").getAsJsonArray()) {

            JsonObject obj = emoji.getAsJsonObject();

            String tier = json.get("tiers").getAsJsonObject().get(String.valueOf(gamemodeId + 1)).getAsString();

            embed.addField(
                    Emoji.fromCustom(obj.get("name").getAsString(), obj.get("id").getAsLong(), false).getAsMention()
                            + " " +
                            gamemodes.getAsJsonArray().get(gamemodeId).getAsJsonObject().get("html").getAsString(),
                    tier,
                    true);

            gamemodeId++;

        }

        embed.setColor(alertColor);
        embed.setFooter(profileEmbed.get("footer").getAsString());
        embed.setThumbnail("https://nmsr.jgj52.hu/bust/" + json.get("uuid").getAsString() + "?w=128");

        event.replyEmbeds(embed.build()).queue();

    }

}
