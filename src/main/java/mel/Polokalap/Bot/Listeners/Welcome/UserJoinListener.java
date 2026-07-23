package mel.Polokalap.Bot.Listeners.Welcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static mel.Polokalap.Bot.Main.*;

public class UserJoinListener extends ListenerAdapter {

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

        // Building embed

        JsonObject welcome = lang.get("welcome").getAsJsonObject();
        JsonObject embedJson = welcome.get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();
        Guild guild = event.getGuild();
        TextChannel channel = guild.getTextChannelById(data.get("welcome-channel").getAsLong());
        Member member = event.getMember();

        for (JsonElement line : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(line.getAsString() + "\n");

        }
        String description = descriptionBuilder
                .toString()
                .replace("%user%", "<@" + event.getMember().getId() + ">")
                .replace("%channel%", "<#" + data.get("rules-channel").getAsLong() + ">");

        JsonArray colors = embedJson.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        embed.setTitle(
                embedJson.get("title").getAsString()
                    .replace("%emoji%", Emoji.fromCustom(
                                data.get("plus-emoji").getAsJsonObject().get("name").getAsString(),
                                data.get("plus-emoji").getAsJsonObject().get("id").getAsLong(),
                                false).getAsMention()
                    )
        );
        embed.setDescription(description);
        embed.setColor(color);
        embed.setFooter(embedJson.get("footer").getAsString());
        embed.setThumbnail(event.getMember().getEffectiveAvatarUrl());

        channel.sendMessageEmbeds(embed.build()).queue(
                message -> {

                    for (JsonElement element : embedJson.get("reaction-emojis").getAsJsonArray()) {

                        message.addReaction(Emoji.fromUnicode(element.getAsString())).queue();

                    }

                }
        );;

        guild.addRoleToMember(member, guild.getRoleById(data.get("default-role").getAsLong())).queue();

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

        if (response.statusCode() >= 400 && response.statusCode() <= 500) return;

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (!json.get("banned").getAsBoolean()) return;

        Role bannedRole = guild.getRoleById(data.get("banned-role").getAsString());

        guild.addRoleToMember(member, bannedRole).queue();

    }

}
