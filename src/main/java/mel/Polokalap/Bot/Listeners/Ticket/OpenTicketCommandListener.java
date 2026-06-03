package mel.Polokalap.Bot.Listeners.Ticket;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import mel.Polokalap.Bot.Utils.CustomButton;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.awt.*;
import java.util.EnumSet;

import static mel.Polokalap.Bot.Main.data;
import static mel.Polokalap.Bot.Main.lang;

public class OpenTicketCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();
        String name = ticket.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        // Check if channel already exists

        Guild guild = event.getGuild();
        Category category = guild.getCategoryById(data.get("ticket-category").getAsLong());

        for (Channel channel : category.getChannels()) {

            if (channel.getName().replace(ticket.get("prefix").getAsString(), "").equals(event.getMember().getEffectiveName().toLowerCase())) {

                event.reply(ticket.get("fail").getAsString()).setEphemeral(true).queue();
                return;

            }

        }

        StringBuilder topicBuilder = new StringBuilder();

        for (JsonElement topicLine : ticket.get("topic").getAsJsonArray()) {

            topicBuilder.append(
                    topicLine.getAsString()
                            .replace("%username%", event.getMember().getEffectiveName())
                            .replace("%id%", event.getMember().getId())
                            + "\n"
            );

        }

        // Building embed

        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder description = new StringBuilder();
        JsonArray colors = ticket.get("embed").getAsJsonObject().get("color").getAsJsonArray();

        for (JsonElement line : ticket.get("embed").getAsJsonObject().get("description").getAsJsonArray()) {

            description.append(
                    line.getAsString()
                            .replace("%user%", "<@" + event.getMember().getId() + ">")
                    + "\n"
            );

        }

        Color color = new Color(
                colors.get(0).getAsInt(),
                colors.get(1).getAsInt(),
                colors.get(2).getAsInt()
        );

        // Creating buttons

        JsonObject actions = ticket.get("embed").getAsJsonObject().get("actions").getAsJsonObject();

        CustomButton closeButton = new CustomButton(
                actions.getAsJsonObject().get("close").getAsJsonObject().get("text").getAsString(),
                "ticket_close_" + event.getMember().getId(),
                ButtonStyle.valueOf(actions.getAsJsonObject().get("close").getAsJsonObject().get("style").getAsString())
        );

        CustomButton archiveButton = new CustomButton(
                actions.getAsJsonObject().get("archive").getAsJsonObject().get("text").getAsString(),
                "ticket_archive_" + event.getMember().getId(),
                ButtonStyle.valueOf(actions.getAsJsonObject().get("archive").getAsJsonObject().get("style").getAsString())
        );

        embed.setTitle(ticket.get("embed").getAsJsonObject().get("title").getAsString());
        embed.setDescription(description.toString());
        embed.setColor(color);
        embed.setFooter(
                ticket.get("embed").getAsJsonObject().get("footer").getAsString()
                        .replace("%id%", event.getMember().getId())
        );

        guild.createTextChannel(ticket.get("prefix").getAsString() + event.getMember().getEffectiveName())
                .setParent(category)
                .addMemberPermissionOverride(
                        event.getMember().getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                        null
                )
                .addRolePermissionOverride(
                        guild.getPublicRole().getIdLong(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)
                )
                .setTopic(topicBuilder.toString())
                .queue(
                channel -> {

                    channel
                            .sendMessage(ticket.get("embed").getAsJsonObject().get("ping").getAsString())
                            .setEmbeds(embed.build())
                            .setActionRow(
                                    closeButton.getButton(),
                                    archiveButton.getButton()
                            )
                            .queue();

                    event.reply(ticket.get("created").getAsString().replace("%channel%", "<#" + channel.getId() + ">")).setEphemeral(true).queue();

                }
        );

    }

}
