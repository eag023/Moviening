package com.amgoapps.moviening.ui.search;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amgoapps.moviening.LanguageUtils;
import com.amgoapps.moviening.Movie;
import com.amgoapps.moviening.MovieAdapter;
import com.amgoapps.moviening.MovieDetailActivity;
import com.amgoapps.moviening.PersonDetailActivity;
import com.amgoapps.moviening.R;
import com.amgoapps.moviening.api.MovieResponse;
import com.amgoapps.moviening.api.TmdbClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragmento encargado de la búsqueda de contenido (Películas y Personas).
 * Funcionalidades principales:
 * 1. Búsqueda en tiempo real (con cierto retraso para optimizar llamadas a API).
 * 2. Visualización de resultados en Grid.
 * 3. Menú de acciones rápidas (Long Press) para añadir a listas de Firebase.
 * 4. Filtrado y ordenación local de los resultados mostrados.
 */
public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private SearchView searchView;
    private TextView txtHeader;
    private ImageButton btnFilter;

    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    // Variables para gestionar el filtrado y ordenación local sin volver a llamar a la API
    private List<Movie> originalList = new ArrayList<>();
    private int currentFilterMode = 0; // 0: Ninguno, 1: Nombre, 2: Año, 3: Valoración
    private int sortDirection = 0;     // 1: Ascendente, 2: Descendente

    // Manejador para el retardo de búsqueda (Debounce)
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    /**
     * Inicializa la vista, configura los adaptadores y los listeners de búsqueda.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_search, container, false);

        searchView = root.findViewById(R.id.search_view);
        recyclerView = root.findViewById(R.id.recycler_search);
        txtHeader = root.findViewById(R.id.txt_header_search);
        btnFilter = root.findViewById(R.id.btn_filter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new MovieAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Configuración de Clic simple: Navegación a detalles
        adapter.setOnItemClickListener(new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Movie movie) {
                // Distingue entre persona y película para abrir la actividad correcta
                if ("person".equals(movie.getMediaType())) {
                    Intent intent = new Intent(getContext(), PersonDetailActivity.class);
                    intent.putExtra("PERSON_ID", movie.getId());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getContext(), MovieDetailActivity.class);
                    intent.putExtra("MOVIE_DATA", movie);
                    startActivity(intent);
                }
            }
        });

        // Configuración de Long Clic: Menú rápido de listas
        adapter.setOnMovieLongClickListener(new MovieAdapter.OnMovieLongClickListener() {
            @Override
            public void onMovieLongClick(Movie movie, View view) {
                // Las personas no se pueden añadir a listas de visualización
                if ("person".equals(movie.getMediaType())) {
                    return;
                }
                mostrarDialogoOpcionesPelicula(movie, view);
            }
        });

        btnFilter.setOnClickListener(v -> mostrarMenuFiltros());

        // Carga inicial de contenido
        cargarPeliculasPopulares();

        // Configuración del buscador con patrón Debounce
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Cancela la búsqueda anterior si el usuario sigue escribiendo
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                // Si borra el texto, volver a populares
                if (newText.trim().isEmpty()) {
                    cargarPeliculasPopulares();
                    return true;
                }
                // Programa una nueva búsqueda con 500ms de retraso
                searchRunnable = () -> buscarPeliculas(newText);
                searchHandler.postDelayed(searchRunnable, 500);
                return true;
            }
        });

        // Gestión del botón "Atrás" del sistema
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si hay texto en el buscador, lo limpiamos primero
                if (searchView != null && searchView.getQuery().length() > 0) {
                    searchView.setQuery("", false);
                    searchView.clearFocus();
                }
                // Si ya estaba vacío, cerramos la actividad/app
                else {
                    requireActivity().finish();
                }
            }
        });

        return root;
    }

    /**
     * Limpia el texto del buscador y quita el foco.
     * Método público accesible desde la Activity contenedora.
     */
    public void limpiarBuscador() {
        if (searchView != null) {
            if (searchView.getQuery().length() > 0) {
                searchView.setQuery("", false);
                searchView.clearFocus();
            }
        }
    }

    /**
     * Fuerza la aparición del teclado y pone el foco en la barra de búsqueda.
     */
    public void activarTeclado() {
        if (searchView != null) {
            searchView.setIconified(false);
            searchView.requestFocusFromTouch();

            // Pequeño retraso para asegurar que la vista esté lista antes de pedir el teclado al sistema
            searchView.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    // ========================================================================
    // LÓGICA DE MENÚ RÁPIDO (LONG PRESS) Y FIREBASE
    // ========================================================================

    /**
     * Muestra un Popup flotante sobre la película seleccionada.
     * Permite añadir/quitar rápidamente de Favoritas, Vistas, Pendientes y de listas personalizadas.
     */
    private void mostrarDialogoOpcionesPelicula(Movie movie, View anchorView) {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_movie_quick_actions, null);

        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(20);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView btnFav = popupView.findViewById(R.id.btn_action_fav);
        ImageView btnWatched = popupView.findViewById(R.id.btn_action_watched);
        ImageView btnWatchlist = popupView.findViewById(R.id.btn_action_watchlist);
        TextView btnOther = popupView.findViewById(R.id.btn_other_lists);
        TextView txtTitle = popupView.findViewById(R.id.txt_movie_title);

        txtTitle.setText(movie.getTitle() != null ? movie.getTitle() : movie.getName());

        TooltipCompat.setTooltipText(btnFav, getString(R.string.favorites));
        TooltipCompat.setTooltipText(btnWatched, getString(R.string.watched));
        TooltipCompat.setTooltipText(btnWatchlist, getString(R.string.watchlist));

        // Comprobación asíncrona del estado inicial en Firebase para colorear iconos
        checkMovieInList(getString(R.string.favorites), movie.getId(), exists -> actualizarIcono(btnFav, exists, R.drawable.ic_heart, R.drawable.ic_heart_filled, R.color.dark_purple));
        checkMovieInList(getString(R.string.watched), movie.getId(), exists -> actualizarIcono(btnWatched, exists, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green));
        checkMovieInList(getString(R.string.watchlist), movie.getId(), exists -> actualizarIcono(btnWatchlist, exists, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));

        // Listeners para modificar el estado al hacer clic
        btnFav.setOnClickListener(v -> toggleMovieInList(getString(R.string.favorites), movie, btnFav, R.drawable.ic_heart, R.drawable.ic_heart_filled, R.color.dark_purple));
        btnWatched.setOnClickListener(v -> toggleMovieInList(getString(R.string.watched), movie, btnWatched, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green));
        btnWatchlist.setOnClickListener(v -> toggleMovieInList(getString(R.string.watchlist), movie, btnWatchlist, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));

        btnOther.setOnClickListener(v -> {
            popupWindow.dismiss();
            mostrarDialogoOtrasListas(movie);
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        // Mostrar popup ligeramente desplazado respecto al elemento tocado
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0] + 50, location[1] + 50);
    }

    // --- MÉTODOS AUXILIARES FIREBASE ---

    /**
     * Consulta única a Firebase para ver si una película existe en una lista.
     */
    private void checkMovieInList(String listName, int movieId, OnCheckListener listener) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(listName).child(String.valueOf(movieId));
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listener.onCheck(snapshot.exists());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    interface OnCheckListener { void onCheck(boolean exists); }

    /**
     * Actualiza visualmente el icono (relleno/vacío) y su color.
     */
    private void actualizarIcono(ImageView img, boolean filled, int resNormal, int resFilled, int colorResId) {
        if (filled) {
            img.setImageResource(resFilled);
            if (getContext() != null) {
                int resolvedColor = ContextCompat.getColor(getContext(), colorResId);
                img.setColorFilter(resolvedColor, PorterDuff.Mode.SRC_IN);
            }
        } else {
            img.setImageResource(resNormal);
            img.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            img.clearColorFilter();
        }
        img.setTag(filled); // Guardamos el estado en el tag
    }

    /**
     * Añade o elimina la película de Firebase y actualiza la interfaz.
     * Si la lista se queda vacía, se mantiene la clave padre con un valor placeholder.
     */
    private void toggleMovieInList(String listName, Movie movie, ImageView img, int resNormal, int resFilled, int colorResId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference listRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(listName);
        DatabaseReference movieRef = listRef.child(String.valueOf(movie.getId()));

        boolean isCurrentlyAdded = (boolean) (img.getTag() != null ? img.getTag() : false);

        if (isCurrentlyAdded) {
            // Eliminar
            movieRef.removeValue().addOnSuccessListener(v -> {
                actualizarIcono(img, false, resNormal, resFilled, colorResId);
                // Evitar que Firebase borre el nodo padre si no quedan hijos
                listRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) listRef.setValue("created");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            });
        } else {
            // Añadir
            movieRef.setValue(movie).addOnSuccessListener(v -> {
                actualizarIcono(img, true, resNormal, resFilled, colorResId);
            });
        }
    }

    // ========================================================================
    // DIÁLOGO OTRAS LISTAS CON CREACIÓN RÁPIDA
    // ========================================================================

    /**
     * Muestra un diálogo con todas las listas personalizadas del usuario.
     * Permite crear nuevas listas dinámicamente y asignar la película mediante CheckBoxes.
     */
    private void mostrarDialogoOtrasListas(Movie movie) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userListsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_to_custom_list, null);
        LinearLayout containerLists = dialogView.findViewById(R.id.container_custom_lists);
        EditText etNewList = dialogView.findViewById(R.id.et_new_list_quick);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        // Cargar listas existentes y generar CheckBoxes
        userListsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                containerLists.removeAllViews();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String listName = data.getKey();
                    // Excluir listas del sistema
                    if (listName != null &&
                            !listName.equals(getString(R.string.favorites)) &&
                            !listName.equals(getString(R.string.watched)) &&
                            !listName.equals(getString(R.string.watchlist))) {

                        agregarCheckBoxLista(containerLists, listName, movie, userListsRef);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Lógica para crear lista al pulsar "intro" en el teclado
        etNewList.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                crearListaDesdeInput(etNewList, containerLists, movie, userListsRef);

                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etNewList.getWindowToken(), 0);
                etNewList.clearFocus();
                return true;
            }
            return false;
        });

        // Lógica para crear lista al perder el foco del input
        etNewList.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                crearListaDesdeInput(etNewList, containerLists, movie, userListsRef);
            }
        });

        dialog.show();
    }

    private void crearListaDesdeInput(EditText editText, LinearLayout container, Movie movie, DatabaseReference userListsRef) {
        String newListName = editText.getText().toString().trim();
        if (!newListName.isEmpty()) {
            userListsRef.child(newListName).setValue("created");
            agregarCheckBoxLista(container, newListName, movie, userListsRef);
            editText.setText("");
        }
    }

    /**
     * Genera dinámicamente un CheckBox para una lista.
     * Gestiona la sincronización bidireccional (Check -> Añadir a BD, Uncheck -> Borrar de BD).
     */
    private void agregarCheckBoxLista(LinearLayout container, String listName, Movie movie, DatabaseReference userListsRef) {
        CheckBox checkBox = new CheckBox(getContext());
        checkBox.setText(listName);
        checkBox.setTextSize(16);
        checkBox.setPadding(16, 16, 16, 16);

        userListsRef.child(listName).child(String.valueOf(movie.getId())).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                checkBox.setOnCheckedChangeListener(null);
                checkBox.setChecked(snapshot.exists());

                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    DatabaseReference listRef = userListsRef.child(listName);
                    DatabaseReference movieRef = listRef.child(String.valueOf(movie.getId()));

                    if (isChecked) {
                        movieRef.setValue(movie);
                    } else {
                        movieRef.removeValue().addOnSuccessListener(aVoid -> {
                            listRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot s) {
                                    if (!s.exists()) listRef.setValue("created");
                                }
                                @Override public void onCancelled(@NonNull DatabaseError e) {}
                            });
                        });
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        container.addView(checkBox);
    }

    // ========================================================================
    // FILTROS Y ORDENACIÓN LOCAL
    // ========================================================================

    /**
     * Muestra los filtros en el menú.
     */
    private void mostrarMenuFiltros() {
        PopupMenu popup = new PopupMenu(getContext(), btnFilter);
        popup.getMenu().add(0, 1, 0, getTextoFiltro(getString(R.string.name), 1));
        popup.getMenu().add(0, 2, 0, getTextoFiltro(getString(R.string.year), 2));
        popup.getMenu().add(0, 3, 0, getTextoFiltro(getString(R.string.rate), 3));

        popup.setOnMenuItemClickListener(item -> {
            aplicarFiltro(item.getItemId());
            return true;
        });
        popup.show();
    }

    /**
     * Genera el texto del menú indicando si hay ordenación activa (flechas).
     */
    private String getTextoFiltro(String titulo, int modeID) {
        if (currentFilterMode == modeID) {
            if (modeID == 1) { // Nombre (A-Z)
                if (sortDirection == 1) return titulo + " ⬇";
                if (sortDirection == 2) return titulo + " ⬆";
            } else { // Año y Rating (Mayor a Menor)
                if (sortDirection == 1) return titulo + " ⬆";
                if (sortDirection == 2) return titulo + " ⬇";
            }
        }
        return titulo;
    }

    /**
     * Aplica la lógica de tres estados: Ascendente -> Descendente -> Apagado.
     */
    private void aplicarFiltro(int selectedMode) {
        if (currentFilterMode == selectedMode) {
            if (selectedMode == 1) { // Nombre
                if (sortDirection == 1) sortDirection = 2;
                else { apagarFiltros(); return; }
            } else { // Año y Rating
                if (sortDirection == 2) sortDirection = 1;
                else { apagarFiltros(); return; }
            }
        } else {
            currentFilterMode = selectedMode;
            if (selectedMode == 1) sortDirection = 1;
            else sortDirection = 2;
        }
        ordenarListaActual();
    }

    private void apagarFiltros() {
        sortDirection = 0;
        currentFilterMode = 0;
        restaurarOrdenOriginal();
    }

    /**
     * Ordena la lista visible.
     */
    private void ordenarListaActual() {
        List<Movie> listaActual = adapter.getMovies();
        if (listaActual == null || listaActual.isEmpty()) return;

        Collections.sort(listaActual, new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                int result = 0;
                switch (currentFilterMode) {
                    case 1: // Nombre
                        String n1 = (m1.getTitle() != null) ? m1.getTitle() : m1.getName();
                        String n2 = (m2.getTitle() != null) ? m2.getTitle() : m2.getName();
                        if (n1 == null) n1 = ""; if (n2 == null) n2 = "";
                        result = n1.compareToIgnoreCase(n2);
                        break;
                    case 2: // Año
                        String d1 = m1.getReleaseDate() != null ? m1.getReleaseDate() : "";
                        String d2 = m2.getReleaseDate() != null ? m2.getReleaseDate() : "";
                        result = d1.compareTo(d2);
                        break;
                    case 3: // Rating
                        result = Double.compare(m1.getTmdbRating(), m2.getTmdbRating());
                        break;
                }
                if (sortDirection == 2) result = -result;
                return result;
            }
        });
        adapter.notifyDataSetChanged();
    }

    private void restaurarOrdenOriginal() {
        if (!originalList.isEmpty()) {
            adapter.setMovies(new ArrayList<>(originalList));
        }
    }

    /**
     * Guarda una copia de la lista obtenida de la API para poder restaurar el orden original.
     */
    private void guardarBackup(List<Movie> lista) {
        originalList.clear();
        originalList.addAll(lista);
        currentFilterMode = 0;
        sortDirection = 0;
    }

    // ========================================================================
    // LLAMADAS API
    // ========================================================================

    private void cargarPeliculasPopulares() {
        if (txtHeader != null) {
            txtHeader.setText(R.string.popular);
            txtHeader.setVisibility(View.VISIBLE);
        }

        TmdbClient.getApiService().getPopularMovies(API_KEY, LanguageUtils.getApiLanguage(), 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> listaOriginal = response.body().getResults();

                            // Filtrado inicial de calidad
                            List<Movie> listaFiltrada = filtrarPeliculasDeCalidad(listaOriginal);

                            guardarBackup(listaFiltrada);
                            adapter.setMovies(listaFiltrada);
                        }
                    }
                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {}
                });
    }

    private void buscarPeliculas(String query) {
        if (txtHeader != null) txtHeader.setVisibility(View.GONE);

        TmdbClient.getApiService().searchMulti(API_KEY, query, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> resultados = response.body().getResults();
                            if (resultados.isEmpty()) return;

                            Movie mejorCoincidencia = resultados.get(0);

                            // Si el mejor resultado es una persona, mostrar solo esa persona
                            if ("person".equals(mejorCoincidencia.getMediaType())) {
                                List<Movie> personaUnica = new ArrayList<>();
                                personaUnica.add(mejorCoincidencia);
                                guardarBackup(personaUnica);
                                adapter.setMovies(personaUnica);
                            } else {
                                // Si no, filtrar y mostrar películas
                                List<Movie> pelisLimpias = new ArrayList<>();
                                for (Movie m : resultados) {
                                    if ("movie".equals(m.getMediaType()) || m.getMediaType() == null) {
                                        pelisLimpias.add(m);
                                    }
                                }
                                List<Movie> pelisUnicas = eliminarDuplicados(pelisLimpias);
                                guardarBackup(pelisUnicas);
                                adapter.setMovies(pelisUnicas);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {}
                });
    }

    /**
     * Elimina duplicados de la lista de resultados usando un Set de IDs.
     */
    private List<Movie> eliminarDuplicados(List<Movie> listaSucia) {
        List<Movie> listaLimpia = new ArrayList<>();
        java.util.Set<Integer> idsVistos = new java.util.HashSet<>();
        for (Movie m : listaSucia) {
            if (!idsVistos.contains(m.getId())) {
                listaLimpia.add(m);
                idsVistos.add(m.getId());
            }
        }
        return listaLimpia;
    }

    /**
     * Aplica un filtro de calidad eliminando películas sin sinopsis o con pocos votos.
     */
    private List<Movie> filtrarPeliculasDeCalidad(List<Movie> listaSucia) {
        List<Movie> listaLimpia = new ArrayList<>();
        for (Movie m : listaSucia) {
            // Validación de sinopsis válida
            boolean tieneSinopsis = m.getOverview() != null && !m.getOverview().trim().isEmpty();

            // Validación de popularidad mínima (más de 100 votos)
            boolean tieneVotosSuficientes = m.getVoteCount() > 100;

            if (tieneSinopsis && tieneVotosSuficientes) {
                listaLimpia.add(m);
            }
        }
        return listaLimpia;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null && getArguments().getBoolean("PONER_CURSOR", false)) {
            ponerCursorEnBarra();
            getArguments().putBoolean("PONER_CURSOR", false);
        }
    }

    /**
     * Expande la barra de búsqueda y coloca el cursor en el campo de texto.
     */
    public void ponerCursorEnBarra() {
        if (searchView != null) {
            searchView.setIconified(false);

            EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
            if (searchEditText != null) {
                searchEditText.requestFocus();

                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }
    }
}