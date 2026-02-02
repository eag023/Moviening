package com.amgoapps.moviening;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PersonCreditsResponse {

    @SerializedName("cast")
    private List<Movie> cast;

    @SerializedName("crew")
    private List<Movie> crew;

    public List<Movie> getCast() {
        return cast;
    }

    public List<Movie> getCrew() {
        return crew;
    }
}