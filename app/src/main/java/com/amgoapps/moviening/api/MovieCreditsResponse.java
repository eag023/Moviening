package com.amgoapps.moviening.api;
import com.amgoapps.moviening.Movie;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MovieCreditsResponse {
    @SerializedName("cast")
    private List<Movie> cast;

    @SerializedName("crew")
    private List<Movie> crew;

    public List<Movie> getCast() { return cast; }
    public List<Movie> getCrew() { return crew; }
}
