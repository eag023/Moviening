package com.amgoapps.moviening; // O tu paquete correspondiente

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CollectionResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("parts")
    private List<Movie> parts;

    public List<Movie> getParts() {
        return parts;
    }
}