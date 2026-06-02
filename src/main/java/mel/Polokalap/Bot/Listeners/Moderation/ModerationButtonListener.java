package mel.Polokalap.Bot.Listeners.Moderation;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

import static mel.Polokalap.Bot.Main.lang;

public class ModerationButtonListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        if (
                !event.getMember().hasPermission(Permission.KICK_MEMBERS) ||
                !event.getMember().hasPermission(Permission.BAN_MEMBERS)
        ) return;

        String id = event.getComponentId();
        JsonObject actions = lang
                .get("moderation").getAsJsonObject()
                .get("alert").getAsJsonObject()
                .get("actions").getAsJsonObject();

        if (id.startsWith("moderation_mute_user_")) {

            UserSnowflake user = UserSnowflake.fromId(id.replace("moderation_mute_user_", ""));

            event.getGuild().retrieveMember(user).queue(
                    member -> {

                    if (member.isTimedOut()) {

                        event.reply(actions.get("mute").getAsJsonObject().get("fail").getAsString()).setEphemeral(true).queue();
                        return;

                    }

                        event.getGuild().timeoutFor(user, 6, TimeUnit.HOURS).queue();
                        event.reply(
                                        actions.get("mute").getAsJsonObject().get("done").getAsString()
                                                .replace("%user%", "<@" + id.replace("moderation_mute_user_", "") + ">")
                                                .replace("%id%", id.replace("moderation_mute_user_", "")
                                                ))
                                .setEphemeral(true)
                                .queue();

                },
                    error -> {

                        event.reply(actions.get("user_left").getAsString()).setEphemeral(true).queue();
                        return;

                    }
            );

        }

        if (id.startsWith("moderation_kick_user_")) {

            UserSnowflake user = UserSnowflake.fromId(id.replace("moderation_kick_user_", ""));

            event.getGuild().retrieveMember(user).queue(
                    member -> {

                        if (member.isTimedOut()) {

                            event.reply(actions.get("mute").getAsJsonObject().get("fail").getAsString()).setEphemeral(true).queue();
                            return;

                        }

                        event.getGuild().kick(user).queue();
                        event.reply(
                                        actions.get("kick").getAsJsonObject().get("done").getAsString()
                                                .replace("%user%", "<@" + id.replace("moderation_kick_user_", "") + ">")
                                                .replace("%id%", id.replace("moderation_kick_user_", "")
                                                ))
                                .setEphemeral(true)
                                .queue();

                    },
                    error -> {

                        event.reply(actions.get("user_left").getAsString()).setEphemeral(true).queue();
                        return;

                    }

            );

        }

        if (id.startsWith("moderation_ban_user_")) {

            UserSnowflake user = UserSnowflake.fromId(id.replace("moderation_ban_user_", ""));

            event.getGuild().retrieveMember(user).queue(
                    member -> {

                        if (member.isTimedOut()) {

                            event.reply(actions.get("mute").getAsJsonObject().get("fail").getAsString()).setEphemeral(true).queue();
                            return;

                        }

                        event.getGuild().ban(user, 0, TimeUnit.MILLISECONDS).queue();
                        event.reply(
                                        actions.get("ban").getAsJsonObject().get("done").getAsString()
                                                .replace("%user%", "<@" + id.replace("moderation_ban_user_", "") + ">")
                                                .replace("%id%", id.replace("moderation_ban_user_", "")
                                                ))
                                .setEphemeral(true)
                                .queue();

                    },
                    error -> {

                        event.reply(actions.get("user_left").getAsString()).setEphemeral(true).queue();
                        return;

                    }

            );

        }

    }

}
