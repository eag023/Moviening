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

public class MyReviewsActivity extends AppCompatActivity {
    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    private RecyclerView recyclerView;
    private MyReviewsAdapter adapter;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private ImageButton btnFilter;
    private TextView txtTitle;

    private DatabaseReference reviewsRef;
    private ValueEventListener reviewsListener; // Listener global
    private String currentUid;

    private List<Review> originalList = new ArrayList<>();
    private int currentFilterMode = 0;
    private int sortDirection = 2;

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
        adapter = new MyReviewsAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews");

        cargarMisResenas();
    }

    private void cargarMisResenas() {
        progressBar.setVisibility(View.VISIBLE);

        // CAMBIO: Usamos addValueEventListener para actualizaciones en tiempo real
        reviewsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalList.clear();

                for (DataSnapshot movieSnapshot : snapshot.getChildren()) {
                    if (movieSnapshot.hasChild(currentUid)) {
                        Review r = movieSnapshot.child(currentUid).getValue(Review.class);
                        if (r != null) {
                            originalList.add(r);
                        }
                    }
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

                    ordenarLista();
                    actualizarIdiomaVisual();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };

        reviewsRef.addValueEventListener(reviewsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // IMPORTANTE: Eliminar el listener al cerrar la actividad
        if (reviewsRef != null && reviewsListener != null) {
            reviewsRef.removeEventListener(reviewsListener);
        }
    }

    // =========================================================================
    // LÓGICA DE ORDENACIÓN
    // =========================================================================

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

    private String getTextoFiltro(String titulo, int modeID) {
        if (currentFilterMode == modeID) {
            return titulo + (sortDirection == 1 ? " ⬆" : " ⬇");
        }
        return titulo;
    }

    private void aplicarFiltro(int selectedMode) {
        if (currentFilterMode == selectedMode) {
            sortDirection = (sortDirection == 1) ? 2 : 1;
        } else {
            currentFilterMode = selectedMode;
            sortDirection = 2;
        }
        ordenarLista();
    }

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

        adapter.setReviews(originalList);
    }

    // =========================================================================
    // ACTUALIZACIÓN DE IDIOMA
    // =========================================================================

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

                                // Notificamos cambios basados en el objeto, el adapter encontrará la posición
                                adapter.notifyDataSetChanged();
                            }
                        }
                        @Override public void onFailure(Call<Movie> call, Throwable t) {}
                    });
        }
    }
}