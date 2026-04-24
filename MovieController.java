import java.sql.*;
import java.util.*;

public class MovieController {

    public void addMovie(Movie m) {
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR IGNORE INTO movies VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setInt(1, m.getId());
            stmt.setString(2, m.getTitle());
            stmt.setString(3, m.getYear());
            stmt.setString(4, m.getDirectors());
            stmt.setString(5, m.getActors());
            stmt.setString(6, m.getGenre());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Movie> getAllMovies() {
        List<Movie> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM movies ORDER BY title");
            while (rs.next()) {
                list.add(new Movie(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("year"),
                    rs.getString("directors"),
                    rs.getString("actors"),
                    rs.getString("genre")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("getAllMovies: returning " + list.size());
        return list;
    }

    // Mark as watched and save rating (0 = no rating)
    public void markWatched(Movie m, User user, int rating) {
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO watched(user_id, movie_id, rating) VALUES (?, ?, ?) " +
                "ON CONFLICT(user_id, movie_id) DO UPDATE SET rating=excluded.rating"
            );
            stmt.setInt(1, user.getId());
            stmt.setInt(2, m.getId());
            stmt.setInt(3, rating);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<Integer> getWatchedIds(User user) {
        Set<Integer> ids = new HashSet<>();
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT movie_id FROM watched WHERE user_id=?"
            );
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ids.add(rs.getInt("movie_id"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    public Map<Integer, Integer> getRatings(User user) {
        Map<Integer, Integer> ratings = new HashMap<>();
        try (Connection conn = DatabaseManager.connect()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT movie_id, rating FROM watched WHERE user_id=? AND rating > 0"
            );
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ratings.put(rs.getInt("movie_id"), rs.getInt("rating"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ratings;
    }

    // Smart recommendations based on ratings:
    // - Genres/actors from movies rated >= 4 are boosted
    // - Genres/actors from movies rated <= 2 are penalised
    // - Already watched movies are excluded
    public List<Movie> getRecommendations(User user) {
        List<Movie> recommended = new ArrayList<>();
        try (Connection conn = DatabaseManager.connect()) {

            Map<String, Double> genreScore = new HashMap<>();
            Map<String, Double> actorScore = new HashMap<>();
            Set<Integer> seenIds = new HashSet<>();

            // Pull watched movies with ratings
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT m.id, m.genre, m.actors, w.rating " +
                "FROM movies m JOIN watched w ON m.id = w.movie_id WHERE w.user_id=?"
            );
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                seenIds.add(rs.getInt("id"));
                int rating = rs.getInt("rating");
                // Weight: +2 per star above 3, -1 per star below 3
                double weight = rating == 0 ? 1.0 : (rating - 3) * 1.5 + 1.0;

                String genre = rs.getString("genre");
                if (genre != null) {
                    for (String g : genre.split("[,|]")) {
                        g = g.trim();
                        if (!g.isEmpty())
                            genreScore.merge(g, weight * 2, Double::sum);
                    }
                }
                String actors = rs.getString("actors");
                if (actors != null) {
                    for (String a : actors.split(",")) {
                        a = a.trim();
                        if (!a.isEmpty())
                            actorScore.merge(a, weight, Double::sum);
                    }
                }
            }

            if (genreScore.isEmpty() && actorScore.isEmpty()) return recommended;

            // Score every unwatched movie
            ResultSet all = conn.createStatement().executeQuery("SELECT * FROM movies");
            List<double[]> scored = new ArrayList<>(); // [score, id_as_double]
            List<Movie> movies = new ArrayList<>();

            while (all.next()) {
                int id = all.getInt("id");
                if (seenIds.contains(id)) continue;

                String g = all.getString("genre");
                String a = all.getString("actors");
                double score = 0;

                if (g != null) {
                    for (String key : genreScore.keySet())
                        if (g.contains(key)) score += genreScore.get(key);
                }
                if (a != null) {
                    for (String key : actorScore.keySet())
                        if (a.contains(key)) score += actorScore.get(key);
                }

                if (score > 0) {
                    scored.add(new double[]{score, id});
                    movies.add(new Movie(id, all.getString("title"), all.getString("year"),
                        all.getString("directors"), all.getString("actors"), g));
                }
            }

            // Sort by score descending
            scored.sort((x, y) -> Double.compare(y[0], x[0]));
            Map<Integer, Movie> movieMap = new HashMap<>();
            for (Movie mv : movies) movieMap.put(mv.getId(), mv);

            for (double[] entry : scored) {
                Movie mv = movieMap.get((int) entry[1]);
                if (mv != null) recommended.add(mv);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return recommended;
    }
}