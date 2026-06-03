package mel.Polokalap.Bot.Listeners.Ticket.Panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.CustomButton;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.*;

import static mel.Polokalap.Bot.Main.lang;

public class TicketPanelCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket-panel").getAsJsonObject();
        String name = ticket.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        // Check permission

        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) {

            event.reply(ticket.get("permission").getAsString()).setEphemeral(true).queue();
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

        embed.setTitle(embedJson.get("title").getAsString());
        embed.setDescription(descriptionBuilder.toString());
        embed.setColor(color);
        embed.setFooter(embedJson.get("footer").getAsString());

        CustomButton ticketButton = new CustomButton(
                embedJson.get("actions").getAsJsonObject().get("open").getAsJsonObject().get("text").getAsString(),
                "ticket_panel_button",
                ButtonStyle.valueOf(embedJson.get("actions").getAsJsonObject().get("open").getAsJsonObject().get("style").getAsString())
        );

        event.reply(ticket.get("done").getAsString()).setEphemeral(true).queue();
        event.getChannel()
                .sendMessageEmbeds(embed.build())
                .setActionRow(
                        ticketButton.getButton()
                )
                .queue();

    }

}
