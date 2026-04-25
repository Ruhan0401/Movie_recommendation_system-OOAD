import java.io.*;
import java.util.*;

public class CSVLoader {

    public static List<Movie> loadMovies(String filePath) {
        List<Movie> movies = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine(); // skip header
            if (line == null) {
                System.out.println("CSVLoader: file is empty!");
                return movies;
            }
            System.out.println("CSVLoader: header = " + line);

            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = splitCSV(line);
                    if (parts.length < 6) {
                        System.out.println("CSVLoader: skipping line " + lineNum + " (only " + parts.length + " cols): " + line.substring(0, Math.min(80, line.length())));
                        continue;
                    }
                    int id          = Integer.parseInt(parts[0].trim());
                    String title    = clean(parts[1]);
                    String year     = clean(parts[2]);
                    String directors= clean(parts[3]);
                    String actors   = clean(parts[4]);
                    String genres   = clean(parts[5]);

                    movies.add(new Movie(id, title, year, directors, actors, genres));
                } catch (Exception e) {
                    System.out.println("CSVLoader: error on line " + lineNum + ": " + e.getMessage());
                }
            }
            System.out.println("CSVLoader: loaded " + movies.size() + " movies from " + filePath);

        } catch (FileNotFoundException e) {
            System.out.println("CSVLoader: file not found: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return movies;
    }

    // Handles quoted fields with commas inside
    private static String[] splitCSV(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // handle escaped quotes ""
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString());
        return result.toArray(new String[0]);
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\""))
            s = s.substring(1, s.length() - 1).trim();
        return s;
    }
}