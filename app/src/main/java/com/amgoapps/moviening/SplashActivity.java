package com.amgoapps.moviening;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.amgoapps.moviening.api.MovieResponse;
import com.amgoapps.moviening.api.TmdbClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad de inicio (Splash) que gestiona la carga inicial de datos y la
 * verificación del estado de autenticación del usuario.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";
    private static final long DURACION_MINIMA_SPLASH = 2000;

    private long tiempoInicio;

    /**
     * Inicializa la actividad y registra el tiempo de inicio para garantizar
     * una duración mínima de la pantalla de bienvenida.
     * @param savedInstanceState Estado de la instancia guardada.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tiempoInicio = System.currentTimeMillis();

        cargarDatosYDecidir();
    }

    /**
     * Realiza una petición a la API de TMDB para precargar las películas populares
     * en la memoria caché de la aplicación antes de navegar a la siguiente pantalla.
     */
    private void cargarDatosYDecidir() {
        TmdbClient.getApiService().getPopularMovies(API_KEY, LanguageUtils.getApiLanguage(), 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DataCache.popularMoviesCache = response.body().getResults();
                        }
                        navegarSiguientePantalla();
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {
                        navegarSiguientePantalla();
                    }
                });
    }

    /**
     * Gestiona la transición a la siguiente actividad (MainActivity o LoginActivity)
     * dependiendo de si existe una sesión activa de Firebase, respetando el tiempo
     * mínimo de exposición del Splash.
     */
    private void navegarSiguientePantalla() {
        long tiempoTranscurrido = System.currentTimeMillis() - tiempoInicio;
        long tiempoRestante = DURACION_MINIMA_SPLASH - tiempoTranscurrido;
        if (tiempoRestante < 0) tiempoRestante = 0;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;

            if (currentUser != null) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish();

        }, tiempoRestante);
    }
}