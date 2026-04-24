import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class TMDBService {

    private static final String API_KEY = "e02fc8cac9d2fd3890ab2c2a66363307";

    public static class MovieData {
        public BufferedImage image;
        public double rating;

        public MovieData(BufferedImage image, double rating) {
            this.image = image;
            this.rating = rating;
        }
    }

    public static MovieData fetchMovieData(String title) {
        try {
            String query = URLEncoder.encode(title.trim(), "UTF-8");

            String urlStr = "https://api.themoviedb.org/3/search/movie?api_key="
                    + API_KEY + "&query=" + query;

            URL url = new URL(urlStr);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String json = br.readLine();

            // poster
            int index = json.indexOf("\"poster_path\":\"");
            if (index == -1) return null;

            String path = json.substring(index + 15, json.indexOf("\"", index + 15));

            // rating
            int rIndex = json.indexOf("\"vote_average\":");
            double rating = Double.parseDouble(
                    json.substring(rIndex + 15, json.indexOf(",", rIndex))
            );

            String imageUrl = "https://image.tmdb.org/t/p/w200" + path;
            BufferedImage img = ImageIO.read(new URL(imageUrl));

            return new MovieData(img, rating);

        } catch (Exception e) {
            return null;
        }
    }
}