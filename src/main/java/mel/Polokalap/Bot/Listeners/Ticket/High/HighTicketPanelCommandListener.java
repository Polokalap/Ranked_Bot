package mel.Polokalap.Bot.Listeners.Ticket.High;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.CustomButton;
import mel.Polokalap.Bot.Utils.CustomSelector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;

import static mel.Polokalap.Bot.Main.*;
import static mel.Polokalap.Bot.Main.data;

public class HighTicketPanelCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("high-ticket-panel").getAsJsonObject();
        String name = ticket.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        event.deferReply(true).queue();

        // Check permission

        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {

            event.getHook().sendMessage(ticket.get("permission").getAsString()).setEphemeral(true).queue();
            return;

        }

        // Build embed

        EmbedBuilder embed = new EmbedBuilder();
        JsonObject embedJson = ticket.get("embed").getAsJsonObject();
        StringBuilder descriptionBuilder = new StringBuilder();
        JsonArray colors = embedJson.get("color").getAsJsonArray();

        for (JsonElement line : embedJson.get("description").getAsJsonArray()) {

            descriptionBuilder.append(line.getAsString() + "\n");

        }

        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        // embed.setTitle(embedJson.get("title").getAsString());
        // embed.setDescription(descriptionBuilder.toString());
        // embed.setColor(color);
        // embed.setFooter(embedJson.get("footer").getAsString());

        CustomSelector gamemodeSelector = new CustomSelector(
                "high-test-selector",
                embedJson.get("actions").getAsJsonObject().get("selector").getAsJsonObject().get("text").getAsString()
        );

        int gamemodeId = 0;

        for (JsonElement entry : gamemodes) {

            Emoji gamemodeEmoji = Emoji.fromCustom(
                    data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("name").getAsString(),
                    data.get("gamemodes").getAsJsonArray().get(gamemodeId).getAsJsonObject().get("id").getAsLong(),
                    false
            );

            gamemodeSelector.addOption(
                    entry.getAsJsonObject().get("html").getAsString(),
                    "high-test-selector-" + gamemodeId,
                    gamemodeEmoji
            );

            gamemodeId++;

        }

        CustomButton ticketButton = new CustomButton(
                embedJson.get("actions").getAsJsonObject().get("open-ticket").getAsJsonObject().get("text").getAsString(),
                "high_ticket_panel_button",
                ButtonStyle.valueOf(embedJson.get("actions").getAsJsonObject().get("open-ticket").getAsJsonObject().get("style").getAsString())
        );

        Container container = Container.of(
                TextDisplay.of("## " + embedJson.get("title").getAsString()),
                TextDisplay.of(descriptionBuilder.toString()),
                Separator.create(true, Separator.Spacing.SMALL),
                ActionRow.of(
                        gamemodeSelector.getMenu()
                ),
                ActionRow.of(
                        ticketButton.getButton()
                ),
                Separator.create(true, Separator.Spacing.SMALL),
                TextDisplay.of("-# " + embedJson.get("footer").getAsString())
        ).withAccentColor(color);

        event.getChannel()
                .sendMessageComponents(container)
                .useComponentsV2()
                .queue(

                        message -> {

                            event.getHook().sendMessage(ticket.get("done").getAsString()).setEphemeral(true).queue();

                        }

                );

    }

}
