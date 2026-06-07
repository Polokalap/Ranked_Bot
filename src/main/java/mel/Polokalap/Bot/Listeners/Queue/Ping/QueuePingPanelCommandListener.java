package mel.Polokalap.Bot.Listeners.Queue.Ping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.CustomSelector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

import static mel.Polokalap.Bot.Main.*;

public class QueuePingPanelCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject queue = commands.get("queue-ping").getAsJsonObject();
        String name = queue.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        boolean ephemeral = true;

        if (event.getMember().getPermissions().contains(Permission.MANAGE_CHANNEL)) {

            ephemeral = false;

        }

        EmbedBuilder embed = new EmbedBuilder();
        JsonObject actions = queue.get("embed").getAsJsonObject().get("actions").getAsJsonObject();
        StringBuilder descriptionBuilder = new StringBuilder();

        CustomSelector menu = new CustomSelector(
                "queue-selector",
                actions.get("selector").getAsJsonObject().get("text").getAsString()
        );

        int gamemodeId = 0;

        for (JsonElement entry : gamemodes) {

            Emoji gamemodeEmoji = Emoji.fromCustom(
                    data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("name").getAsString(),
                    data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("id").getAsLong(),
                    false
            );

            menu.addOption(
                    entry.getAsJsonObject().get("html").getAsString(),
                    "queue-selector-" + gamemodeId,
                    gamemodeEmoji
            );

            gamemodeId++;

        }

        for (JsonElement entry : queue.get("embed").getAsJsonObject().get("description").getAsJsonArray()) {

            descriptionBuilder.append(entry.getAsString() + "\n");

        }

        String description = descriptionBuilder.toString();

        JsonArray colors = queue.get("embed").getAsJsonObject().get("color").getAsJsonArray();
        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        embed.setTitle(queue.get("embed").getAsJsonObject().get("title").getAsString());
        embed.setDescription(description);
        embed.setFooter(queue.get("embed").getAsJsonObject().get("footer").getAsString());
        embed.setColor(color);

        if (ephemeral) {

            event.replyEmbeds(embed.build()).setActionRow(menu.getMenu()).setEphemeral(true).queue();
            return;

        }

        event.getChannel()
                .sendMessageEmbeds(embed.build())
                .setActionRow(menu.getMenu())
                .queue();

        event.reply(queue.get("sent").getAsString()).setEphemeral(true).queue();

    }

}
