import java.sql.*;

public class AuthController {

    public User login(String username, String password) {
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT * FROM users WHERE username=? AND password=?"
            );
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("username"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean userExists(String username) {
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM users WHERE username=?"
            );
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void signup(String username, String password) {
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users(username, password) VALUES (?, ?)"
            );
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("User already exists!");
        }
    }
}