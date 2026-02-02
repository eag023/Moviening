package com.amgoapps.moviening;
import com.google.gson.annotations.SerializedName;

public class PersonDetailsResponse {
    @SerializedName("biography")
    private String biography;

    @SerializedName("place_of_birth")
    private String placeOfBirth;

    @SerializedName("profile_path")
    private String profilePath;

    @SerializedName("name")
    private String name;

    // Getters
    public String getBiography() { return biography; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public String getProfilePath() { return profilePath; }
    public String getName() { return name; }
}