package com.amgoapps.moviening;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
 * Actividad que muestra las películas en las que ha participado una persona específica.
 * Permite filtrar la lista, añadir películas a listas de Firebase mediante pulsación larga
 * y gestionar listas personalizadas.
 */
public class PersonMoviesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private TextView txtTitle;
    private ImageButton btnFilter;

    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    private List<Movie> originalList = new ArrayList<>();
    private int currentFilterMode = 0;
    private int sortDirection = 0;

    /**
     * Inicializa la interfaz de usuario, configura el RecyclerView y gestiona la recepción
     * de datos del intent para cargar los créditos de la persona.
     * @param savedInstanceState Estado guardado de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_movies);

        try {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.light_gray));
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } catch (Exception e) { e.printStackTrace(); }

        int id = getIntent().getIntExtra("ID", -1);
        String name = getIntent().getStringExtra("NAME");
        String type = getIntent().getStringExtra("TYPE");

        if (id == -1) { finish(); return; }

        txtTitle = findViewById(R.id.txt_list_title);
        recyclerView = findViewById(R.id.recycler_person_movies);
        btnFilter = findViewById(R.id.btn_filter_person);

        if ("cast".equals(type)) {
            txtTitle.setText(name + " " + getString(R.string.l_acting));
        } else {
            txtTitle.setText(name + " " + getString(R.string.l_directing));
        }

        btnFilter.setOnClickListener(v -> mostrarMenuFiltros());

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new MovieAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new MovieAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Movie movie) {
                Intent intent = new Intent(PersonMoviesActivity.this, MovieDetailActivity.class);
                intent.putExtra("MOVIE_DATA", movie);
                startActivity(intent);
            }
        });

        adapter.setOnMovieLongClickListener(new MovieAdapter.OnMovieLongClickListener() {
            @Override
            public void onMovieLongClick(Movie movie, View view) {
                if ("person".equals(movie.getMediaType())) {
                    return;
                }
                mostrarDialogoOpcionesPelicula(movie, view);
            }
        });

        cargarPeliculas(id, type);
    }

    /**
     * Muestra un popup con acciones rápidas para una película (Favoritos, Vistos, Watchlist).
     * @param movie Objeto película seleccionado.
     * @param anchorView Vista que sirve de ancla para posicionar el popup.
     */
    private void mostrarDialogoOpcionesPelicula(Movie movie, View anchorView) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.dialog_movie_quick_actions, null);

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

        checkMovieInList(getString(R.string.favorite), movie.getId(), exists -> actualizarIcono(btnFav, exists, R.drawable.ic_heart, R.drawable.ic_heart_filled, R.color.dark_purple));
        checkMovieInList(getString(R.string.watched), movie.getId(), exists -> actualizarIcono(btnWatched, exists, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green));
        checkMovieInList(getString(R.string.watchlist), movie.getId(), exists -> actualizarIcono(btnWatchlist, exists, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));

        btnFav.setOnClickListener(v -> toggleMovieInList(getString(R.string.favorite), movie, btnFav, R.drawable.ic_heart, R.drawable.ic_heart_filled, R.color.dark_purple));
        btnWatched.setOnClickListener(v -> toggleMovieInList(getString(R.string.watched), movie, btnWatched, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green));
        btnWatchlist.setOnClickListener(v -> toggleMovieInList(getString(R.string.watchlist), movie, btnWatchlist, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));

        btnOther.setOnClickListener(v -> {
            popupWindow.dismiss();
            mostrarDialogoOtrasListas(movie);
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0] + 50, location[1] + 50);
    }

    /**
     * Verifica si una película existe dentro de una lista específica en Firebase.
     * @param listName Nombre de la lista.
     * @param movieId ID de la película.
     * @param listener Callback para devolver el resultado de la comprobación.
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

    /**
     * Interfaz para la comprobación asíncrona de existencia de elementos en Firebase.
     */
    interface OnCheckListener { void onCheck(boolean exists); }

    /**
     * Cambia visualmente el icono y el color de un botón de acción según su estado.
     * @param img ImageView a modificar.
     * @param filled Booleano que indica si el elemento está en la lista.
     * @param resNormal Recurso del icono vacío.
     * @param resFilled Recurso del icono relleno.
     * @param colorResId Color a aplicar cuando está relleno.
     */
    private void actualizarIcono(ImageView img, boolean filled, int resNormal, int resFilled, int colorResId) {
        if (filled) {
            img.setImageResource(resFilled);
            int resolvedColor = ContextCompat.getColor(this, colorResId);
            img.setColorFilter(resolvedColor, PorterDuff.Mode.SRC_IN);
        } else {
            img.setImageResource(resNormal);
            img.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            img.clearColorFilter();
        }
        img.setTag(filled);
    }

    /**
     * Alterna la presencia de una película en una lista de Firebase (añadir/quitar).
     * @param listName Nombre de la lista objetivo.
     * @param movie Objeto película.
     * @param img Vista del icono para actualizar su estado.
     * @param resNormal Recurso icono normal.
     * @param resFilled Recurso icono relleno.
     * @param colorResId Recurso de color para estado activo.
     */
    private void toggleMovieInList(String listName, Movie movie, ImageView img, int resNormal, int resFilled, int colorResId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference listRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(listName);
        DatabaseReference movieRef = listRef.child(String.valueOf(movie.getId()));

        boolean isCurrentlyAdded = (boolean) (img.getTag() != null ? img.getTag() : false);

        if (isCurrentlyAdded) {
            movieRef.removeValue().addOnSuccessListener(v -> {
                actualizarIcono(img, false, resNormal, resFilled, colorResId);
                listRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) listRef.setValue("created");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            });
        } else {
            movieRef.setValue(movie).addOnSuccessListener(v -> {
                actualizarIcono(img, true, resNormal, resFilled, colorResId);
            });
        }
    }

    /**
     * Muestra un diálogo para gestionar la pertenencia de una película a listas personalizadas.
     * @param movie Película a gestionar.
     */
    private void mostrarDialogoOtrasListas(Movie movie) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userListsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_custom_list, null);
        LinearLayout containerLists = dialogView.findViewById(R.id.container_custom_lists);
        EditText etNewList = dialogView.findViewById(R.id.et_new_list_quick);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        userListsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                containerLists.removeAllViews();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String listName = data.getKey();
                    if (listName != null &&
                            !listName.equals(getString(R.string.favorite)) &&
                            !listName.equals(getString(R.string.watched)) &&
                            !listName.equals(getString(R.string.watchlist))) {

                        agregarCheckBoxLista(containerLists, listName, movie, userListsRef);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        etNewList.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                crearListaDesdeInput(etNewList, containerLists, movie, userListsRef);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etNewList.getWindowToken(), 0);
                etNewList.clearFocus();
                return true;
            }
            return false;
        });

        etNewList.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                crearListaDesdeInput(etNewList, containerLists, movie, userListsRef);
            }
        });

        dialog.show();
    }

    /**
     * Crea una nueva lista en Firebase a partir del texto introducido en un EditText.
     * @param editText Campo de texto con el nombre de la lista.
     * @param container Contenedor visual donde añadir el nuevo CheckBox.
     * @param movie Película actual.
     * @param userListsRef Referencia a las listas del usuario en Firebase.
     */
    private void crearListaDesdeInput(EditText editText, LinearLayout container, Movie movie, DatabaseReference userListsRef) {
        String newListName = editText.getText().toString().trim();
        if (!newListName.isEmpty()) {
            userListsRef.child(newListName).setValue("created");
            agregarCheckBoxLista(container, newListName, movie, userListsRef);
            editText.setText("");
        }
    }

    /**
     * Añade un CheckBox al contenedor para representar una lista personalizada y gestiona su vinculación con Firebase.
     * @param container Vista padre.
     * @param listName Nombre de la lista.
     * @param movie Objeto película.
     * @param userListsRef Referencia de base de datos.
     */
    private void agregarCheckBoxLista(LinearLayout container, String listName, Movie movie, DatabaseReference userListsRef) {
        CheckBox checkBox = new CheckBox(this);
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

    /**
     * Despliega el menú de opciones para ordenar la lista actual.
     */
    private void mostrarMenuFiltros() {
        PopupMenu popup = new PopupMenu(this, btnFilter);
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
     * Construye el texto del menú de filtro incluyendo flechas de dirección según el estado actual.
     * @param titulo Nombre del criterio.
     * @param modeID ID del modo de filtro.
     * @return Título formateado con indicadores de ordenación.
     */
    private String getTextoFiltro(String titulo, int modeID) {
        if (currentFilterMode == modeID) {
            if (modeID == 1) {
                if (sortDirection == 1) return titulo + " ⬇";
                if (sortDirection == 2) return titulo + " ⬆";
            } else {
                if (sortDirection == 1) return titulo + " ⬆";
                if (sortDirection == 2) return titulo + " ⬇";
            }
        }
        return titulo;
    }

    /**
     * Aplica la lógica de selección de filtro y dirección de ordenación.
     * @param selectedMode Modo de ordenación seleccionado.
     */
    private void aplicarFiltro(int selectedMode) {
        if (currentFilterMode == selectedMode) {
            if (selectedMode == 1) {
                if (sortDirection == 1) sortDirection = 2;
                else { apagarFiltros(); return; }
            } else {
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

    /**
     * Desactiva todos los filtros y restaura la lista a su orden de carga original.
     */
    private void apagarFiltros() {
        sortDirection = 0;
        currentFilterMode = 0;
        restaurarOrdenOriginal();
    }

    /**
     * Ejecuta la ordenación de la lista basándose en los parámetros actuales y actualiza la UI.
     */
    private void ordenarListaActual() {
        List<Movie> listaActual = adapter.getMovies();
        if (listaActual == null || listaActual.isEmpty()) return;

        Collections.sort(listaActual, new Comparator<Movie>() {
            @Override
            public int compare(Movie m1, Movie m2) {
                int result = 0;
                switch (currentFilterMode) {
                    case 1:
                        String n1 = (m1.getTitle() != null) ? m1.getTitle() : "";
                        String n2 = (m2.getTitle() != null) ? m2.getTitle() : "";
                        result = n1.compareToIgnoreCase(n2);
                        break;
                    case 2:
                        String d1 = m1.getReleaseDate() != null ? m1.getReleaseDate() : "";
                        String d2 = m2.getReleaseDate() != null ? m2.getReleaseDate() : "";
                        result = d1.compareTo(d2);
                        break;
                    case 3:
                        result = Double.compare(m1.getTmdbRating(), m2.getTmdbRating());
                        break;
                }
                if (sortDirection == 2) result = -result;
                return result;
            }
        });
        adapter.notifyDataSetChanged();
    }

    /**
     * Reemplaza la lista del adaptador por la copia de seguridad original.
     */
    private void restaurarOrdenOriginal() {
        if (!originalList.isEmpty()) {
            adapter.setMovies(new ArrayList<>(originalList));
        }
    }

    /**
     * Crea una copia de seguridad de la lista recién cargada para permitir restauraciones tras filtrado.
     * @param lista Lista de películas original.
     */
    private void guardarBackup(List<Movie> lista) {
        originalList.clear();
        originalList.addAll(lista);
        currentFilterMode = 0;
        sortDirection = 0;
    }

    /**
     * Realiza la petición a la API para cargar las películas (reparto o dirección) del artista.
     * @param id ID de la persona en TMDB.
     * @param type Tipo de crédito.
     */
    private void cargarPeliculas(int id, String type) {
        TmdbClient.getApiService().getPersonCredits(String.valueOf(id), API_KEY, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<PersonCreditsResponse>() {
                    @Override
                    public void onResponse(Call<PersonCreditsResponse> call, Response<PersonCreditsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {

                            List<Movie> listaParaProcesar = new ArrayList<>();

                            if ("cast".equals(type)) {
                                if (response.body().getCast() != null) {
                                    listaParaProcesar.addAll(response.body().getCast());
                                }
                            } else {
                                if (response.body().getCrew() != null) {
                                    for (Movie m : response.body().getCrew()) {
                                        if ("Director".equals(m.getJob())) {
                                            listaParaProcesar.add(m);
                                        }
                                    }
                                }
                            }

                            List<Movie> listaFinal = eliminarDuplicados(listaParaProcesar);

                            if (listaFinal.isEmpty()) {
                                Toast.makeText(PersonMoviesActivity.this, "No hay películas", Toast.LENGTH_SHORT).show();
                            }

                            guardarBackup(listaFinal);
                            adapter.setMovies(listaFinal);
                        }
                    }

                    @Override
                    public void onFailure(Call<PersonCreditsResponse> call, Throwable t) {}
                });
    }

    /**
     * Filtra la lista eliminando películas duplicadas basándose en su ID único.
     * @param listaSucia Lista original que puede contener duplicados.
     * @return Lista filtrada con elementos únicos.
     */
    private List<Movie> eliminarDuplicados(List<Movie> listaSucia) {
        List<Movie> listaLimpia = new ArrayList<>();
        java.util.Set<Integer> idsVistos = new java.util.HashSet<>();
        if (listaSucia == null) return listaLimpia;

        for (Movie m : listaSucia) {
            if (!idsVistos.contains(m.getId())) {
                listaLimpia.add(m);
                idsVistos.add(m.getId());
            }
        }
        return listaLimpia;
    }
}