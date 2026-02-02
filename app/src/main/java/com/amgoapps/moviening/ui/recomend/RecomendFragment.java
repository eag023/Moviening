package com.amgoapps.moviening.ui.recomend;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.amgoapps.moviening.LanguageUtils;
import com.amgoapps.moviening.Movie;
import com.amgoapps.moviening.MovieDetailActivity;
import com.amgoapps.moviening.R;
import com.amgoapps.moviening.api.MovieCreditsResponse;
import com.amgoapps.moviening.api.MovieResponse;
import com.amgoapps.moviening.api.TmdbClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento principal del Motor de Recomendaciones.
 *
 * Funcionalidades principales:
 * 1. Pantalla de inicio.
 * 2. Asistente: Secuencia de preguntas para filtrar preferencias.
 * 3. Resultados: Carrusel de movimiento infinito con las películas encontradas (3 películas).
 * * Implementa lógica para búsquedas en la API y animaciones.
 */
public class RecomendFragment extends Fragment {

    private View rootContainer, startScreen, revealBg, wizardContainer;
    private ImageView ring1, ring2, ring3;
    private Button btnStart, btnRandom;

    private LinearLayout layoutMovieDay;
    private ImageView imgDayPoster;
    private TextView txtDayTitle, txtDayDirector, txtDayYear;
    private Movie movieOfTheDay;

    private androidx.constraintlayout.widget.ConstraintLayout layoutQuestionBox;
    private LinearLayout containerOptions, layoutResults, layoutSearching;
    private TextView txtQuestion;
    private ViewPager2 carouselPager;
    private Button btnFinish;

    private TextView txtResultsTitle, txtNoResults;

    // Variables de control de flujo
    private int currentQuestionIndex = 0;
    private boolean isRandomMode = false;
    private boolean isSearchCancelled = false;
    private boolean isAnimating = false;
    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    private static final long APP_START_TIME = System.currentTimeMillis();

    // Máximo de reintentos recursivos si la API devuelve películas sin póster o ya vistas
    private static final int MAX_RETRIES = 10;

    private List<Movie> accumulatedMovies = new ArrayList<>();

    // Variables de filtro para la consulta a la API
    private String selectedGenre = null;
    private int selectedRuntimeLte = 0;
    private int selectedRuntimeGte = 0;
    private String selectedCountry = null;
    private String selectedReleaseDateGte = null;
    private String selectedReleaseDateLte = null;
    private String withoutGenres = null;
    private Double selectedVoteAvg = null;
    private View viewFadeLeft, viewFadeRight;

    private List<Integer> watchedMovieIds = new ArrayList<>();

    /**
     * Inicializa la vista, las animaciones de fondo y los listeners.
     * Configura el comportamiento del botón "Atrás" para navegar entre estados internos del fragmento.
     */
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recomend, container, false);

        rootContainer = root.findViewById(R.id.root_container);
        startScreen = root.findViewById(R.id.layout_start_screen);
        revealBg = root.findViewById(R.id.view_reveal_bg);
        wizardContainer = root.findViewById(R.id.layout_wizard_container);
        ring1 = root.findViewById(R.id.ring_1);
        ring2 = root.findViewById(R.id.ring_2);
        ring3 = root.findViewById(R.id.ring_3);
        btnStart = root.findViewById(R.id.btn_start);
        btnRandom = root.findViewById(R.id.btn_random);

        layoutMovieDay = root.findViewById(R.id.layout_movie_day);
        imgDayPoster = root.findViewById(R.id.img_day_poster);
        txtDayTitle = root.findViewById(R.id.txt_day_title);
        txtDayDirector = root.findViewById(R.id.txt_day_director);
        txtDayYear = root.findViewById(R.id.txt_day_year);

        layoutQuestionBox = root.findViewById(R.id.layout_question_box);
        containerOptions = root.findViewById(R.id.container_options);
        layoutResults = root.findViewById(R.id.layout_results);
        layoutSearching = root.findViewById(R.id.layout_searching);
        txtQuestion = root.findViewById(R.id.txt_question);
        carouselPager = root.findViewById(R.id.viewpager_carousel);
        btnFinish = root.findViewById(R.id.btn_done);

        txtResultsTitle = root.findViewById(R.id.txt_results_title);
        txtNoResults = root.findViewById(R.id.txt_no_results);

        viewFadeLeft = root.findViewById(R.id.view_fade_left);
        viewFadeRight = root.findViewById(R.id.view_fade_right);

        animarCirculos();
        cargarPeliculaDelDia();
        obtenerIdsVistos();

        if (layoutQuestionBox.getLayoutTransition() != null) {
            layoutQuestionBox.getLayoutTransition().enableTransitionType(android.animation.LayoutTransition.CHANGING);
        }

        btnStart.setOnClickListener(v -> {
            if (!isAnimating) iniciarRecomendador(false);
        });
        btnRandom.setOnClickListener(v -> {
            if (!isAnimating) iniciarRecomendador(true);
        });

        layoutMovieDay.setOnClickListener(v -> {
            if (movieOfTheDay != null) abrirDetalle(movieOfTheDay);
        });

        btnFinish.setOnClickListener(v -> {
            if (!isAnimating) cerrarRecomendador();
        });

        // Callback para interceptar el botón físico "Atrás"
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAnimating) return;

                // Si estamos en la pantalla inicial, delegamos navegación al menú principal
                if (startScreen.getVisibility() == View.VISIBLE) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                            requireActivity().findViewById(R.id.nav_view);

                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_search);
                    }
                }
                // Si estamos dentro del proceso, volvemos a la pantalla inicial
                else {
                    cerrarRecomendador();
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refrescar lista de vistos por si hubo cambios en otras pantallas
        obtenerIdsVistos();
    }

    // ========================================================================
    // ANIMACIONES Y UI INICIAL
    // ========================================================================

    /**
     * Inicia la rotación infinita de los anillos decorativos.
     */
    private void animarCirculos() {
        rotarVista(ring1, 8000);
        rotarVista(ring2, 12000);
        rotarVista(ring3, 15000);
    }

    /**
     * Permite la rotación infinita de los anillos decorativos.
     */
    private void rotarVista(View view, long duration) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f);
        anim.setDuration(duration);
        anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.start();
        anim.setCurrentPlayTime((System.currentTimeMillis() - APP_START_TIME) % duration);
    }

    /**
     * Inicia la transición desde la pantalla de inicio al asistente.
     * * @param random define si es modo aleatorio (sin preguntas) o modo asistente.
     */
    private void iniciarRecomendador(boolean random) {
        if (isAnimating) return;
        obtenerIdsVistos();
        isSearchCancelled = false;

        isRandomMode = random;
        currentQuestionIndex = 0;
        resetearFiltros();
        accumulatedMovies.clear();

        // Configuración de color según el modo
        int color = random ? ContextCompat.getColor(getContext(), R.color.orange)
                : ContextCompat.getColor(getContext(), R.color.purple);
        revealBg.setBackgroundColor(color);

        // Calcular el centro de la animación basado en el botón pulsado
        View sourceBtn = random ? btnRandom : btnStart;
        int cx = (sourceBtn.getLeft() + sourceBtn.getRight()) / 2;
        int cy = (sourceBtn.getTop() + sourceBtn.getBottom()) / 2;
        float finalRadius = (float) Math.hypot(rootContainer.getWidth(), rootContainer.getHeight());

        Animator anim = ViewAnimationUtils.createCircularReveal(revealBg, cx, cy, 0f, finalRadius);
        anim.setDuration(600);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
                wizardContainer.setVisibility(View.VISIBLE);
                startScreen.setVisibility(View.INVISIBLE);

                if (isRandomMode) {
                    mostrarBusqueda();
                    buscarPeliculasEnAPI();
                } else {
                    mostrarSiguientePregunta();
                }
            }
        });

        revealBg.setVisibility(View.VISIBLE);
        anim.start();
    }

    /**
     * Revierte la animación para volver a la pantalla de inicio.
     */
    private void cerrarRecomendador() {
        if (isAnimating) return;
        // Detener cualquier búsqueda en curso
        isSearchCancelled = true;

        int cx, cy;

        // Calcular coordenadas de origen para la animación inversa
        if (btnFinish.isShown()) {
            int[] locationButton = new int[2];
            btnFinish.getLocationInWindow(locationButton);
            int[] locationRoot = new int[2];
            rootContainer.getLocationInWindow(locationRoot);

            cx = locationButton[0] - locationRoot[0] + btnFinish.getWidth() / 2;
            cy = locationButton[1] - locationRoot[1] + btnFinish.getHeight() / 2;
        } else {
            cx = rootContainer.getWidth() / 2;
            cy = rootContainer.getHeight() / 2;
        }

        float finalRadius = (float) Math.hypot(rootContainer.getWidth(), rootContainer.getHeight());

        // Obtener color del tema actual para la transición limpia
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
        int colorFondoOriginal = typedValue.data;

        startScreen.setBackgroundColor(colorFondoOriginal);
        startScreen.bringToFront();
        startScreen.setVisibility(View.VISIBLE);

        Animator anim = ViewAnimationUtils.createCircularReveal(startScreen, cx, cy, 0f, finalRadius);
        anim.setDuration(600);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Resetear visibilidad y estados internos al terminar la animación
                wizardContainer.setVisibility(View.INVISIBLE);
                wizardContainer.setTranslationY(0f);
                revealBg.setVisibility(View.INVISIBLE);

                layoutResults.setVisibility(View.GONE);
                layoutSearching.setVisibility(View.GONE);
                layoutSearching.setAlpha(1f);
                layoutQuestionBox.setVisibility(View.VISIBLE);

                // Reordenar vistas en el contenedor
                if (rootContainer instanceof ViewGroup) {
                    ((ViewGroup) rootContainer).removeView(startScreen);
                    ((ViewGroup) rootContainer).addView(startScreen, 0);
                }
                isAnimating = false;
            }
        });

        anim.start();
    }

    // ========================================================================
    // LÓGICA DE PREGUNTAS
    // ========================================================================

    /**
     * Muestra la pregunta correspondiente al índice actual o inicia la búsqueda si no hay más.
     */
    private void mostrarSiguientePregunta() {
        containerOptions.removeAllViews();

        View parent = (View) containerOptions.getParent();
        if (parent instanceof ScrollView) {
            ((ScrollView) parent).scrollTo(0, 0);
        }

        layoutQuestionBox.setAlpha(0f);
        layoutQuestionBox.animate().alpha(1f).setDuration(500).start();

        switch (currentQuestionIndex) {
            case 0: configPreguntaGenero(); break;
            case 1: configPreguntaDuracion(); break;
            case 2: configPreguntaPais(); break;
            case 3: configPreguntaEpoca(); break;
            case 4: configPreguntaInfantil(); break;
            case 5: configPreguntaValoracion(); break;
            default:
                mostrarBusqueda();
                buscarPeliculasEnAPI();
                break;
        }
    }

    private void opcionSeleccionada() {
        currentQuestionIndex++;
        mostrarSiguientePregunta();
    }

    /**
     * Crea y añade dinámicamente un botón de opción al contenedor.
     * Incluye una pequeña animación de entrada en cascada.
     */
    private void agregarBotonOpcion(String texto, Runnable accion) {
        Button btn = new Button(getContext());
        btn.setText(texto);
        btn.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.white));
        btn.setTextColor(ContextCompat.getColor(getContext(), R.color.purple));
        btn.setTextSize(16f);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        btn.setLayoutParams(params);

        btn.setAlpha(0f);
        btn.setTranslationY(50f);
        btn.animate().alpha(1f).translationY(0f).setDuration(300).setStartDelay(containerOptions.getChildCount() * 50).start();

        btn.setOnClickListener(v -> {
            accion.run();
            opcionSeleccionada();
        });
        containerOptions.addView(btn);
    }

    // Configuración de cada paso del Recomendador

    private void configPreguntaGenero() {
        txtQuestion.setText(getString(R.string.genre));
        agregarBotonOpcion(getString(R.string.action), () -> selectedGenre = "28");
        agregarBotonOpcion(getString(R.string.adventure), () -> selectedGenre = "12");
        agregarBotonOpcion(getString(R.string.sci_fi), () -> selectedGenre = "878");
        agregarBotonOpcion(getString(R.string.fantasy), () -> selectedGenre = "14");
        agregarBotonOpcion(getString(R.string.horror), () -> selectedGenre = "27");
        agregarBotonOpcion(getString(R.string.comedy), () -> selectedGenre = "35");
        agregarBotonOpcion(getString(R.string.mistery), () -> selectedGenre = "9648");
        agregarBotonOpcion(getString(R.string.animation), () -> selectedGenre = "16");
        agregarBotonOpcion(getString(R.string.musical), () -> selectedGenre = "10402");
        agregarBotonOpcion(getString(R.string.drama), () -> selectedGenre = "18");
        agregarBotonOpcion(getString(R.string.romance), () -> selectedGenre = "10749");
        agregarBotonOpcion(getString(R.string.random_genre), () -> selectedGenre = null);
    }

    private void configPreguntaDuracion() {
        txtQuestion.setText(getString(R.string.duration));
        agregarBotonOpcion(getString(R.string.short_duration), () -> selectedRuntimeLte = 90);
        agregarBotonOpcion(getString(R.string.normal_duration), () -> selectedRuntimeGte = 90);
        agregarBotonOpcion(getString(R.string.random_duration), () -> {});
    }

    private void configPreguntaPais() {
        txtQuestion.setText(getString(R.string.country));
        agregarBotonOpcion(getString(R.string.spain), () -> selectedCountry = "ES");
        agregarBotonOpcion(getString(R.string.usa), () -> selectedCountry = "US");
        agregarBotonOpcion(getString(R.string.japan), () -> selectedCountry = "JP");
        agregarBotonOpcion(getString(R.string.corea), () -> selectedCountry = "KR");
        agregarBotonOpcion(getString(R.string.uk), () -> selectedCountry = "GB");
        agregarBotonOpcion(getString(R.string.france), () -> selectedCountry = "FR");
        agregarBotonOpcion(getString(R.string.other_country), () -> selectedCountry = "IT,DE,CN");
        agregarBotonOpcion(getString(R.string.random_country), () -> selectedCountry = null);
    }

    private void configPreguntaEpoca() {
        txtQuestion.setText(getString(R.string.decade));
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = fmt.format(cal.getTime());

        agregarBotonOpcion(getString(R.string.newest), () -> {
            selectedReleaseDateLte = today;
            cal.add(Calendar.MONTH, -6);
            selectedReleaseDateGte = fmt.format(cal.getTime());
        });

        agregarBotonOpcion(getString(R.string.some_new), () -> {
            selectedReleaseDateLte = today;
            cal.add(Calendar.YEAR, -5);
            selectedReleaseDateGte = fmt.format(cal.getTime());
        });

        agregarBotonOpcion(getString(R.string.some_old), () -> {
            cal.add(Calendar.YEAR, -5);
            selectedReleaseDateLte = fmt.format(cal.getTime());
            cal.add(Calendar.YEAR, -20);
            selectedReleaseDateGte = fmt.format(cal.getTime());
        });

        agregarBotonOpcion(getString(R.string.oldest), () -> {
            cal.add(Calendar.YEAR, -25);
            selectedReleaseDateLte = fmt.format(cal.getTime());
            selectedReleaseDateGte = null;
        });

        agregarBotonOpcion(getString(R.string.random_decade), () -> {
            selectedReleaseDateGte = null;
            selectedReleaseDateLte = null;
        });
    }

    private void configPreguntaInfantil() {
        txtQuestion.setText(getString(R.string.children));
        agregarBotonOpcion(getString(R.string.yes_child), () -> selectedGenre = (selectedGenre == null ? "10751" : selectedGenre + ",10751"));
        agregarBotonOpcion(getString(R.string.no_child), () -> withoutGenres = "10751,16");
        agregarBotonOpcion(getString(R.string.random_children), () -> {});
    }

    private void configPreguntaValoracion() {
        txtQuestion.setText(getString(R.string.rate_question));
        agregarBotonOpcion(getString(R.string.best_rate), () -> selectedVoteAvg = 7.0);
        agregarBotonOpcion(getString(R.string.normal_rate), () -> selectedVoteAvg = 5.0);
        agregarBotonOpcion(getString(R.string.random_rate), () -> {});
    }

    private void resetearFiltros() {
        selectedGenre = null; selectedRuntimeLte = 0; selectedRuntimeGte = 0;
        selectedCountry = null; selectedReleaseDateGte = null; selectedReleaseDateLte = null;
        withoutGenres = null; selectedVoteAvg = null;
    }

    // ========================================================================
    // API Y RESULTADOS
    // ========================================================================

    private void mostrarBusqueda() {
        layoutQuestionBox.setVisibility(View.GONE);
        layoutSearching.setVisibility(View.VISIBLE);
        layoutSearching.setAlpha(0f);
        layoutSearching.animate().alpha(1f).setDuration(500).start();
    }

    private void buscarPeliculasEnAPI() {
        accumulatedMovies.clear();
        realizarBusquedaRecursiva(1);
    }

    /**
     * Realiza la llamada a la API de TMDB.
     * Utiliza recursividad para manejar reintentos si las películas obtenidas no son válidas
     * (ej: sin póster, ya vistas, sin descripción).
     * * @param attempt Número de intento actual.
     */
    private void realizarBusquedaRecursiva(int attempt) {
        if (selectedReleaseDateLte == null) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            selectedReleaseDateLte = fmt.format(new Date());
        }

        // Filtros por defecto para asegurar calidad mínima
        String finalWithoutGenres = (withoutGenres == null || withoutGenres.isEmpty()) ? "99" : withoutGenres + ",99";
        Integer finalRuntimeGte = (selectedRuntimeGte == 0) ? 45 : selectedRuntimeGte;
        Integer finalRuntimeLte = (selectedRuntimeLte == 0) ? null : selectedRuntimeLte;

        // Selección de página
        int pageToRequest;
        if (isRandomMode) {
            pageToRequest = new Random().nextInt(50) + 1;
        } else {
            pageToRequest = attempt;
        }

        TmdbClient.getApiService().discoverMovies(
                API_KEY, LanguageUtils.getApiLanguage(),
                isRandomMode ? null : "popularity.desc",
                false, false,
                pageToRequest,
                selectedReleaseDateLte, selectedReleaseDateGte,
                selectedGenre,
                finalRuntimeGte,
                finalRuntimeLte,
                selectedVoteAvg,
                100,
                selectedCountry,
                finalWithoutGenres
        ).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (isSearchCancelled) return;

                if (response.isSuccessful() && response.body() != null) {
                    procesarResultadosYReintentar(response.body().getResults(), attempt);
                } else {
                    checkAndRetry(attempt);
                }
            }
            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                if (isSearchCancelled) return;

                if (accumulatedMovies.isEmpty()) {
                    mostrarPantallaError();
                } else {
                    checkAndRetry(attempt);
                }
            }
        });
    }

    private void mostrarPantallaError() {
        layoutSearching.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            layoutSearching.setVisibility(View.GONE);
            layoutResults.setVisibility(View.VISIBLE);
            layoutResults.setAlpha(0f);
            layoutResults.animate().alpha(1f).setDuration(500).start();
            txtResultsTitle.setText(getString(R.string.oops));
            carouselPager.setVisibility(View.GONE);
            txtNoResults.setVisibility(View.VISIBLE);
            txtNoResults.setText(getString(R.string.no_results));
        }).start();
    }

    /**
     * Filtra los resultados de la API y decide si buscar más.
     */
    private void procesarResultadosYReintentar(List<Movie> candidates, int attempt) {
        if (isSearchCancelled) return;
        Collections.shuffle(candidates);

        for (Movie m : candidates) {
            if (esPeliculaValida(m)) {
                boolean yaEsta = false;
                for (Movie acc : accumulatedMovies) {
                    if (acc.getId() == m.getId()) {
                        yaEsta = true;
                        break;
                    }
                }
                if (!yaEsta) {
                    accumulatedMovies.add(m);
                }
                // Si ya tenemos 3 buenas opciones, mostramos resultados
                if (accumulatedMovies.size() >= 3) {
                    List<Movie> safeCopy = new ArrayList<>(accumulatedMovies.subList(0, 3));
                    mostrarResultadosCarrusel(safeCopy);
                    return;
                }
            }
        }
        checkAndRetry(attempt);
    }

    private boolean esPeliculaValida(Movie m) {
        return m.getPosterPath() != null && !m.getPosterPath().isEmpty() &&
                !watchedMovieIds.contains(m.getId()) &&
                m.getOverview() != null && !m.getOverview().trim().isEmpty();
    }

    /**
     * Controla el límite de intentos.
     */
    private void checkAndRetry(int attempt) {
        if (isSearchCancelled) return;
        if (attempt < MAX_RETRIES) {
            realizarBusquedaRecursiva(attempt + 1);
        } else {
            if (!accumulatedMovies.isEmpty()) {
                List<Movie> safeCopy = new ArrayList<>(accumulatedMovies);
                mostrarResultadosCarrusel(safeCopy);
            } else {
                mostrarPantallaError();
            }
        }
    }

    // ========================================================================
    // PELÍCULA DEL DÍA Y UTILIDADES
    // ========================================================================

    /**
     * Gestiona la "Película del Día".
     * Utiliza SharedPreferences para persistir la elección durante 24 horas.
     */
    private void cargarPeliculaDelDia() {
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("MovieAppPrefs", Context.MODE_PRIVATE);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String todayDate = fmt.format(new Date());

        String savedDate = prefs.getString("DAILY_MOVIE_DATE", "");
        int savedMovieId = prefs.getInt("DAILY_MOVIE_ID", -1);

        if (todayDate.equals(savedDate) && savedMovieId != -1) {
            cargarDetallesPeliDia(savedMovieId);
        } else {
            buscarNuevaPeliAleatoriaParaHoy(prefs, todayDate);
        }
    }

    private void buscarNuevaPeliAleatoriaParaHoy(SharedPreferences prefs, String todayDate) {
        int randomPage = new Random().nextInt(500) + 1;
        TmdbClient.getApiService().discoverMovies(
                API_KEY, LanguageUtils.getApiLanguage(), null, false, false, randomPage, null, null, null, null, null, null,
                100, null, "99"
        ).enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> results = response.body().getResults();
                    List<Movie> validCandidates = new ArrayList<>();
                    for (Movie m : results) {
                        if (m.getPosterPath() != null && !m.getPosterPath().isEmpty() &&
                                m.getOverview() != null && !m.getOverview().trim().isEmpty()) {
                            validCandidates.add(m);
                        }
                    }
                    if (!validCandidates.isEmpty()) {
                        Movie selected = validCandidates.get(new Random().nextInt(validCandidates.size()));
                        prefs.edit()
                                .putString("DAILY_MOVIE_DATE", todayDate)
                                .putInt("DAILY_MOVIE_ID", selected.getId())
                                .apply();
                        movieOfTheDay = selected;
                        actualizarUIPeliDia();
                    } else {
                        buscarNuevaPeliAleatoriaParaHoy(prefs, todayDate);
                    }
                }
            }
            @Override public void onFailure(Call<MovieResponse> call, Throwable t) {}
        });
    }

    private void cargarDetallesPeliDia(int movieId) {
        TmdbClient.getApiService().getMovieDetails(movieId, API_KEY, LanguageUtils.getApiLanguage()).enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(Call<Movie> call, Response<Movie> response) {
                if (response.isSuccessful() && response.body() != null) {
                    movieOfTheDay = response.body();
                    actualizarUIPeliDia();
                }
            }
            @Override public void onFailure(Call<Movie> call, Throwable t) {}
        });
    }

    private void actualizarUIPeliDia() {
        if (movieOfTheDay == null || getContext() == null) return;
        Glide.with(this).load(movieOfTheDay.getFullPosterUrl()).into(imgDayPoster);
        txtDayTitle.setText(movieOfTheDay.getTitle());
        if (movieOfTheDay.getReleaseDate() != null && movieOfTheDay.getReleaseDate().length() >= 4)
            txtDayYear.setText(movieOfTheDay.getReleaseDate().substring(0, 4));
        else
            txtDayYear.setText("----");

        TmdbClient.getApiService().getMovieCredits(movieOfTheDay.getId(), API_KEY, LanguageUtils.getApiLanguage()).enqueue(new Callback<MovieCreditsResponse>() {
            @Override
            public void onResponse(Call<MovieCreditsResponse> call, Response<MovieCreditsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    txtDayDirector.setText(getString(R.string.unknown));
                    for (Movie crew : response.body().getCrew()) {
                        if ("Director".equals(crew.getJob())) {
                            txtDayDirector.setText(crew.getName());
                            break;
                        }
                    }
                }
            }
            @Override public void onFailure(Call<MovieCreditsResponse> call, Throwable t) {}
        });
    }

    /**
     * Obtiene de Firebase la lista de películas ya vistas por el usuario
     * para excluirlas de las recomendaciones.
     */
    private void obtenerIdsVistos() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(getString(R.string.watched))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        watchedMovieIds.clear();
                        for (DataSnapshot s : snapshot.getChildren()) {
                            try {
                                int id = Integer.parseInt(s.getKey());
                                watchedMovieIds.add(id);
                            } catch (Exception e) {
                                // Ignorar claves no numéricas
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void abrirDetalle(Movie m) {
        Intent intent = new Intent(getContext(), MovieDetailActivity.class);
        intent.putExtra("MOVIE_DATA", m);
        startActivity(intent);
    }

    // ========================================================================
    // MÉTODOS DE RESULTADOS Y ADAPTADOR INFINITO
    // ========================================================================

    /**
     * Configura y muestra el carrusel de resultados.
     * Aplica profundidad a los elementos y configura el scroll infini
     */
    private void mostrarResultadosCarrusel(List<Movie> movies) {
        // 1. Animación de transición UI
        layoutSearching.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            layoutSearching.setVisibility(View.GONE);
            layoutResults.setVisibility(View.VISIBLE);
            layoutResults.setAlpha(0f);
            layoutResults.animate().alpha(1f).setDuration(500).start();

            if (txtResultsTitle != null) txtResultsTitle.setText(getString(R.string.results));
            if (carouselPager != null) carouselPager.setVisibility(View.VISIBLE);
            if (txtNoResults != null) txtNoResults.setVisibility(View.GONE);
        }).start();

        // Configuración de colores temáticos (Al azar vs Recomendador)
        int colorResId = isRandomMode ? R.color.orange : R.color.purple;
        int color = ContextCompat.getColor(getContext(), colorResId);

        if (viewFadeLeft != null) viewFadeLeft.setBackgroundTintList(ColorStateList.valueOf(color));
        if (viewFadeRight != null) viewFadeRight.setBackgroundTintList(ColorStateList.valueOf(color));

        // 2. Configuración del Adaptador
        CarouselAdapter adapter = new CarouselAdapter(movies, carouselPager);
        carouselPager.setAdapter(adapter);

        carouselPager.setOffscreenPageLimit(3);
        carouselPager.setClipToPadding(false);
        carouselPager.setClipChildren(false);
        carouselPager.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        // 3. Efecto de profundidad
        CompositePageTransformer transformer = new CompositePageTransformer();
        transformer.addTransformer((page, position) -> {
            float absPos = Math.abs(position);

            // Escala (reduce tamaño al alejarse del centro)
            float scale = 1.0f - (absPos * 0.15f);
            if (scale < 0.5f) scale = 0.5f;
            page.setScaleY(scale);
            page.setScaleX(scale);

            // Transparencia
            float alpha = 1.0f - (absPos * 0.5f);
            if (alpha < 0.5f) alpha = 0.5f;
            page.setAlpha(alpha);

            // Profundidad (eje Z)
            page.setTranslationZ(10f - absPos);

            // Solapamiento horizontal
            float overlap = 150 * absPos;
            if (position > 0) page.setTranslationX(-overlap);
            else page.setTranslationX(overlap);

            // Título visible solo en la central
            View title = page.findViewById(R.id.txt_movie_title);
            if (title != null) {
                float titleAlpha = 1f - (absPos * 2.0f);
                if (titleAlpha < 0f) titleAlpha = 0f;
                title.setAlpha(titleAlpha);
            }
        });
        carouselPager.setPageTransformer(transformer);

        // 4. Posicionamiento inicial en el "centro" del scroll infinito
        int startPosition = (Integer.MAX_VALUE / 2) - ((Integer.MAX_VALUE / 2) % movies.size());
        carouselPager.setCurrentItem(startPosition, false);
    }

    // --- Otros métodos ---


    /**
     * Adaptador para el carrusel de películas.
     * Simula una lista infinita devolviendo un ItemCount.
     */
    class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CViewHolder> {
        List<Movie> movies;
        ViewPager2 viewPager;

        CarouselAdapter(List<Movie> m, ViewPager2 vp) {
            this.movies = m;
            this.viewPager = vp;
        }

        @NonNull
        @Override
        public CViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carousel_movie, parent, false);
            return new CViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CViewHolder holder, int position) {
            if (movies.isEmpty()) return;
            // Mapeo de la posición "infinita" a la posición real de la lista
            int realPosition = position % movies.size();
            Movie m = movies.get(realPosition);

            holder.title.setText(m.getTitle());
            Glide.with(getContext()).load(m.getFullPosterUrl()).into(holder.poster);

            holder.itemView.setOnClickListener(v -> {
                if (viewPager.getCurrentItem() == position) {
                    abrirDetalle(m);
                } else {
                    viewPager.setCurrentItem(position, true);
                }
            });
        }

        @Override
        public int getItemCount() {
            // Devuelve un número enorme para permitir scroll "infinito"
            return (movies == null || movies.isEmpty()) ? 0 : Integer.MAX_VALUE;
        }

        class CViewHolder extends RecyclerView.ViewHolder {
            ImageView poster;
            TextView title;
            CViewHolder(View v) {
                super(v);
                poster = v.findViewById(R.id.img_movie_poster);
                title = v.findViewById(R.id.txt_movie_title);
            }
        }
    }
}