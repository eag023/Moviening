package com.amgoapps.moviening;

public class Review {
    private String userId;
    private String username;
    private String userPhotoUrl;
    private double rating;
    private String comment;
    private long timestamp;

    private String movieId;
    private String movieTitle;
    private String moviePoster;

    public Review() {
    }

    public Review(String userId, String username, String userPhotoUrl, double rating, String comment,
                  String movieId, String movieTitle, String moviePoster) {
        this.userId = userId;
        this.username = username;
        this.userPhotoUrl = userPhotoUrl;
        this.rating = rating;
        this.comment = comment;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.moviePoster = moviePoster;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters y Setters

    public String getUserId() { return userId; }

    public String getUsername() { return username; }

    public double getRating() { return rating; }

    public String getComment() { return comment; }

    public long getTimestamp() { return timestamp; }


    public int getId() {
        try {
            return Integer.parseInt(movieId);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getMovieId() { return movieId; }

    public String getMovieTitle() { return movieTitle; }

    public String getMoviePoster() { return moviePoster; }

    public void setTitle(String title) {
        this.movieTitle = title;
    }
    public void setPosterPath(String path) {
        this.moviePoster = path;
    }
}