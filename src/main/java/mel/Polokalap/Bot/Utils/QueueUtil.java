package mel.Polokalap.Bot.Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import javax.swing.plaf.ActionMapUIResource;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumSet;
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
    public static HashMap<Member, Member> testing = new HashMap<>();
    public static HashMap<Integer, Message> queueMessage = new HashMap<>();
    public static HashMap<Member, TextChannel> memberChannel = new HashMap<>();

    public static final int MAX = 3000;
    public static final int N1 = 30;
    public static final int N2 = 60;
    public static final int N3 = 85;
    public static final int N4 = 95;

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
                .addComponents(ActionRow.of(
                        joinButton.getButton(),
                        leaveButton.getButton()
        ))
                .queue(

                        message -> {

                            queueMessage.put(gamemode, message);

                        }

                );

        isQueueActive.put(gamemode, true);

    }

    public static void createTicket(Guild guild, Member tester, Member player, int gamemode) {

        testing.put(tester, player);

        JsonObject queue = lang.get("queue").getAsJsonObject().get("ticket").getAsJsonObject();
        JsonObject embedJson = queue.get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();
        long categoryId = gamemodes.get(gamemode).getAsJsonObject().get("category").getAsLong();

        for (JsonElement element : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        StringBuilder queuePlayers = new StringBuilder();
        int count = 1;

        for (Member queuePlayer : queues.get(gamemode)) {

            queuePlayers.append("`" + count + ".` " + queuePlayer.getAsMention() + "\n");
            count++;

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

        String description = descriptionBuilder.toString()
                .replaceAll("%tester%", tester.getAsMention())
                .replaceAll("%player%", player.getAsMention())
                .replaceAll("%gamemode%", emoji + " " + gamemodeName);

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
                "queue-give-tier-" + gamemode,
                ButtonStyle.valueOf(embedJson.get("actions").getAsJsonObject().get("give-tier").getAsJsonObject().get("style").getAsString())
        );

        CustomButton closeButton = new CustomButton(
                embedJson.get("actions").getAsJsonObject().get("close").getAsJsonObject().get("text").getAsString(),
                "queue-close-ticket-" + gamemode,
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
                .addMemberPermissionOverride(
                        tester.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                        null
                )
                .addRolePermissionOverride(
                        guild.getPublicRole().getIdLong(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)
                )
                .queue(

                        textChannel -> {

                            memberChannel.put(player, textChannel);

                            textChannel
                                    .sendMessageEmbeds(embed.build())
                                    .addComponents(ActionRow.of(
                                            rateTestButton.getButton(),
                                            closeButton.getButton()
                                    ))
                                    .queue();

                        }

                );

    }

    public static void notifyDm(Guild guild, Member player, int gamemode) {

        JsonObject queue = lang.get("queue").getAsJsonObject().get("dm").getAsJsonObject();
        JsonObject embedJson = queue.get("embed").getAsJsonObject();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        String gamemodeName = gamemodes.get(gamemode).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(gamemode).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        for (JsonElement element : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(element.getAsString() + "\n");

        }

        String description = descriptionBuilder
                .toString()
                .replace("%tester%", getTester.get(gamemode).getAsMention())
                .replace("%gamemode%", emoji + " " + gamemodeName);

        JsonArray colors = embedJson.getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        embed.setTitle(embedJson.get("title").getAsString());
        embed.setDescription(description);
        embed.setColor(color);
        embed.setFooter(embedJson.get("footer").getAsString());

        player.getUser().openPrivateChannel().queue(

                privateChannel -> {

                    privateChannel
                            .sendMessage(player.getAsMention())
                            .setEmbeds(embed.build())
                            .queue();

                }

        );

    }

    public static void addToQueue(Member player, int gamemode) {

        queues.get(gamemode).add(player);

        updateEmbed(gamemode);

    }

    public static void removeFromQueue(Member player, int gamemode) {

        queues.get(gamemode).remove(player);

        updateEmbed(gamemode);

    }

    public static void newCycle(Guild guild, int gamemode) {

        if (queues.get(gamemode) == null || queues.get(gamemode).isEmpty()) return;

        Member player = queues.get(gamemode).getFirst();
        Member tester = getTester.get(gamemode);

        if (player == null) return;

        createTicket(guild, tester, player, gamemode);
        testing.put(tester, player);
        removeFromQueue(player, gamemode);
        notifyDm(guild, player, gamemode);

    }

    public static void updateEmbed(int gamemode) {

        Message message = queueMessage.get(gamemode);

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
                .replace("%tester%", tester.getAsMention())
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

        message
                .editMessageEmbeds(embed.build())
                .setComponents(ActionRow.of(
                        joinButton.getButton(),
                        leaveButton.getButton()
                ))
                .queue();

    }

    public static int tierToElo(Tiers tier, int highest) {

        int h = Math.max(highest, MAX);
        int n1 = Math.round(h * (N1 / 100f));
        int n2 = Math.round(h * (N2 / 100f));
        int n3 = Math.round(h * (N3 / 100f));
        int n4 = Math.round(h * (N4 / 100f));

        return switch (tier) {

            case Tiers.LT5 -> n1 / 2;
            case Tiers.HT5 -> (n1 + n2) / 2;
            case Tiers.LT4 -> (n2 + n3) / 2;
            case Tiers.HT4 -> (n3 + n4) / 2;
            case Tiers.LT3 -> (n4 + h) / 2;
            case Tiers.HT3 -> -2;
            case Tiers.LT2 -> -3;
            case Tiers.HT2 -> -4;
            case Tiers.LT1 -> -5;
            case Tiers.HT1 -> -6;

        };

    }

    public static void setTier(Member player, int gamemode, Tiers tier) {

        Database.execute(
                "UPDATE players SET elos = jsonb_set(elos, ARRAY[?::text], to_jsonb(?::text)) WHERE discord_id = ?",
                gamemode + 1, tierToElo(tier, MAX), player.getId()
        );

    }

    public static void addCooldown(Member player, int gamemode) {

        Database.execute(
                "UPDATE players SET last_tested = jsonb_set(COALESCE(last_tested, '{}'::jsonb), ARRAY[?]::text[], to_jsonb(?::bigint), true) WHERE discord_id = ?;",
                gamemode,
                System.currentTimeMillis() + lang.get("commands").getAsJsonObject().get("queue-panel").getAsJsonObject().get("cooldown").getAsLong(),
                player.getId()
        );

    }

}
