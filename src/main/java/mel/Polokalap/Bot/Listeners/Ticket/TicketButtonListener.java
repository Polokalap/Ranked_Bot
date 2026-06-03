package mel.Polokalap.Bot.Listeners.Ticket;

import com.google.gson.JsonObject;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;

import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.lang;

public class TicketButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject actions = lang
                .get("commands").getAsJsonObject()
                .get("ticket").getAsJsonObject()
                .get("embed").getAsJsonObject()
                .get("actions").getAsJsonObject();
        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();
        Dotenv dotenv = Dotenv.load();

        if (id.startsWith("ticket_close_")) {

            if (
                    event.getChannel().getName().replace(ticket.get("prefix").getAsString(), "").equals(event.getMember().getEffectiveName().toLowerCase()) ||
                    event.getMember().hasPermission(Permission.MESSAGE_MANAGE)
            ) {

                event.reply(actions.get("close").getAsJsonObject().get("done").getAsString()).queue(

                        interactionHook -> {

                            event.getChannel()
                                    .delete()
                                    .queueAfter(
                                            actions.get("close")
                                                    .getAsJsonObject()
                                                    .get("timeout").getAsInt(), TimeUnit.SECONDS
                                    );

                        }

                );
                return;

            }

            event.reply(actions.get("permission").getAsString()).setEphemeral(true).queue();

        }

        if (id.startsWith("ticket_archive_")) {

            if (event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {

                event.reply(actions.get("archive").getAsJsonObject().get("done").getAsString().replace("%user%", "<@" + event.getMember().getId() + ">")).queue(

                        interactionHook -> {

                            GuildChannel channel = (GuildChannel) event.getChannel();
                            String newName = event.getChannel().getName().replace(ticket.get("prefix").getAsString(), ticket.get("archived-prefix").getAsString());

                            ((TextChannelManager) channel.getManager())
                                    .setParent(event.getGuild().getCategoryById(dotenv.get("ARCHIVED_CATEGORY")))
                                    .setName(newName)
                                    .queue();

                            channel.getPermissionContainer().getPermissionOverrides().forEach(override -> {
                                override.delete().queue();
                            });

                            channel.getPermissionContainer()
                                    .upsertPermissionOverride(event.getGuild().getPublicRole())
                                    .deny(Permission.VIEW_CHANNEL)
                                    .queueAfter(
                                            actions.get("archive")
                                                    .getAsJsonObject()
                                                    .get("timeout").getAsInt(),
                                            TimeUnit.SECONDS
                                    );

                        }

                );
                return;

            }

            event.reply(actions.get("permission").getAsString()).setEphemeral(true).queue();

        }

    }

}
