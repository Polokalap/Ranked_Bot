package mel.Polokalap.Bot.Listeners.Queue.Ping;

import com.google.gson.JsonObject;
import mel.Polokalap.Bot.Utils.TicketUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import static mel.Polokalap.Bot.Main.*;

public class QueuePingSelectorButtonListener extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {

        JsonObject queue = lang.get("commands").getAsJsonObject().get("queue-ping").getAsJsonObject();

        String componentId = event.getComponentId();
        if (!componentId.equals("queue-selector")) return;

        String selected = event.getValues().get(0);
        int actualId = Integer.parseInt(selected.replace("queue-selector-", ""));

        String gamemodeName = gamemodes.get(actualId).getAsJsonObject().get("html").getAsString();
        String emoji = Emoji.fromCustom(
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("name").getAsString(),
                data.get("gamemodes").getAsJsonArray().get(actualId).getAsJsonObject().get("id").getAsLong(),
                false
        ).getAsMention();

        Member member = event.getMember();
        Guild guild = event.getGuild();
        long roleId = gamemodes.get(actualId).getAsJsonObject().get("role").getAsLong();
        Role role = guild.getRoleById(roleId);

        StringSelectMenu menuCopy = event.getSelectMenu().createCopy().build();
        event.deferEdit().queue();

        if (member.getRoles().toString().contains(String.valueOf(roleId))) {

            guild.removeRoleFromMember(member, role).queue();

            event.getHook().editOriginalComponents(ActionRow.of(menuCopy)).queue();
            event.getHook().sendMessage(
                    queue.get("role-revoked").getAsString()
                            .replace("%gamemode%", emoji + " " + gamemodeName)
            ).setEphemeral(true).queue();
            return;

        }

        guild.addRoleToMember(member, role).queue();

        event.getHook().editOriginalComponents(ActionRow.of(menuCopy)).queue();
        event.getHook().sendMessage(
                queue.get("role-given").getAsString()
                        .replace("%gamemode%", emoji + " " + gamemodeName)
        ).setEphemeral(true).queue();

    }

}
