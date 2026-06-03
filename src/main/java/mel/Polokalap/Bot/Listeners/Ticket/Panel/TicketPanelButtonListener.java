package mel.Polokalap.Bot.Listeners.Ticket.Panel;

import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.TicketUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;

import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.data;
import static mel.Polokalap.Bot.Main.lang;

public class TicketPanelButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        String id = event.getComponentId();
        JsonObject ticket = lang.get("commands").getAsJsonObject().get("ticket").getAsJsonObject();

        if (id.equals("ticket_panel_button")) {

            TicketUtil.createTicket(event);

        }

    }

}
