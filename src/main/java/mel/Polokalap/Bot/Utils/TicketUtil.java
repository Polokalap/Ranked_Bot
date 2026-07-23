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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;
import java.util.EnumSet;

import static mel.Polokalap.Bot.Main.data;
import static mel.Polokalap.Bot.Main.lang;

public class TicketUtil {

    public static boolean createTicket(SlashCommandInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();
        Guild guild = event.getGuild();
        Member member = event.getMember();

        // Check if channel already exist

        Category category = guild.getCategoryById(data.get("ticket-category").getAsLong());

        for (Channel channel : category.getChannels()) {

            if (channel.getName().replace(ticket.get("prefix").getAsString(), "").equals(member.getEffectiveName().toLowerCase())) {

                event.reply(ticket.get("fail").getAsString()).setEphemeral(true).queue();
                return false;

            }

        }

        StringBuilder topicBuilder = new StringBuilder();

        for (JsonElement topicLine : ticket.get("topic").getAsJsonArray()) {

            topicBuilder.append(
                    topicLine.getAsString()
                            .replace("%username%", member.getEffectiveName())
                            .replace("%id%", member.getId())
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
                            .replace("%user%", "<@" + member.getId() + ">")
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
                "ticket_close_" + member.getId(),
                ButtonStyle.valueOf(actions.getAsJsonObject().get("close").getAsJsonObject().get("style").getAsString())
        );

        CustomButton archiveButton = new CustomButton(
                actions.getAsJsonObject().get("archive").getAsJsonObject().get("text").getAsString(),
                "ticket_archive_" + member.getId(),
                ButtonStyle.valueOf(actions.getAsJsonObject().get("archive").getAsJsonObject().get("style").getAsString())
        );

        embed.setTitle(ticket.get("embed").getAsJsonObject().get("title").getAsString());
        embed.setDescription(description.toString());
        embed.setColor(color);
        embed.setFooter(
                ticket.get("embed").getAsJsonObject().get("footer").getAsString()
                        .replace("%id%", member.getId())
        );
        embed.setThumbnail(member.getEffectiveAvatarUrl());

        guild.createTextChannel(ticket.get("prefix").getAsString() + member.getUser().getName())
                .setParent(category)
                .addMemberPermissionOverride(
                        member.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                        null
                )
                .addRolePermissionOverride(
                        guild.getPublicRole().getIdLong(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)
                )
                .addRolePermissionOverride(
                        data.get("moderator-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .addRolePermissionOverride(
                        data.get("manager-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .addRolePermissionOverride(
                        data.get("admin-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .setTopic(topicBuilder.toString())
                .queue(
                        channel -> {

                            channel
                                    .sendMessage(ticket.get("embed").getAsJsonObject().get("ping").getAsString())
                                    .setEmbeds(embed.build())
                                    .addComponents(ActionRow.of(
                                            closeButton.getButton(),
                                            archiveButton.getButton()
                                    ))
                                    .flatMap(Message::pin)
                                    .queue();

                            event.reply(ticket.get("created").getAsString().replace("%channel%", "<#" + channel.getId() + ">")).setEphemeral(true).queue();

                        }
                );

        return true;

    }

    public static boolean createTicket(ButtonInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();
        Guild guild = event.getGuild();
        Member member = event.getMember();

        event.deferReply(true).queue();

        // Check if channel already exist

        Category category = guild.getCategoryById(data.get("ticket-category").getAsLong());

        for (Channel channel : category.getChannels()) {

            if (channel.getName().replace(ticket.get("prefix").getAsString(), "").equals(member.getEffectiveName().toLowerCase())) {

                event.getHook().sendMessage(ticket.get("fail").getAsString()).setEphemeral(true).queue();
                return false;

            }

        }

        StringBuilder topicBuilder = new StringBuilder();

        for (JsonElement topicLine : ticket.get("topic").getAsJsonArray()) {

            topicBuilder.append(
                    topicLine.getAsString()
                            .replace("%username%", member.getUser().getName())
                            .replace("%id%", member.getId())
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
                            .replace("%user%", "<@" + member.getId() + ">")
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
                "ticket_close_" + member.getId(),
                ButtonStyle.valueOf(actions.getAsJsonObject().get("close").getAsJsonObject().get("style").getAsString())
        );

        CustomButton archiveButton = new CustomButton(
                actions.getAsJsonObject().get("archive").getAsJsonObject().get("text").getAsString(),
                "ticket_archive_" + member.getId(),
                ButtonStyle.valueOf(actions.getAsJsonObject().get("archive").getAsJsonObject().get("style").getAsString())
        );

        embed.setTitle(ticket.get("embed").getAsJsonObject().get("title").getAsString());
        embed.setDescription(description.toString());
        embed.setColor(color);
        embed.setFooter(
                ticket.get("embed").getAsJsonObject().get("footer").getAsString()
                        .replace("%id%", member.getId())
        );
        embed.setThumbnail(member.getEffectiveAvatarUrl());

        guild.createTextChannel(ticket.get("prefix").getAsString() + member.getUser().getName())
                .setParent(category)
                .addMemberPermissionOverride(
                        member.getIdLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND),
                        null
                )
                .addRolePermissionOverride(
                        guild.getPublicRole().getIdLong(),
                        null,
                        EnumSet.of(Permission.VIEW_CHANNEL)
                )
                .addRolePermissionOverride(
                        data.get("moderator-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .addRolePermissionOverride(
                        data.get("manager-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .addRolePermissionOverride(
                        data.get("admin-role").getAsLong(),
                        EnumSet.of(Permission.VIEW_CHANNEL),
                        null
                )
                .setTopic(topicBuilder.toString())
                .queue(
                        channel -> {

                            channel
                                    .sendMessage(ticket.get("embed").getAsJsonObject().get("ping").getAsString())
                                    .setEmbeds(embed.build())
                                    .addComponents(ActionRow.of(
                                            closeButton.getButton(),
                                            archiveButton.getButton()
                                    ))
                                    .flatMap(Message::pin)
                                    .queue();

                            event.getHook().sendMessage(ticket.get("created").getAsString().replace("%channel%", "<#" + channel.getId() + ">")).setEphemeral(true).queue();

                        }
                );

        return true;

    }

}
