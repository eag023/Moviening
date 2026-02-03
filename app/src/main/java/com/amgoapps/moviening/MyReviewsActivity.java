package com.amgoapps.moviening;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad encargada de mostrar el listado de reseñas creadas por el usuario actual.
 * Gestiona la recuperación de datos desde Firebase, el filtrado, ordenación y actualización visual.
 */
public class MyReviewsActivity extends AppCompatActivity {
    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    private RecyclerView recyclerView;
    private MyReviewsAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ImageButton btnFilter;
    private TextView txtTitle;

    private DatabaseReference reviewsRef;
    private ValueEventListener reviewsListener;
    private String currentUid;

    private List<Review> originalList = new ArrayList<>();
    private int currentFilterMode = 0;
    private int sortDirection = 2;

    /**
     * Método llamado al crear la actividad.
     * Inicializa la interfaz de usuario, configura el RecyclerView y establece las referencias a Firebase.
     *
     * @param savedInstanceState Estado guardado de la instancia anterior, si existe.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);

        txtTitle = findViewById(R.id.txt_list_title);
        btnFilter = findViewById(R.id.btn_filter_list);
        progressBar = findViewById(R.id.progress_loading);
        emptyState = findViewById(R.id.layout_empty_state);
        recyclerView = findViewById(R.id.recycler_movie_list);

        txtTitle.setText(getString(R.string.reviews));
        btnFilter.setVisibility(View.VISIBLE);
        btnFilter.setOnClickListener(v -> mostrarMenuFiltros());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MyReviewsAdapter(this, originalList);
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        cargarMisResenas();
    }

    /**
     * Configura el listener de Firebase para cargar las reseñas del usuario en tiempo real.
     * Gestiona la visibilidad de la lista y el estado vacío, y actualiza los datos cuando hay cambios.
     */
    private void cargarMisResenas() {
        progressBar.setVisibility(View.VISIBLE);

        reviewsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Review> tempList = new ArrayList<>();

                for (DataSnapshot movieSnapshot : snapshot.getChildren()) {
                    if (movieSnapshot.hasChild(currentUid)) {
                        Review r = movieSnapshot.child(currentUid).getValue(Review.class);
                        if (r != null) {
                            tempList.add(r);
                        }
                    }
                }

                if (originalList.size() != tempList.size()) {
                    originalList.clear();
                    originalList.addAll(tempList);
                    ordenarLista();
                    adapter.notifyDataSetChanged();
                    actualizarIdiomaVisual();
                }

                progressBar.setVisibility(View.GONE);

                if (originalList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    if (btnFilter != null) btnFilter.setVisibility(View.GONE);

                    TextView txtEmpty = findViewById(R.id.txt_empty_message);
                    if (txtEmpty != null) {
                        txtEmpty.setText(R.string.no_my_reviews);
                    }
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (btnFilter != null) btnFilter.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };

        reviewsRef.addValueEventListener(reviewsListener);
    }

    /**
     * Método llamado al destruir la actividad.
     * Elimina el listener de Firebase para evitar fugas de memoria.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsRef != null && reviewsListener != null) {
            reviewsRef.removeEventListener(reviewsListener);
        }
    }

    /**
     * Muestra un menú emergente con las opciones de filtrado y ordenación disponibles.
     */
    private void mostrarMenuFiltros() {
        PopupMenu popup = new PopupMenu(this, btnFilter);
        popup.getMenu().add(0, 0, 0, getTextoFiltro(getString(R.string.date_added), 0));
        popup.getMenu().add(0, 1, 0, getTextoFiltro(getString(R.string.rate), 1));

        popup.setOnMenuItemClickListener(item -> {
            aplicarFiltro(item.getItemId());
            return true;
        });
        popup.show();
    }

    /**
     * Genera el texto para el elemento del menú de filtro, indicando la dirección de ordenación actual.
     *
     * @param titulo Título base del filtro.
     * @param modeID Identificador del modo de filtro.
     * @return Texto formateado con flechas indicadoras de orden.
     */
    private String getTextoFiltro(String titulo, int modeID) {
        if (currentFilterMode == modeID) {
            return titulo + (sortDirection == 1 ? " ⬆" : " ⬇");
        }
        return titulo;
    }

    /**
     * Aplica el filtro seleccionado y alterna la dirección de ordenación si ya estaba activo.
     *
     * @param selectedMode El modo de filtro seleccionado por el usuario.
     */
    private void aplicarFiltro(int selectedMode) {
        if (currentFilterMode == selectedMode) {
            sortDirection = (sortDirection == 1) ? 2 : 1;
        } else {
            currentFilterMode = selectedMode;
            sortDirection = 2;
        }
        ordenarLista();
        adapter.notifyDataSetChanged();
    }

    /**
     * Ordena la lista de reseñas basándose en el modo de filtro y la dirección actuales.
     */
    private void ordenarLista() {
        if (originalList == null || originalList.isEmpty()) return;

        Collections.sort(originalList, (r1, r2) -> {
            int result = 0;
            if (currentFilterMode == 0) {
                result = Long.compare(r1.getTimestamp(), r2.getTimestamp());
            } else if (currentFilterMode == 1) {
                result = Double.compare(r1.getRating(), r2.getRating());
            }
            return (sortDirection == 2) ? -result : result;
        });
    }

    /**
     * Actualiza los títulos y pósters de las películas consultando la API de TMDB
     * para asegurar que coincidan con el idioma actual del dispositivo.
     */
    private void actualizarIdiomaVisual() {
        String language = java.util.Locale.getDefault().getLanguage();
        String apiLanguage = language.equals("es") ? "es-ES" : "en-US";

        for (Review review : originalList) {
            TmdbClient.getApiService().getMovieDetails(review.getId(), API_KEY, apiLanguage)
                    .enqueue(new Callback<Movie>() {
                        @Override
                        public void onResponse(Call<Movie> call, Response<Movie> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                Movie apiData = response.body();
                                review.setTitle(apiData.getTitle());
                                review.setPosterPath(apiData.getPosterPath());
                                adapter.notifyDataSetChanged();
                            }
                        }
                        @Override public void onFailure(Call<Movie> call, Throwable t) {}
                    });
        }
    }
}