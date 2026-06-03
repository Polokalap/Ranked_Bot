package mel.Polokalap.Bot.Listeners.Ticket;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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


        if (id.startsWith("ticket_close_")) {

            if (
                    event.getChannel().getName().replace(ticket.get("prefix").getAsString(), "").equals(event.getMember().getEffectiveName().toLowerCase()) ||
                    event.getMember().hasPermission(Permission.MESSAGE_MANAGE)
            ) {

                event.reply(actions.get("close").getAsJsonObject().get("done").getAsString()).queue(

                        interactionHook -> {

                            event.getChannel().delete().queueAfter(actions.get("close").getAsJsonObject().get("timeout").getAsInt(), TimeUnit.SECONDS);

                        }

                );
                return;

            }

            event.reply(actions.get("permission").getAsString()).setEphemeral(true).queue();

        }

    }

}
