package mel.Polokalap.Bot.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;

import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Main.data;

public class HighTestTicketUtil {

    public static HashMap<Member, Integer> highTicketGamemode = new HashMap<>();

    public static void createTicket(Guild guild, Member player, int gamemode) {

        JsonObject queue = lang.get("commands").getAsJsonObject().get("high-ticket-panel").getAsJsonObject().get("ticket").getAsJsonObject();
        JsonObject embedJson = queue.get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();
        long categoryId = gamemodes.get(gamemode).getAsJsonObject().get("category").getAsLong();

        for (JsonElement element : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        String gamemodeName = gamemodes.get(gamemode).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String logo = Emoji.fromCustom(
                data.get("logo-emoji").getAsJsonObject().get("name").getAsString(),
                data.get("logo-emoji").getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

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

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        String tier = json.get("tiers").getAsJsonObject().get(String.valueOf(gamemode + 1)).getAsString().toLowerCase();

        String tierLeft = Emoji.fromCustom(
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString(),
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(0).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String tierRight = Emoji.fromCustom(
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(1).getAsJsonObject().get("name").getAsString(),
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(1).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String description = descriptionBuilder.toString()
                .replaceAll("%player%", player.getAsMention())
                .replaceAll("%gamemode%", emoji + " " + gamemodeName)
                .replaceAll("%tier%", tierLeft + tierRight);

        JsonArray colors = embedJson.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        embed.setTitle(
                embedJson.get("title").getAsString()
                        .replace("%gamemode%", emoji + " " + gamemodeName)
                        .replace("%logo%", logo)
        );
        embed.setDescription(description);
        embed.setFooter(embedJson.get("footer").getAsString());
        embed.setColor(color);

        CustomButton rateTestButton = new CustomButton(
                embedJson.get("actions").getAsJsonObject().get("give-tier").getAsJsonObject().get("text").getAsString(),
                "high-test-give-tier-" + gamemode,
                ButtonStyle.valueOf(embedJson.get("actions").getAsJsonObject().get("give-tier").getAsJsonObject().get("style").getAsString())
        );

        CustomButton closeButton = new CustomButton(
                embedJson.get("actions").getAsJsonObject().get("close").getAsJsonObject().get("text").getAsString(),
                "high-test-close-ticket-" + gamemode,
                ButtonStyle.valueOf(embedJson.get("actions").getAsJsonObject().get("close").getAsJsonObject().get("style").getAsString())
        );

        Category category = guild.getCategoryById(categoryId);

        guild.createTextChannel(queue.get("prefix").getAsString() + player.getEffectiveName())
                .setParent(category)
                .addMemberPermissionOverride(
                        player.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                        null
                )
                .addRolePermissionOverride(
                        guild.getPublicRole().getIdLong(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)
                )
                .addRolePermissionOverride(
                        data.get("manager-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .queue(

                        textChannel -> {

                            textChannel
                                    .sendMessageEmbeds(embed.build())
                                    .addComponents(ActionRow.of(
                                            rateTestButton.getButton(),
                                            closeButton.getButton()
                                    ))
                                    .queue();

                            sendDms(guild, player, textChannel, gamemode);

                        }

                );

    }

    public static void sendDms(Guild guild, Member player, TextChannel textChannel, int gamemode) {

        JsonObject queue = lang.get("commands").getAsJsonObject().get("high-ticket-panel").getAsJsonObject();
        JsonObject embedJson = queue.get("dm").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();
        long categoryId = gamemodes.get(gamemode).getAsJsonObject().get("category").getAsLong();

        for (JsonElement element : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        String gamemodeName = gamemodes.get(gamemode).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String logo = Emoji.fromCustom(
                data.get("logo-emoji").getAsJsonObject().get("name").getAsString(),
                data.get("logo-emoji").getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

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

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        String tier = json.get("tiers").getAsJsonObject().get(String.valueOf(gamemode + 1)).getAsString().toLowerCase();

        String tierLeft = Emoji.fromCustom(
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString(),
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(0).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String tierRight = Emoji.fromCustom(
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(1).getAsJsonObject().get("name").getAsString(),
                data.get("tiers").getAsJsonObject().get(tier).getAsJsonArray().get(1).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        String description = descriptionBuilder.toString()
                .replaceAll("%player%", player.getAsMention())
                .replaceAll("%gamemode%", emoji + " " + gamemodeName)
                .replaceAll("%tier%", tierLeft + tierRight)
                .replaceAll("%channel%", textChannel.getAsMention());

        JsonArray colors = embedJson.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        embed.setTitle(
                embedJson.get("title").getAsString()
                        .replace("%gamemode%", emoji + " " + gamemodeName)
                        .replace("%logo%", logo)
        );
        embed.setDescription(description);
        embed.setFooter(embedJson.get("footer").getAsString());
        embed.setColor(color);

        Role managerRole = guild.getRoleById(data.get("manager-role").getAsLong());

        guild.findMembersWithRoles(managerRole).onSuccess(members -> {

            for (Member member : members) {

                member.getUser().openPrivateChannel().queue(

                        privateChannel -> {

                            privateChannel.sendMessageEmbeds(embed.build()).queue();

                        }

                );

            }

        });

    }

}
