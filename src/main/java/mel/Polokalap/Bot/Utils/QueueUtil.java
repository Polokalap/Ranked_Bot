package mel.Polokalap.Bot.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Main.data;

public class QueueUtil {

    public static HashMap<Member, Integer> selectedGamemode = new HashMap<>();
    public static HashMap<Integer, List<Member>> queues = new HashMap<>();
    public static HashMap<Member, Boolean> hasQueue = new HashMap<>();

    public static HashMap<Integer, Boolean> isQueueActive = new HashMap<>();
    public static HashMap<Integer, Member> getTester = new HashMap<>();
    public static HashMap<Integer, Message> queueMessage = new HashMap<>();

    public static void announceQueue(Guild guild, Member tester, int gamemode) {

        TextChannel channel = guild.getChannelById(TextChannel.class, gamemodes.get(gamemode).getAsJsonObject().get("channel").getAsLong());
        JsonObject queue = lang.get("queue").getAsJsonObject().get("player").getAsJsonObject().get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement element : queue.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        String description = descriptionBuilder.toString()
                .replace("%tester%", "<@" + tester.getId() + ">")
                .replace("%queue%", "")
                .replace("%size%", String.valueOf(queues.getOrDefault(tester, List.of()).size()));

        JsonArray colors = queue.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        String gamemodeName = gamemodes.get(gamemode).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        embed.setTitle(
                queue.get("title").getAsString()
                        .replace("%gamemode%", emoji + " " + gamemodeName)
        );
        embed.setDescription(description);
        embed.setFooter(queue.get("footer").getAsString());
        embed.setColor(color);

        CustomButton joinButton = new CustomButton(
                queue.get("actions").getAsJsonObject().get("join-queue").getAsJsonObject().get("text").getAsString(),
                "join-queue-" + gamemode,
                ButtonStyle.valueOf(queue.get("actions").getAsJsonObject().get("join-queue").getAsJsonObject().get("style").getAsString())
        );

        CustomButton leaveButton = new CustomButton(
                queue.get("actions").getAsJsonObject().get("leave-queue").getAsJsonObject().get("text").getAsString(),
                "leave-queue-" + gamemode,
                ButtonStyle.valueOf(queue.get("actions").getAsJsonObject().get("leave-queue").getAsJsonObject().get("style").getAsString())
        );

        channel
                .sendMessage("<@&" + gamemodes.get(gamemode).getAsJsonObject().get("role").getAsString() + ">")
                .setEmbeds(embed.build())
                .setActionRow(
                        joinButton.getButton(),
                        leaveButton.getButton()
                )
                .queue(

                        message -> {

                            queueMessage.put(gamemode, message);

                        }

                );

        isQueueActive.put(gamemode, true);

    }

    public static void createTicket(Guild guild, Member tester, Member player, int gamemode) {



    }

    public static void addToQueue(Member player, int gamemode) {

        queues.get(gamemode).add(player);

        JsonObject queue = lang.get("queue").getAsJsonObject().get("player").getAsJsonObject().get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement element : queue.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        StringBuilder queuePlayers = new StringBuilder();
        int count = 1;

        for (Member queuePlayer : queues.get(gamemode)) {

            queuePlayers.append("`" + count + ".` " + queuePlayer.getAsMention() + "\n");
            count++;

        }

        Member tester = getTester.get(gamemode);

        String description = descriptionBuilder.toString()
                .replace("%tester%", "<@" + tester.getId() + ">")
                .replace("%queue%", queuePlayers.toString())
                .replace("%size%", String.valueOf(queues.getOrDefault(gamemode, List.of()).size()));

        JsonArray colors = queue.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        String gamemodeName = gamemodes.get(gamemode).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        embed.setTitle(
                queue.get("title").getAsString()
                        .replace("%gamemode%", emoji + " " + gamemodeName)
        );
        embed.setDescription(description);
        embed.setFooter(queue.get("footer").getAsString());
        embed.setColor(color);

        CustomButton joinButton = new CustomButton(
                queue.get("actions").getAsJsonObject().get("join-queue").getAsJsonObject().get("text").getAsString(),
                "join-queue-" + gamemode,
                ButtonStyle.valueOf(queue.get("actions").getAsJsonObject().get("join-queue").getAsJsonObject().get("style").getAsString())
        );

        CustomButton leaveButton = new CustomButton(
                queue.get("actions").getAsJsonObject().get("leave-queue").getAsJsonObject().get("text").getAsString(),
                "leave-queue-" + gamemode,
                ButtonStyle.valueOf(queue.get("actions").getAsJsonObject().get("leave-queue").getAsJsonObject().get("style").getAsString())
        );

        queueMessage.get(gamemode)
                .editMessageEmbeds(embed.build())
                .setActionRow(
                        joinButton.getButton(),
                        leaveButton.getButton()
                )
                .queue();

    }

    public static void removeFromQueue(Member player, int gamemode) {

        queues.get(gamemode).remove(player);

        JsonObject queue = lang.get("queue").getAsJsonObject().get("player").getAsJsonObject().get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement element : queue.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        StringBuilder queuePlayers = new StringBuilder();
        int count = 1;

        for (Member queuePlayer : queues.get(gamemode)) {

            queuePlayers.append("`" + count + ".` " + queuePlayer.getAsMention() + "\n");
            count++;

        }

        Member tester = getTester.get(gamemode);

        String description = descriptionBuilder.toString()
                .replace("%tester%", "<@" + tester.getId() + ">")
                .replace("%queue%", queuePlayers.toString())
                .replace("%size%", String.valueOf(queues.getOrDefault(gamemode, List.of()).size()));

        JsonArray colors = queue.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        String gamemodeName = gamemodes.get(gamemode).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        embed.setTitle(
                queue.get("title").getAsString()
                        .replace("%gamemode%", emoji + " " + gamemodeName)
        );
        embed.setDescription(description);
        embed.setFooter(queue.get("footer").getAsString());
        embed.setColor(color);

        CustomButton joinButton = new CustomButton(
                queue.get("actions").getAsJsonObject().get("join-queue").getAsJsonObject().get("text").getAsString(),
                "join-queue-" + gamemode,
                ButtonStyle.valueOf(queue.get("actions").getAsJsonObject().get("join-queue").getAsJsonObject().get("style").getAsString())
        );

        CustomButton leaveButton = new CustomButton(
                queue.get("actions").getAsJsonObject().get("leave-queue").getAsJsonObject().get("text").getAsString(),
                "leave-queue-" + gamemode,
                ButtonStyle.valueOf(queue.get("actions").getAsJsonObject().get("leave-queue").getAsJsonObject().get("style").getAsString())
        );

        queueMessage.get(gamemode)
                .editMessageEmbeds(embed.build())
                .setActionRow(
                        joinButton.getButton(),
                        leaveButton.getButton()
                )
                .queue();

    }

}
