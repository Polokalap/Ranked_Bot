package mel.Polokalap.Bot.Utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static mel.Polokalap.Bot.Main.dotenv;

public class Database {

    private static final HikariDataSource dataSource;
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    static {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + dotenv.get("DB_HOST") + ":" + dotenv.get("DB_PORT") + "/" + dotenv.get("DB_DATABASE"));
        config.setUsername(dotenv.get("DB_USER"));
        config.setPassword(dotenv.get("DB_PASSWORD"));
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);

    }

    public static CompletableFuture<Void> execute(String sql, Object... params) {

        return CompletableFuture.runAsync(() -> {

            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
                stmt.executeUpdate();
            } catch (SQLException e) {

                throw new RuntimeException(e);

            }

        }, executor);

    }

    public static <T> CompletableFuture<T> query(String sql, ResultSetMapper<T> mapper, Object... params) {

        return CompletableFuture.supplyAsync(() -> {

            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);

                try (ResultSet rs = stmt.executeQuery()) {

                    return mapper.map(rs);

                }

            } catch (SQLException e) {

                throw new RuntimeException(e);

            }

        }, executor);

    }

    @FunctionalInterface
    public interface ResultSetMapper<T> {

        T map(ResultSet rs) throws SQLException;

    }

}
