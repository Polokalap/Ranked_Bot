package mel.Polokalap.Bot.Listeners.Profile;

import com.google.gson.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

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

        String minecraftName = event.getOption("minecraft_name") != null
                ? event.getOption("minecraft_name").getAsString()
                : "";

        OptionMapping playerOption = event.getOption("player");
        Member player = playerOption != null ? playerOption.getAsMember() : null;

        if (player == null && minecraftName.isEmpty()) {

            event.reply(profile.get("missing").getAsString()).setEphemeral(true).queue();
            return;

        }

        String requestType = player == null ? "name" : "discord_id";
        String requestText = player == null ? minecraftName : player.getId();

        JsonObject profileEmbed = commands.get("profile").getAsJsonObject().get("embed").getAsJsonObject();

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ranked.hu/v1/player?" + requestType + "=" + requestText))
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
                .replace("%discord%", "<@" + json.get("discord_id").getAsString() + ">")
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
        JsonObject tiers = data.get("tiers").getAsJsonObject();

        for (JsonElement emoji : data.get("gamemodes").getAsJsonArray()) {

            int storedId = gamemodes.get(gamemodeId).getAsJsonObject().get("stored").getAsInt();
            JsonObject obj = emoji.getAsJsonObject();
            String tier = json.get("tiers").getAsJsonObject().get(String.valueOf(gamemodeId + 1)).getAsString();

            boolean retired;

            if (json.get("retired").getAsJsonObject().get(String.valueOf(storedId)) != null) retired = json.get("retired").getAsJsonObject().get(String.valueOf(storedId)).getAsBoolean();
            else retired = false;

            if (tier.isEmpty()) tier = "unranked";

            JsonArray tierArray = tiers.get((retired ? "r" : "") + tier.toLowerCase()).getAsJsonArray();

            String fieldValue = Emoji.fromCustom(
                    tierArray.get(0).getAsJsonObject().get("name").getAsString(),
                    tierArray.get(0).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention().concat(Emoji.fromCustom(
                    tierArray.get(1).getAsJsonObject().get("name").getAsString(),
                    tierArray.get(1).getAsJsonObject().get("id").getAsLong(),
                    false
            ).getAsMention());

            embed.addField(
                    Emoji.fromCustom(obj.get("name").getAsString(), obj.get("id").getAsLong(), false).getAsMention()
                            + " " +
                            gamemodes.getAsJsonArray().get(gamemodeId).getAsJsonObject().get("html").getAsString(),
                    fieldValue,
                    true
            );

            gamemodeId++;

        }

        while (embed.getFields().size() % 3 != 0) {

            embed.addField("", "", true);

        }

        embed.setColor(alertColor);
        embed.setFooter(profileEmbed.get("footer").getAsString());
        embed.setThumbnail("https://nmsr.jgj52.dev/bust/" + json.get("uuid").getAsString() + "?w=128");

        event.replyEmbeds(embed.build()).queue();

    }

}
