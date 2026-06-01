package mel.Polokalap.Bot.Listeners.Moderation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Main;
import mel.Polokalap.Bot.Utils.Punishment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.lang;

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

        String name = flag.getAsJsonObject().get("name").getAsString();
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder descriptionBuilder = new StringBuilder();

        for (JsonElement line : lang.get("moderation").getAsJsonObject().get("description").getAsJsonArray()) {

            descriptionBuilder.append(line.getAsString() + "\n");

        }

        String description = descriptionBuilder
                .toString()
                .replace("%flag%", flag.getAsJsonObject().get("name").getAsString())
                .replace("%description%", flag.getAsJsonObject().get("description").getAsString());


        JsonArray colors = lang.get("moderation").getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

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

        embed.setTitle(lang.get("moderation").getAsJsonObject().get("title").getAsString());
        embed.setDescription(description);
        embed.setColor(color);
        embed.setFooter(lang.get("moderation").getAsJsonObject().get("footer").getAsString());

        message.delete().queue();

        message.getChannel()
                .sendMessage("<@" + message.getAuthor().getId() + ">")
                .setEmbeds(embed.build())
                .queue(sentMessage -> sentMessage.delete().queueAfter(lang.get("moderation").getAsJsonObject().get("keep_for").getAsInt(), TimeUnit.SECONDS));

        message.getAuthor().openPrivateChannel().flatMap(
                channel -> channel.sendMessageEmbeds(embed.build())
        ).queue();

    }

}
