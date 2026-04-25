import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:movies.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {

            // Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE, " +
                    "password TEXT)");

            // Movies table (UPDATED)
            stmt.execute("CREATE TABLE IF NOT EXISTS movies (" +
                    "id INTEGER PRIMARY KEY, " +
                    "title TEXT, " +
                    "year TEXT, " +
                    "directors TEXT, " +
                    "actors TEXT, " +
                    "genre TEXT)");

            // Watched table
            stmt.execute("CREATE TABLE IF NOT EXISTS watched (" +
                    "user_id INTEGER, " +
                    "movie_id INTEGER, " +
                    "rating INTEGER, " +
                    "PRIMARY KEY(user_id, movie_id))");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}