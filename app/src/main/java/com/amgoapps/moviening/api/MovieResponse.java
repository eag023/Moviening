package com.amgoapps.moviening.api;
import com.amgoapps.moviening.Movie;
import java.util.List;

public class MovieResponse {
    private int page;
    private List<Movie> results;

    public List<Movie> getResults() {
        return results;
    }
}