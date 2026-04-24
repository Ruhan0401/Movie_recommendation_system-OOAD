public class Movie {
    private int id;
    private String title;
    private String year;
    private String directors;
    private String actors;
    private String genre;

    public Movie(int id, String title, String year, String directors, String actors, String genre) {
        this.id = id;
        this.title = title;
        this.year = year;
        this.directors = directors;
        this.actors = actors;
        this.genre = genre;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getYear() { return year; }
    public String getDirectors() { return directors; }
    public String getActors() { return actors; }
    public String getGenre() { return genre; }
}