package mel.Polokalap.Bot.Listeners.Add;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import static mel.Polokalap.Bot.Main.data;
import static mel.Polokalap.Bot.Main.lang;

public class AddCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

        JsonObject commands = lang.get("commands").getAsJsonObject();
        JsonObject profile = commands.get("add").getAsJsonObject();
        String name = profile.get("name").getAsString();
        Guild guild = event.getGuild();
        Member member = event.getMember();
        Role managerRole = guild.getRoleById(data.get("manager-role").getAsLong());

        if (!event.getName().equals(name)) return;

        if (!member.getRoles().contains(managerRole)) {

            event.reply(profile.get("permission").getAsString()).setEphemeral(true).queue();
            return;

        }

        OptionMapping playerOption = event.getOption("user");
        Member user = playerOption != null ? playerOption.getAsMember() : null;

        TextChannel channel = event.getChannel().asTextChannel();

        channel.upsertPermissionOverride(user)
                .grant(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                .queue();

        event.reply(
                profile.get("added").getAsString()
                        .replace("%player%", user.getAsMention())
        ).setEphemeral(true).queue();

    }

}
