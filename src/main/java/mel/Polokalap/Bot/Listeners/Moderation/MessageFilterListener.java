package mel.Polokalap.Bot.Listeners.Moderation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Main;
import mel.Polokalap.Bot.Utils.Punishment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;

import java.awt.*;
import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.*;

public class MessageFilterListener extends ListenerAdapter {

    private static JsonObject filter;

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
                .replace("\n", "");

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

                    for (JsonElement ignoreKey : flag.getAsJsonObject().get("ignore").getAsJsonArray()) {

                        if (message.contains(ignoreKey.getAsString().toLowerCase())) return;

                    }

                    respondBack(event.getMessage(), flag, punishment);
                    return;

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

        // Alert for mods

        TextChannel channel = jda.getTextChannelById(MOD_CHANNEL);
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
                .replace("%time%", "<t:" + System.currentTimeMillis() + ":S>");

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

        channel
                .sendMessageEmbeds(modEmbed.build())
                .queue();

    }

}
