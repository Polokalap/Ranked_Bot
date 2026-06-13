package mel.Polokalap.Bot;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.reflections.Reflections;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static mel.Polokalap.Bot.Utils.QueueUtil.queueMessage;

public class Main {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final Dotenv dotenv = Dotenv.load();
    public static JsonObject lang;
    public static JsonObject data;
    public static JDA jda;
    public static JsonArray gamemodes;
    public static ArrayList<String> gamemodeNames = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        String token = dotenv.get("BOT_TOKEN");
        String langJson;

        try {
            data = Main.load( "data.json");
            langJson = data.get("language").getAsString();
            lang = Main.load(langJson + ".json");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Reflections reflections = new Reflections("mel.Polokalap.Bot.Listeners");
        Set<Class<? extends net.dv8tion.jda.api.hooks.ListenerAdapter>> classes = reflections.getSubTypesOf(net.dv8tion.jda.api.hooks.ListenerAdapter.class);

        JDABuilder builder = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS);

        for (Class<? extends net.dv8tion.jda.api.hooks.ListenerAdapter> clazz : classes) {
            builder.addEventListeners(clazz.getDeclaredConstructor().newInstance());
        }

        jda = builder
                .setActivity(Activity.playing(data.get("status").getAsString()))
                .build();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            for (Message message : queueMessage.values()) message.delete().queue();

            if (jda != null) jda.shutdown();

        }));

        jda.awaitReady();

        List<CommandData> botCommands = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : lang.get("commands").getAsJsonObject().entrySet()) {

            JsonObject command = entry.getValue().getAsJsonObject();

            SlashCommandData slashCommand = Commands.slash(
                    command.get("name").getAsString(),
                    command.get("description").getAsString()
            );

            if (command.has("options")) {

                for (JsonElement element : command.get("options").getAsJsonArray()) {

                    JsonObject option = element.getAsJsonObject();

                    slashCommand.addOption(
                            OptionType.valueOf(option.get("type").getAsString()),
                            option.get("name").getAsString(),
                            option.get("description").getAsString(),
                            option.get("required").getAsBoolean()
                    );

                }

            }

            botCommands.add(slashCommand);

        }

        for (Guild guild : jda.getGuilds()) {

            guild.updateCommands().addCommands(botCommands).queue();

        }

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ranked.hu/v1/gamemodes"))
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        gamemodes = JsonParser.parseString(response.body()).getAsJsonArray();

        for (JsonElement gamemode : JsonParser.parseString(response.body()).getAsJsonArray()) {

            gamemodeNames.add(gamemode.getAsJsonObject().get("name").getAsString().toLowerCase());

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