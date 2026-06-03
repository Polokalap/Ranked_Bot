package mel.Polokalap.Bot.Listeners.Ticket;

import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.TicketUtil;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static mel.Polokalap.Bot.Main.lang;

public class OpenTicketCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();
        String name = ticket.get("name").getAsString();

        if (!event.getName().equals(name)) return;

        TicketUtil.createTicket(event);

    }

}
