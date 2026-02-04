package com.amgoapps.moviening;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Actividad que visualiza una lista de películas guardadas por el usuario.
 * Permite filtrar, ordenar y eliminar elementos de la base de datos de Firebase.
 */
public class MovieListActivity extends AppCompatActivity {
    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    private RecyclerView recyclerView;
    private MovieAdapter adapter;
    private TextView txtTitle;
    private ImageButton btnFilter;
    private LinearLayout layoutEmptyState;
    private ProgressBar progressBar;

    private String listName;
    private List<Movie> originalList = new ArrayList<>();

    private DatabaseReference listRef;
    private ValueEventListener moviesListener;

    private int currentFilterMode = 0;
    private int sortDirection = 2;

    /**
     * Inicializa la actividad, configura la interfaz de usuario y los listeners de los botones.
     * @param savedInstanceState Estado de la instancia guardada.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        listName = getIntent().getStringExtra("LIST_NAME");
        String listTitle = getIntent().getStringExtra("LIST_TITLE");

        txtTitle = findViewById(R.id.txt_list_title);
        btnFilter = findViewById(R.id.btn_filter_list);
        recyclerView = findViewById(R.id.recycler_movie_list);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        progressBar = findViewById(R.id.progress_loading);

        if (listTitle != null && !listTitle.isEmpty()) {
            txtTitle.setText(listTitle);
        } else if (listName != null) {
            txtTitle.setText(listName);
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(35, 0, 40, 35);

        adapter = new MovieAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(movie -> {
            if ("person".equals(movie.getMediaType())) {
                Intent intent = new Intent(this, PersonDetailActivity.class);
                intent.putExtra("PERSON_ID", movie.getId());
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, MovieDetailActivity.class);
                intent.putExtra("MOVIE_DATA", movie);
                startActivity(intent);
            }
        });

        adapter.setOnMovieLongClickListener(this::mostrarPopupEliminar);

        btnFilter.setOnClickListener(v -> mostrarMenuFiltros());

        cargarPeliculasDeFirebase();
    }

    /**
     * Muestra un popup flotante para confirmar la eliminación de un elemento de la lista.
     * @param movie Objeto Movie que se desea eliminar.
     * @param anchorView Vista de referencia para posicionar el popup.
     */
    private void mostrarPopupEliminar(Movie movie, View anchorView) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_delete_item, null);
        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setElevation(20);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        popupView.findViewById(R.id.btn_delete_item).setOnClickListener(v -> {
            popupWindow.dismiss();
            eliminarPeliculaDeFirebase(movie);
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0] + (anchorView.getWidth() / 4),
                location[1] + (anchorView.getHeight() / 2));
    }

    /**
     * Elimina una película específica de la base de datos de Firebase del usuario.
     * @param movie Objeto Movie a eliminar.
     */
    private void eliminarPeliculaDeFirebase(Movie movie) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || listName == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("users")
                .child(uid).child("lists").child(listName).child(String.valueOf(movie.getId()));

        itemRef.removeValue().addOnSuccessListener(aVoid -> {
        });
    }

    /**
     * Configura la escucha en tiempo real de Firebase para cargar los datos de la lista actual.
     */
    private void cargarPeliculasDeFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || listName == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listRef = FirebaseDatabase.getInstance().getReference("users")
                .child(uid).child("lists").child(listName);

        progressBar.setVisibility(View.VISIBLE);

        moviesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                List<Movie> movies = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        if (data.hasChild("id")) {
                            Movie movie = data.getValue(Movie.class);
                            if (movie != null) movies.add(movie);
                        }
                    } catch (Exception e) { }
                }

                originalList.clear();
                originalList.addAll(movies);

                if (movies.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    btnFilter.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    layoutEmptyState.setVisibility(View.GONE);
                    btnFilter.setVisibility(View.VISIBLE);

                    ordenarLista();
                    actualizarIdiomaVisual();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };
        listRef.addValueEventListener(moviesListener);
    }

    /**
     * Limpia los recursos y remueve los listeners de Firebase para evitar fugas de memoria.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listRef != null && moviesListener != null) {
            listRef.removeEventListener(moviesListener);
        }
    }

    /**
     * Sincroniza los datos locales con la API de TMDB para asegurar que los títulos
     * y descripciones se muestren en el idioma actual del dispositivo.
     */
    private void actualizarIdiomaVisual() {
        String language = java.util.Locale.getDefault().getLanguage();
        String apiLanguage = language.equals("es") ? "es-ES" : "en-US";

        for (Movie movieLocal : originalList) {
            com.amgoapps.moviening.api.TmdbClient.getApiService().getMovieDetails(movieLocal.getId(), API_KEY, apiLanguage)
                    .enqueue(new retrofit2.Callback<Movie>() {
                        @Override
                        public void onResponse(retrofit2.Call<Movie> call, retrofit2.Response<Movie> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Movie movieApi = response.body();
                                movieLocal.setTitle(movieApi.getTitle());
                                movieLocal.setPosterPath(movieApi.getPosterPath());
                                movieLocal.setOverview(movieApi.getOverview());

                                int position = adapter.getMovies().indexOf(movieLocal);
                                if (position != -1) {
                                    adapter.notifyItemChanged(position);
                                }
                            }
                        }
                        @Override public void onFailure(retrofit2.Call<Movie> call, Throwable t) {}
                    });
        }
    }

    /**
     * Despliega un menú emergente con opciones de filtrado y ordenación.
     */
    private void mostrarMenuFiltros() {
        PopupMenu popup = new PopupMenu(this, btnFilter);
        popup.getMenu().add(0, 0, 0, getTextoFiltro(getString(R.string.date_added), 0));
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
     * Genera el texto del filtro incluyendo una flecha indicadora de dirección si está activo.
     * @param titulo Texto base del filtro.
     * @param modeID ID del modo de filtro.
     * @return El título formateado con el icono de dirección.
     */
    private String getTextoFiltro(String titulo, int modeID) {
        if (currentFilterMode == modeID) {
            if (sortDirection == 1) return titulo + " ⬆";
            if (sortDirection == 2) return titulo + " ⬇";
        }
        return titulo;
    }

    /**
     * Establece el modo de filtro seleccionado y gestiona la alternancia de dirección (asc/desc).
     * @param selectedMode El ID del modo de ordenación elegido.
     */
    private void aplicarFiltro(int selectedMode) {
        if (currentFilterMode == selectedMode) {
            sortDirection = (sortDirection == 1) ? 2 : 1;
        } else {
            currentFilterMode = selectedMode;
            sortDirection = 2;
        }
        ordenarLista();
    }

    /**
     * Ordena la lista de películas según el criterio y dirección seleccionados y actualiza el adaptador.
     */
    private void ordenarLista() {
        List<Movie> listaParaOrdenar = new ArrayList<>(originalList);
        Collections.sort(listaParaOrdenar, (m1, m2) -> {
            int result = 0;
            switch (currentFilterMode) {
                case 0:
                    result = Long.compare(m1.getTimestampAdded(), m2.getTimestampAdded());
                    break;
                case 1:
                    String n1 = (m1.getTitle() != null) ? m1.getTitle() : m1.getName();
                    String n2 = (m2.getTitle() != null) ? m2.getTitle() : m2.getName();
                    result = (n1 == null ? "" : n1).compareToIgnoreCase(n2 == null ? "" : n2);
                    break;
                case 2:
                    String d1 = m1.getReleaseDate() != null ? m1.getReleaseDate() : "0000";
                    String d2 = m2.getReleaseDate() != null ? m2.getReleaseDate() : "0000";
                    result = d1.compareTo(d2);
                    break;
                case 3:
                    result = Double.compare(m1.getTmdbRating(), m2.getTmdbRating());
                    break;
            }

            if (currentFilterMode == 1) return (sortDirection == 2) ? result : -result;
            return (sortDirection == 2) ? -result : result;
        });
        adapter.setMovies(listaParaOrdenar);
    }
}