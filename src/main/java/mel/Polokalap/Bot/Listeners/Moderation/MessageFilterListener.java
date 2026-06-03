package mel.Polokalap.Bot.Listeners.Moderation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Main;
import mel.Polokalap.Bot.Utils.CustomButton;
import mel.Polokalap.Bot.Utils.Punishment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.*;

public class MessageFilterListener extends ListenerAdapter {

    private static JsonObject filter;
    private static HashMap<Member, ArrayList<Long>> userFlags = new HashMap<>();

    static {
        try {
            filter = Main.load("filter.json");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

        if (
                event.getAuthor().isBot() ||
                event.isWebhookMessage() ||
                event.getMember() == null
        ) return;

        JsonArray flags = filter.get("flags").getAsJsonArray();
        String message = event.getMessage()
                .getContentRaw()
                .toLowerCase()
                .replace(" ", "")
                .replace("\\", "")
                .replace("\n", "");

        String[] splitMessage = event.getMessage()
                .getContentRaw()
                .toLowerCase()
                .split(" ");

        if (
                event.getMember().hasPermission(Permission.MANAGE_CHANNEL) ||
                event.getMember().hasPermission(Permission.MESSAGE_MANAGE)
        ) return;

        for (JsonElement flag : flags) {

            Punishment punishment;

            try {
                punishment = Punishment.valueOf(
                        flag.getAsJsonObject().get("punishment").getAsString().toUpperCase()
                );
            } catch (IllegalArgumentException | NullPointerException e) {
                punishment = Punishment.NONE;
            }

            for (JsonElement key : flag.getAsJsonObject().get("always").getAsJsonArray()) {

                if (message.contains(key.getAsString().toLowerCase())) {

                    respondBack(event.getMessage(), flag, punishment);
                    return;

                }

            }

            for (JsonElement key : flag.getAsJsonObject().get("flags").getAsJsonArray()) {

                if (message.contains(key.getAsString().toLowerCase())) {

                    boolean contains = false;

                    for (JsonElement ignoreKey : flag.getAsJsonObject().get("ignore").getAsJsonArray()) {

                        if (message.contains(ignoreKey.getAsString().toLowerCase())) contains = true;

                    }

                    if (!contains) {

                        respondBack(event.getMessage(), flag, punishment);
                        return;

                    }

                }

                // Chunking

                for (String chunk : splitMessage) {

                    if (chunk.contains(key.getAsString().toLowerCase())) {

                        boolean contains = false;

                        for (JsonElement ignoreKey : flag.getAsJsonObject().get("ignore").getAsJsonArray()) {

                            if (chunk.contains(ignoreKey.getAsString().toLowerCase())) contains = true;

                        }

                        if (!contains) {

                            respondBack(event.getMessage(), flag, punishment);
                            return;

                        }

                    }

                }

            }

        }

    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {

        if (
                event.getAuthor().isBot() || event.getMember() == null
        ) return;

        JsonArray flags = filter.get("flags").getAsJsonArray();
        String message = event.getMessage()
                .getContentRaw()
                .toLowerCase()
                .replace(" ", "")
                .replace("\\", "")
                .replace("\n", "");

        String[] splitMessage = event.getMessage()
                .getContentRaw()
                .toLowerCase()
                .split(" ");

        if (
                event.getMember().hasPermission(Permission.MANAGE_CHANNEL) ||
                        event.getMember().hasPermission(Permission.MESSAGE_MANAGE)
        ) return;

        for (JsonElement flag : flags) {

            Punishment punishment;

            try {
                punishment = Punishment.valueOf(
                        flag.getAsJsonObject().get("punishment").getAsString().toUpperCase()
                );
            } catch (IllegalArgumentException | NullPointerException e) {
                punishment = Punishment.NONE;
            }

            for (JsonElement key : flag.getAsJsonObject().get("always").getAsJsonArray()) {

                if (message.contains(key.getAsString().toLowerCase())) {

                    respondBack(event.getMessage(), flag, punishment);
                    return;

                }

            }

            for (JsonElement key : flag.getAsJsonObject().get("flags").getAsJsonArray()) {

                if (message.contains(key.getAsString().toLowerCase())) {

                    boolean contains = false;

                    for (JsonElement ignoreKey : flag.getAsJsonObject().get("ignore").getAsJsonArray()) {

                        if (message.contains(ignoreKey.getAsString().toLowerCase())) contains = true;

                    }

                    if (!contains) {

                        respondBack(event.getMessage(), flag, punishment);
                        return;

                    }

                }

                // Chunking

                for (String chunk : splitMessage) {

                    if (chunk.contains(key.getAsString().toLowerCase())) {

                        boolean contains = false;

                        for (JsonElement ignoreKey : flag.getAsJsonObject().get("ignore").getAsJsonArray()) {

                            if (chunk.contains(ignoreKey.getAsString().toLowerCase())) contains = true;

                        }

                        if (!contains) {

                            respondBack(event.getMessage(), flag, punishment);
                            return;

                        }

                    }

                }

            }

        }

    }

    private static void respondBack(Message message, JsonElement flag, Punishment punishment) {

        // Building embed

        String name = flag.getAsJsonObject().get("name").getAsString();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();
        JsonObject moderation = lang.get("moderation").getAsJsonObject();

        for (JsonElement line : moderation.get("description").getAsJsonArray()) {

            descriptionBuilder.append(line.getAsString() + "\n");

        }

        String description = descriptionBuilder
                .toString()
                .replace("%flag%", name)
                .replace("%description%", flag.getAsJsonObject().get("description").getAsString());


        JsonArray colors = moderation.get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        // Setting punishment (keep as is)

        switch (punishment) {

            case NONE:
                break;

            case FIVE_MIN:
                message.getMember().timeoutFor(5, TimeUnit.MINUTES).queue();
                break;

            case ONE_HOUR:
                message.getMember().timeoutFor(1, TimeUnit.HOURS).queue();
                break;

            case ONE_DAY:
                message.getMember().timeoutFor(1, TimeUnit.DAYS).queue();
                break;

            case THREE_DAYS:
                message.getMember().timeoutFor(3, TimeUnit.DAYS).queue();
                break;

            case SEVEN_DAYS:
                message.getMember().timeoutFor(7, TimeUnit.DAYS).queue();
                break;

            case KICK:
                message.getGuild().kick(message.getMember()).queue();
                break;

            case BAN:
                message.getGuild().ban(message.getMember(), 0, TimeUnit.SECONDS).queue();
                break;

            default:
                break;

        }

        // Building embed

        embed.setTitle(moderation.get("title").getAsString());
        embed.setDescription(description);
        embed.setColor(color);
        embed.setFooter(moderation.get("footer").getAsString());

        message.delete().queue(); // Delete message (wow no sheesh sherlock)

        // Sending embed TO CHAT

        message.getChannel()
                .sendMessage("<@" + message.getAuthor().getId() + ">")
                .setEmbeds(embed.build())
                .queue(sentMessage -> sentMessage.delete().queueAfter(moderation.get("keep_for").getAsInt(), TimeUnit.SECONDS));

        // Sending embed to DM

        message.getAuthor().openPrivateChannel().flatMap(
                channel -> channel.sendMessageEmbeds(embed.build())
        ).queue();

        // Time out user if too many flags in a given amount of time

        userFlags.putIfAbsent(message.getMember(), new ArrayList<>());
        userFlags.get(message.getMember()).add(System.currentTimeMillis());

        int amountOfFlags = 0;

        for (long value : userFlags.get(message.getMember())) {

            if (System.currentTimeMillis() - value < 10000) amountOfFlags++;

        }

        JsonObject timeout = lang.get("moderation").getAsJsonObject()
                .get("alert").getAsJsonObject()
                .get("timeout").getAsJsonObject();

        if (amountOfFlags >= timeout.get("after").getAsInt()) {

            userFlags.get(message.getMember()).clear();
            message.getMember().timeoutFor(Duration.ofMinutes(timeout.get("punishment").getAsInt())).queue();

            // Building DM embed

            EmbedBuilder dmEmbed = new EmbedBuilder();
            StringBuilder dmDescriptionBuilder = new StringBuilder();

            for (JsonElement line : timeout.get("description").getAsJsonArray()) {

                dmDescriptionBuilder.append(line.getAsString() + "\n");

            }

            String dmDescription = dmDescriptionBuilder
                    .toString()
                    .replace("%time%", "<t:" + (System.currentTimeMillis() / 1000 + (long) timeout.get("punishment").getAsInt() * 60) + ":R>");


            JsonArray dmColors = timeout.get("color").getAsJsonArray();
            Color dmColor = new Color(
                    dmColors.get(0).getAsInt(),
                    dmColors.get(1).getAsInt(),
                    dmColors.get(2).getAsInt()
            );

            dmEmbed.setTitle(timeout.get("title").getAsString());
            dmEmbed.setDescription(dmDescription);
            dmEmbed.setColor(dmColor);
            dmEmbed.setFooter(timeout.get("footer").getAsString());

            message.getAuthor().openPrivateChannel().flatMap(
                    channel -> channel.sendMessageEmbeds(dmEmbed.build())
            ).queue();

        }

        // Alert for mods

        TextChannel channel = jda.getTextChannelById(data.get("mod-hannel").getAsLong());
        EmbedBuilder modEmbed = new EmbedBuilder();
        StringBuilder modDescriptionBuilder = new StringBuilder();
        JsonObject alert = moderation.get("alert").getAsJsonObject();

        for (JsonElement line : alert.get("description").getAsJsonArray()) {

            modDescriptionBuilder.append(line.getAsString() + "\n");

        }

        String alertDescription = modDescriptionBuilder
                .toString()
                .replace("%username%", "<@" + message.getAuthor().getId() + ">")
                .replace("%id%", message.getAuthor().getId())
                .replace("%reason%", name)
                .replace("%message%", message.getContentRaw())
                .replace("%time%", "<t:" + System.currentTimeMillis() / 1000 + ":S>")
                .replace("%path%", "<#" + message.getChannelId() + ">");

        JsonArray alertColors = alert.getAsJsonObject().get("color").getAsJsonArray();
        Color alertColor = new Color(
                alertColors.get(0).getAsInt(),
                alertColors.get(1).getAsInt(),
                alertColors.get(2).getAsInt()
        );

        modEmbed.setTitle(alert.get("title").getAsString());
        modEmbed.setDescription(alertDescription);
        modEmbed.setColor(alertColor);
        modEmbed.setFooter(alert.get("footer").getAsString());

        JsonObject actions = alert.get("actions").getAsJsonObject();

        CustomButton muteUser = new CustomButton(actions.get("mute").getAsJsonObject().get("text").getAsString(), "moderation_mute_user_" + message.getAuthor().getId(), ButtonStyle.valueOf(actions.get("mute").getAsJsonObject().get("style").getAsString()));
        CustomButton kickUser = new CustomButton(actions.get("kick").getAsJsonObject().get("text").getAsString(), "moderation_kick_user_" + message.getAuthor().getId(), ButtonStyle.valueOf(actions.get("kick").getAsJsonObject().get("style").getAsString()));
        CustomButton banUser = new CustomButton(actions.get("ban").getAsJsonObject().get("text").getAsString(), "moderation_ban_user_" + message.getAuthor().getId(), ButtonStyle.valueOf(actions.get("ban").getAsJsonObject().get("style").getAsString()));

        channel
                .sendMessageEmbeds(modEmbed.build())
                .setActionRow(
                        muteUser.getButton(),
                        kickUser.getButton(),
                        banUser.getButton()
                )
                .queue();

    }

}
