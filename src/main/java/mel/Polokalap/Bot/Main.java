package mel.Polokalap.Bot;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.reflections.Reflections;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static JsonObject lang;
    public static long MOD_CHANNEL;
    public static JDA jda;

    public static void main() throws Exception {

        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("BOT_TOKEN");
        String langJson = dotenv.get("LANGUAGE");
        MOD_CHANNEL = Long.parseLong(dotenv.get("MOD_ALERT"));

        try {
            lang = Main.load(langJson + ".json");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Reflections reflections = new Reflections("mel.Polokalap.Bot.Listeners");
        Set<Class<? extends net.dv8tion.jda.api.hooks.ListenerAdapter>> classes = reflections.getSubTypesOf(net.dv8tion.jda.api.hooks.ListenerAdapter.class);

        JDABuilder builder = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT);

        for (Class<? extends net.dv8tion.jda.api.hooks.ListenerAdapter> clazz : classes) {
            builder.addEventListeners(clazz.getDeclaredConstructor().newInstance());
        }

        jda = builder.build();
        jda.awaitReady();

        List<CommandData> botCommands = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : lang.get("commands").getAsJsonObject().entrySet()) {

            JsonObject command = entry.getValue().getAsJsonObject();

            botCommands.add(

                    Commands.slash(
                            command.get("name").getAsString(),
                            command.get("description").getAsString()
                    )

            );

        }

        for (Guild guild : jda.getGuilds()) {

            guild.updateCommands().addCommands(botCommands).queue();

        }

    }

    public static JsonObject load(String path) throws Exception {

        var stream = Main.class.getClassLoader().getResourceAsStream(path);

        if (stream == null) {

            throw new RuntimeException("Lang file not found: " + path);

        }

        return JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();

    }

}