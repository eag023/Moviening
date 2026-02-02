package com.amgoapps.moviening;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Movie implements Serializable {

    @SerializedName("id")
    private int id;

    @SerializedName("media_type")
    private String mediaType;

    @SerializedName("title")
    private String title;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("vote_average")
    private double tmdbRating;

    @SerializedName("vote_count")
    private int voteCount;

    @SerializedName("name")
    private String name;

    @SerializedName("profile_path")
    private String profilePath;

    @SerializedName("gender")
    private int gender;

    @SerializedName("known_for_department")
    private String knownForDepartment;

    @SerializedName("job")
    private String job;

    @SerializedName("belongs_to_collection")
    private CollectionInfo belongsToCollection;

    @SerializedName("overview")
    private String overview;

    @SerializedName("genres")
    private List<Map<String, Object>> genres;

    private String character;

    private long timestampAdded;

    public Movie() {
    }

    public String getFullPosterUrl() {
        String path = null;
        if (posterPath != null && !posterPath.isEmpty()) {
            path = posterPath;
        } else if (profilePath != null && !profilePath.isEmpty()) {
            path = profilePath;
        }

        if (path == null) return null;
        if (path.startsWith("http")) return path;

        return "https://image.tmdb.org/t/p/w500" + path;
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMediaType() { return mediaType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public String getReleaseDate() { return releaseDate; }

    public double getTmdbRating() { return tmdbRating; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getGender() { return gender; }

    public String getKnownForDepartment() { return knownForDepartment; }

    public String getJob() { return job; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public String getCharacter() { return character; }

    public int getVoteCount() { return voteCount; }

    public long getTimestampAdded() { return timestampAdded; }

    public CollectionInfo getBelongsToCollection() { return belongsToCollection; }
    public void setBelongsToCollection(CollectionInfo belongsToCollection) { this.belongsToCollection = belongsToCollection; }

    public List<Map<String, Object>> getGenres() {
        return genres;
    }

    public void setGenres(List<Map<String, Object>> genres) {
        this.genres = genres;
    }

    public static class CollectionInfo implements Serializable {
        @SerializedName("id")
        public int id;
        @SerializedName("name")
        public String name;
    }

    public void setTimestampAdded(long timestampAdded) {
        this.timestampAdded = timestampAdded;
    }
}