package mel.Polokalap.Bot.Listeners.Ticket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.EnumSet;

import static mel.Polokalap.Bot.Main.lang;

public class OpenTicketCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();
        String name = ticket.get("name").getAsString();
        Dotenv dotenv = Dotenv.load();

        if (!event.getName().equals(name)) return;

        Guild guild = event.getGuild();

        StringBuilder topicBuilder = new StringBuilder();

        for (JsonElement topicLine : ticket.get("topic").getAsJsonArray()) {

            topicBuilder.append(
                    topicLine.getAsString()
                            .replace("%username%", event.getMember().getEffectiveName())
                            .replace("%id%", event.getMember().getId())
                            + "\n"
            );

        }

        guild.createTextChannel(ticket.get("channel").getAsString().replace("%name%", event.getMember().getEffectiveName()))
                .setParent(guild.getCategoryById(dotenv.get("TICKET_CATEGORY")))
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

                    event.reply(ticket.get("created").getAsString().replace("%channel%", "<#" + channel.getId() + ">")).setEphemeral(true).queue();

                }
        );

    }

}
