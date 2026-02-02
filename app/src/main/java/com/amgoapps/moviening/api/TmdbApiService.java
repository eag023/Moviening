package com.amgoapps.moviening.api;

import com.amgoapps.moviening.CollectionResponse;
import com.amgoapps.moviening.Movie;
import com.amgoapps.moviening.PersonCreditsResponse;
import com.amgoapps.moviening.PersonDetailsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interfaz de servicio Retrofit para interactuar con The Movie Database API (TMDB).
 * Define los endpoints HTTP para recuperar películas, personas, créditos y colecciones.
 */
public interface TmdbApiService {

    /**
     * Realiza una búsqueda global que incluye películas, series de TV y personas.
     * Útil para barras de búsqueda generales.
     *
     * @param query El texto o término a buscar.
     */
    @GET("search/multi")
    Call<MovieResponse> searchMulti(
            @Query("api_key") String apiKey,
            @Query("query") String query,
            @Query("language") String language
    );

    /**
     * Obtiene una lista de películas populares actuales.
     * Los resultados se devuelven paginados.
     */
    @GET("movie/popular")
    Call<MovieResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    /**
     * Obtiene los créditos (filmografía) de una persona específica.
     * Devuelve las películas donde ha participado como actor o equipo técnico.
     *
     * @param personId ID único de la persona en TMDB.
     */
    @GET("person/{person_id}/movie_credits")
    Call<PersonCreditsResponse> getPersonCredits(
            @retrofit2.http.Path("person_id") String personId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    /**
     * Recupera la información detallada de una persona (biografía, fecha de nacimiento, etc.).
     */
    @GET("person/{person_id}")
    Call<PersonDetailsResponse> getPersonDetails(
            @retrofit2.http.Path("person_id") int personId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    /**
     * Obtiene el reparto (actores) y el equipo técnico (directores, guionistas) de una película.
     */
    @GET("movie/{movie_id}/credits")
    Call<MovieCreditsResponse> getMovieCredits(
            @retrofit2.http.Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    /**
     * Obtiene los detalles completos de una película específica (sinopsis, duración, presupuesto, etc.).
     */
    @GET("movie/{movie_id}")
    Call<Movie> getMovieDetails(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );

    /**
     * Descubrimiento avanzado de películas mediante filtros múltiples.
     * Permite filtrar por género, fechas, duración, votaciones, etc.
     *
     * Nota sobre sufijos:
     * - .gte (Greater Than or Equal)
     * - .lte (Less Than or Equal)
     */
    @GET("discover/movie")
    Call<MovieResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("sort_by") String sortBy,               // Ordenar por (ej: popularity.desc, release_date.asc)
            @Query("include_adult") boolean includeAdult,  // Para excluir contenido infantil
            @Query("include_video") boolean includeVideo,  // Incluir videos auxiliares
            @Query("page") int page,
            @Query("primary_release_date.lte") String releaseDateLte, // Fecha de estreno máxima
            @Query("primary_release_date.gte") String releaseDateGte, // Fecha de estreno mínima
            @Query("with_genres") String withGenres,       // IDs de géneros
            @Query("with_runtime.gte") Integer runtimeGte, // Duración mínima
            @Query("with_runtime.lte") Integer runtimeLte, // Duración máxima
            @Query("vote_average.gte") Double voteAverageGte, // Nota media mínima (0-10)
            @Query("vote_count.gte") Integer voteCountGte,    // Mínimo de votos requeridos
            @Query("with_origin_country") String originCountry, // Filtro por país de origen
            @Query("without_genres") String withoutGenres       // Excluir géneros específicos
    );

    /**
     * Obtiene los detalles de una saga de películas.
     */
    @GET("collection/{collection_id}")
    Call<CollectionResponse> getCollectionDetails(
            @Path("collection_id") int collectionId,
            @Query("api_key") String apiKey,
            @Query("language") String language
    );
}