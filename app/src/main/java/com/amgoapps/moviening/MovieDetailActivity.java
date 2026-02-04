package com.amgoapps.moviening;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.amgoapps.moviening.api.MovieCreditsResponse;
import com.amgoapps.moviening.api.TmdbClient;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jsibbold.zoomage.ZoomageView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity para mostrar la ficha técnica completa de una película.
 * Funcionalidades principales:
 * 1. Información básica (Póster, Título, Sinopsis expandible).
 * 2. Gestión de pestañas: Equipo, Reseñas, Relacionados (Sagas).
 * 3. Acciones de usuario: Añadir a listas y valorar.
 * 4. Integración con Firebase para persistencia de datos de usuario y reseñas.
 */
public class MovieDetailActivity extends AppCompatActivity {

    private ImageView imgPoster;
    private TextView txtTitle, txtYear, txtOverview, btnExpandOverview, txtGenres;
    private TextView lblDirector, lblActors;
    private RecyclerView recyclerDirector, recyclerActors;
    private Movie currentMovie;

    private ImageView btnFav, btnWatched, btnWatchlist, btnOtherLists;

    private TextView txtRatingPill;
    private ImageView[] stars = new ImageView[10];

    private TabLayout tabLayout;
    private LinearLayout layoutTeamContainer, layoutReviewsContainer;
    private RecyclerView recyclerReviews;
    private TextView txtNoReviews, lblReviewsTitle;
    private Button btnRateMovie;

    private LinearLayout layoutRelatedContainer;

    private LinearLayout layoutUserAverageSection;
    private TextView txtUserAveragePill;
    private ImageView[] userStars = new ImageView[10];

    private ReviewsAdapter reviewsAdapter;
    private Review myReview = null;
    private DatabaseReference reviewsRef;

    private FrameLayout overlayContainer;
    private ZoomageView imgFullScreen;
    private ImageButton btnCloseOverlay;
    private androidx.core.widget.NestedScrollView nestedScrollView;

    private boolean isOverviewExpanded = false;
    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        currentMovie = (Movie) getIntent().getSerializableExtra("MOVIE_DATA");
        if (currentMovie == null) { finish(); return; }

        nestedScrollView = findViewById(R.id.nested_scroll_view);
        imgPoster = findViewById(R.id.img_detail_poster);
        txtTitle = findViewById(R.id.txt_detail_title);
        txtYear = findViewById(R.id.txt_detail_year);
        txtGenres = findViewById(R.id.txt_detail_genres);
        txtOverview = findViewById(R.id.txt_detail_overview);
        btnExpandOverview = findViewById(R.id.btn_expand_overview);
        lblDirector = findViewById(R.id.lbl_director);
        lblActors = findViewById(R.id.lbl_actors);
        recyclerDirector = findViewById(R.id.recycler_directors);
        recyclerActors = findViewById(R.id.recycler_actors);

        btnFav = findViewById(R.id.btn_detail_fav);
        btnWatched = findViewById(R.id.btn_detail_watched);
        btnWatchlist = findViewById(R.id.btn_detail_watchlist);
        btnOtherLists = findViewById(R.id.btn_detail_add_list);

        txtRatingPill = findViewById(R.id.txt_rating_pill);
        stars[0] = findViewById(R.id.star1); stars[1] = findViewById(R.id.star2);
        stars[2] = findViewById(R.id.star3); stars[3] = findViewById(R.id.star4);
        stars[4] = findViewById(R.id.star5); stars[5] = findViewById(R.id.star6);
        stars[6] = findViewById(R.id.star7); stars[7] = findViewById(R.id.star8);
        stars[8] = findViewById(R.id.star9); stars[9] = findViewById(R.id.star10);

        overlayContainer = findViewById(R.id.overlay_container);
        imgFullScreen = findViewById(R.id.img_full_screen);
        btnCloseOverlay = findViewById(R.id.btn_close_overlay);

        tabLayout = findViewById(R.id.tab_layout_detail);
        layoutTeamContainer = findViewById(R.id.layout_team_container);
        layoutReviewsContainer = findViewById(R.id.layout_reviews_container);
        layoutRelatedContainer = findViewById(R.id.layout_related_container);

        recyclerReviews = findViewById(R.id.recycler_reviews);
        txtNoReviews = findViewById(R.id.txt_no_rate);
        lblReviewsTitle = findViewById(R.id.lbl_reviews_title);
        btnRateMovie = findViewById(R.id.btn_rate_movie);

        layoutUserAverageSection = findViewById(R.id.layout_user_average_section);
        txtUserAveragePill = findViewById(R.id.txt_user_average_pill);
        userStars[0] = findViewById(R.id.user_star1); userStars[1] = findViewById(R.id.user_star2);
        userStars[2] = findViewById(R.id.user_star3); userStars[3] = findViewById(R.id.user_star4);
        userStars[4] = findViewById(R.id.user_star5); userStars[5] = findViewById(R.id.user_star6);
        userStars[6] = findViewById(R.id.user_star7); userStars[7] = findViewById(R.id.user_star8);
        userStars[8] = findViewById(R.id.user_star9); userStars[9] = findViewById(R.id.user_star10);

        txtTitle.setText(currentMovie.getTitle());

        // Lógica de formateo de fecha (Año o "Próximamente")
        String dateStr = currentMovie.getReleaseDate();
        if (dateStr == null || dateStr.isEmpty()) {
            txtYear.setText(getString(R.string.coming_soon));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date releaseDate = sdf.parse(dateStr);
                Date today = new Date();
                if (releaseDate != null && releaseDate.after(today)) {
                    String year = (dateStr.length() >= 4) ? dateStr.substring(0, 4) : "";
                    if (!year.isEmpty()) txtYear.setText(getString(R.string.coming_soon) + " (" + year + ")");
                    else txtYear.setText(getString(R.string.coming_soon));
                } else {
                    if (dateStr.length() >= 4) txtYear.setText(dateStr.substring(0, 4));
                    else txtYear.setText("");
                }
            } catch (ParseException e) {
                if (dateStr.length() >= 4) txtYear.setText(dateStr.substring(0, 4));
                else txtYear.setText("");
            }
        }

        configurarPuntuacion(currentMovie.getTmdbRating());
        Glide.with(this).load(currentMovie.getFullPosterUrl()).into(imgPoster);

        // Lógica de expansión de la sinopsis
        if (currentMovie.getOverview() != null && !currentMovie.getOverview().isEmpty()) {
            txtOverview.setText(currentMovie.getOverview());
            txtOverview.setMaxLines(Integer.MAX_VALUE);
            txtOverview.post(() -> {
                if (txtOverview.getLineCount() > 4) {
                    txtOverview.setMaxLines(4);
                    btnExpandOverview.setVisibility(View.VISIBLE);
                } else {
                    btnExpandOverview.setVisibility(View.GONE);
                }
            });
        } else {
            txtOverview.setText(getString(R.string.no_synopsis));
            btnExpandOverview.setVisibility(View.GONE);
        }
        btnExpandOverview.setOnClickListener(v -> {
            if (isOverviewExpanded) {
                txtOverview.setMaxLines(4);
                btnExpandOverview.setText(getString(R.string.show_more));
                isOverviewExpanded = false;
            } else {
                txtOverview.setMaxLines(Integer.MAX_VALUE);
                btnExpandOverview.setText(getString(R.string.show_less));
                isOverviewExpanded = true;
            }
        });

        // Configuración del visor de imagen completa
        imgPoster.setOnClickListener(v -> {
            Glide.with(this).load(currentMovie.getFullPosterUrl()).into(imgFullScreen);
            overlayContainer.setAlpha(0f);
            overlayContainer.setVisibility(View.VISIBLE);
            overlayContainer.animate().alpha(1f).setDuration(300).start();
        });
        View.OnClickListener closeListener = v -> {
            overlayContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> overlayContainer.setVisibility(View.GONE)).start();
        };
        btnCloseOverlay.setOnClickListener(closeListener);
        overlayContainer.setOnClickListener(closeListener);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (overlayContainer.getVisibility() == View.VISIBLE) btnCloseOverlay.performClick();
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });

        // Carga de datos
        cargarCreditosPelicula(currentMovie.getId());
        configurarBotonesAccion(currentMovie);

        obtenerDetallesCompletos();

        // Configuración del módulo de reseñas
        recyclerReviews.setLayoutManager(new LinearLayoutManager(this));
        reviewsAdapter = new ReviewsAdapter(this);
        recyclerReviews.setAdapter(reviewsAdapter);
        reviewsRef = FirebaseDatabase.getInstance().getReference("reviews").child(String.valueOf(currentMovie.getId()));

        // Gestión de visibilidad de contenedores según la pestaña seleccionada
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                layoutTeamContainer.setVisibility(View.GONE);
                layoutReviewsContainer.setVisibility(View.GONE);
                layoutRelatedContainer.setVisibility(View.GONE);

                if (tab.getPosition() == 0) {
                    layoutTeamContainer.setVisibility(View.VISIBLE);
                } else if (tab.getPosition() == 1) {
                    layoutReviewsContainer.setVisibility(View.VISIBLE);
                } else {
                    layoutRelatedContainer.setVisibility(View.VISIBLE);
                }

                // Animación de despazamiento hasta el final de la pantalla al pulsar en un pestaña
                nestedScrollView.post(() -> {
                    View lastChild = nestedScrollView.getChildAt(0);
                    int bottom = lastChild.getBottom() + nestedScrollView.getPaddingBottom();
                    int delta = bottom - (nestedScrollView.getHeight() + nestedScrollView.getScrollY());

                    nestedScrollView.smoothScrollBy(0, delta);
                });
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        cargarResenasYEstadoUsuario();
        btnRateMovie.setOnClickListener(v -> mostrarDialogoValorar(currentMovie));
    }

    /**
     * Verifica si la película pertenece a una colección.
     */
    private void obtenerDetallesCompletos() {
        TmdbClient.getApiService().getMovieDetails(currentMovie.getId(), API_KEY, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<Movie>() {
                    @Override
                    public void onResponse(Call<Movie> call, Response<Movie> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Movie fullDetails = response.body();

                            mostrarGeneros(fullDetails.getGenres());

                            if (fullDetails.getBelongsToCollection() != null) {
                                currentMovie.setBelongsToCollection(fullDetails.getBelongsToCollection());
                            }
                            cargarPeliculasRelacionadas();
                        }
                    }

                    @Override
                    public void onFailure(Call<Movie> call, Throwable t) {
                        cargarPeliculasRelacionadas();
                    }
                });
    }

    /**
     * Si hay colección, solicita los datos de todas las partes de la saga.
     */
    private void cargarPeliculasRelacionadas() {
        if (currentMovie.getBelongsToCollection() == null) {
            return;
        }

        int collectionId = currentMovie.getBelongsToCollection().id;

        TmdbClient.getApiService().getCollectionDetails(collectionId, API_KEY, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<CollectionResponse>() {
                    @Override
                    public void onResponse(Call<CollectionResponse> call, Response<CollectionResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            procesarColeccion(response.body().getParts());
                        }
                    }
                    @Override
                    public void onFailure(Call<CollectionResponse> call, Throwable t) { }
                });
    }

    /**
     * Procesa la lista de películas de la saga para determinar precuelas y secuelas.
     * Ordena cronológicamente y añade la pestaña "Relacionados" dinámicamente.
     */
    private void procesarColeccion(List<Movie> parts) {
        if (parts == null) return;

        Collections.sort(parts, (m1, m2) -> {
            String d1 = m1.getReleaseDate();
            String d2 = m2.getReleaseDate();
            if (d1 == null || d1.isEmpty()) d1 = "9999-12-31";
            if (d2 == null || d2.isEmpty()) d2 = "9999-12-31";
            return d1.compareTo(d2);
        });

        int currentIndex = -1;
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).getId() == currentMovie.getId()) {
                currentIndex = i;
                break;
            }
        }

        layoutRelatedContainer.removeAllViews();
        if (currentIndex == -1) return;

        boolean addedContent = false;

        // Mostrar Secuela (Siguiente en la lista)
        if (currentIndex < parts.size() - 1) {
            Movie secuela = parts.get(currentIndex + 1);
            if (esPeliculaValidaParaMostrar(secuela)) {
                agregarSeccionRelacionada(getString(R.string.secuel), secuela);
                addedContent = true;
            }
        }

        // Mostrar Precuela (Anterior en la lista)
        if (currentIndex > 0) {
            Movie precuela = parts.get(currentIndex - 1);
            if (esPeliculaValidaParaMostrar(precuela)) {
                agregarSeccionRelacionada(getString(R.string.precuel), precuela);
                addedContent = true;
            }
        }

        // Añadir pestaña si se encontró contenido relevante
        if (addedContent) {
            if (tabLayout.getTabCount() < 3) {
                tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.related)));
            }
        }
    }

    private boolean esPeliculaValidaParaMostrar(Movie m) {
        return m.getTitle() != null && !m.getTitle().isEmpty();
    }

    /**
     * Genera dinámicamente la vista para una película relacionada.
     */
    private void agregarSeccionRelacionada(String tituloSeccion, Movie movie) {
        TextView txtHeader = new TextView(this);
        txtHeader.setText(tituloSeccion);
        txtHeader.setTextSize(16);
        txtHeader.setTypeface(null, Typeface.BOLD);
        txtHeader.setTextColor(ContextCompat.getColor(this, R.color.purple));
        txtHeader.setPadding(0, 24, 0, 8);
        layoutRelatedContainer.addView(txtHeader);

        View movieView = LayoutInflater.from(this).inflate(R.layout.item_related_movie, layoutRelatedContainer, false);
        ImageView img = movieView.findViewById(R.id.img_related_poster);
        TextView title = movieView.findViewById(R.id.txt_related_title);
        TextView year = movieView.findViewById(R.id.txt_related_year);

        title.setText(movie.getTitle());
        if (movie.getReleaseDate() != null && movie.getReleaseDate().length() >= 4) {
            year.setText(movie.getReleaseDate().substring(0, 4));
        } else {
            year.setText("----");
        }

        Glide.with(this).load(movie.getFullPosterUrl()).into(img);
        movieView.setOnClickListener(v -> {
            Intent intent = new Intent(this, MovieDetailActivity.class);
            intent.putExtra("MOVIE_DATA", movie);
            startActivity(intent);
        });

        layoutRelatedContainer.addView(movieView);
    }

    // --- Otros métodos ---

    /**
     * Procesa la lista de géneros de la película y los muestra en el TextView correspondiente
     * formateados como una cadena de texto separada por comas.
     *
     * @param genres Lista de mapas que contienen la información de los géneros de TMDB.
     */
    private void mostrarGeneros(List<Map<String, Object>> genres) {
        if (genres == null || genres.isEmpty()) {
            txtGenres.setVisibility(View.GONE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < genres.size(); i++) {
            Map<String, Object> genreMap = genres.get(i);
            Object nameObj = genreMap.get("name");

            if (nameObj != null) {
                sb.append(nameObj.toString());

                if (i < genres.size() - 1) {
                    sb.append(", ");
                }
            }
        }

        if (sb.length() > 0) {
            txtGenres.setText(sb.toString());
            txtGenres.setVisibility(View.VISIBLE);
        } else {
            txtGenres.setVisibility(View.GONE);
        }
    }

    /**
     * Carga las reseñas desde Firebase en tiempo real.
     * Calcula la media de puntuación de los usuarios de la app.
     */
    private void cargarResenasYEstadoUsuario() {
        reviewsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Review> allReviews = new ArrayList<>();
                List<Review> commentsOnly = new ArrayList<>();
                String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                myReview = null;
                double sumRating = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Review r = data.getValue(Review.class);
                    if (r != null) {
                        allReviews.add(r);
                        sumRating += r.getRating();
                        if (r.getUserId().equals(currentUid)) myReview = r;
                        if (r.getComment() != null && !r.getComment().trim().isEmpty()) commentsOnly.add(r);
                    }
                }

                if (myReview != null) btnRateMovie.setText(R.string.edit_review);
                else btnRateMovie.setText(R.string.bt_rate);

                if (!allReviews.isEmpty()) {
                    configurarEstrellasUsuarios(sumRating / allReviews.size());
                    layoutUserAverageSection.setVisibility(View.VISIBLE);
                    lblReviewsTitle.setVisibility(View.VISIBLE);
                } else {
                    layoutUserAverageSection.setVisibility(View.GONE);
                    lblReviewsTitle.setVisibility(View.GONE);
                }

                reviewsAdapter.setReviews(commentsOnly);

                if (!commentsOnly.isEmpty()) {
                    txtNoReviews.setVisibility(View.GONE);
                    recyclerReviews.setVisibility(View.VISIBLE);
                } else {
                    txtNoReviews.setVisibility(View.VISIBLE);
                    recyclerReviews.setVisibility(View.GONE);
                    txtNoReviews.setText(!allReviews.isEmpty() ? R.string.no_reviews : R.string.no_rate);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Configura visualmente la barra de estrellas del promedio de usuarios.
     * @param average El valor promedio de la puntuación de los usuarios (0.0 a 10.0).
     */
    private void configurarEstrellasUsuarios(double average) {
        txtUserAveragePill.setText(String.format(Locale.US, "%.1f", average));
        for (int i = 0; i < 10; i++) {
            if (userStars[i] == null) continue;
            userStars[i].setColorFilter(ContextCompat.getColor(this, R.color.purple), PorterDuff.Mode.SRC_IN);
            double starValue = i + 1;
            if (average >= starValue) userStars[i].setImageResource(R.drawable.ic_star_filled);
            else if (average >= starValue - 0.5) userStars[i].setImageResource(R.drawable.ic_star_half);
            else userStars[i].setImageResource(R.drawable.ic_star_outline);
        }
    }

    /**
     * Muestra el diálogo para que el usuario pueda valorar y comentar una película.
     * Permite editar una reseña existente o crear una nueva.
     * @param movie El objeto Movie que se va a valorar.
     */
    private void mostrarDialogoValorar(Movie movie) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, R.string.no_login, Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_rate_movie, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        EditText etRating = view.findViewById(R.id.et_rating_input);
        EditText etComment = view.findViewById(R.id.et_comment_input);
        ImageView btnDelete = view.findViewById(R.id.btn_delete_review);
        Button btnSave = view.findViewById(R.id.btn_save_review);
        if (myReview != null) {
            etRating.setText(String.valueOf(myReview.getRating()));
            etComment.setText(myReview.getComment());
            btnDelete.setVisibility(View.VISIBLE);
            btnSave.setText(R.string.update);
        } else {
            btnDelete.setVisibility(View.GONE);
            btnSave.setText(R.string.save);
        }
        btnSave.setOnClickListener(v -> {
            String ratingStr = etRating.getText().toString().trim();
            String comment = etComment.getText().toString().trim();
            if (ratingStr.isEmpty()) { etRating.setError(getString(R.string.set_rate)); return; }
            double rating;
            try { rating = Double.parseDouble(ratingStr); if (rating < 0 || rating > 10) { etRating.setError("0 - 10"); return; } }
            catch (NumberFormatException e) { etRating.setError(getString(R.string.invalid)); return; }
            publicarResena(rating, comment, dialog);
        });
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle(R.string.delete_review).setMessage(R.string.r_u_sure).setPositiveButton(R.string.delete, (d, w) -> borrarResena(dialog)).setNegativeButton(R.string.cancel, null).show());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Guarda o actualiza la reseña del usuario en Firebase Realtime Database.
     * Al publicar, también añade automáticamente la película a la lista "Vistas".
     * @param rating Puntuación numérica otorgada.
     * @param comment Texto del comentario.
     * @param dialog Referencia al diálogo para cerrarlo tras el éxito.
     */
    private void publicarResena(double rating, String comment, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String name = (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) ? user.getDisplayName() : getString(R.string.anonymous);

        FirebaseDatabase.getInstance().getReference("users").child(uid).child("photoUrl")
                .get().addOnCompleteListener(task -> {
                    String finalPhoto = null;

                    if (task.isSuccessful() && task.getResult().exists()) {
                        finalPhoto = task.getResult().getValue(String.class);
                    } else if (user.getPhotoUrl() != null) {
                        finalPhoto = user.getPhotoUrl().toString();
                    }

                    Review newReview = new Review(uid, name, finalPhoto, rating, comment,
                            String.valueOf(currentMovie.getId()), currentMovie.getTitle(), currentMovie.getPosterPath());

                    reviewsRef.child(uid).setValue(newReview).addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.review_saved, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        DatabaseReference watchedRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(getString(R.string.watched)).child(String.valueOf(currentMovie.getId()));
                        currentMovie.setTimestampAdded(System.currentTimeMillis());
                        watchedRef.setValue(currentMovie).addOnSuccessListener(v -> {
                            actualizarIcono(btnWatched, true, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green);
                            DatabaseReference watchlistRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(getString(R.string.watchlist)).child(String.valueOf(currentMovie.getId()));
                            watchlistRef.removeValue().addOnSuccessListener(v2 -> actualizarIcono(btnWatchlist, false, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));
                        });
                    });
                });
    }

    /**
     * Elimina la reseña del usuario de la base de datos de Firebase.
     * @param dialogParent Referencia al diálogo abierto para cerrarlo tras la eliminación.
     */
    private void borrarResena(AlertDialog dialogParent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        reviewsRef.child(user.getUid()).removeValue().addOnSuccessListener(aVoid -> { Toast.makeText(this, R.string.review_deleted, Toast.LENGTH_SHORT).show(); dialogParent.dismiss(); });
    }

    /**
     * Configura visualmente la barra de estrellas para la puntuación oficial (TMDB).
     * @param rating Puntuación de la película (0.0 a 10.0).
     */
    private void configurarPuntuacion(double rating) {
        if (rating > 0) txtRatingPill.setText(String.format(Locale.US, "%.1f", rating)); else txtRatingPill.setText("--");
        for (int i = 0; i < 10; i++) {
            if (stars[i] == null) continue;
            stars[i].setColorFilter(ContextCompat.getColor(this, R.color.orange), PorterDuff.Mode.SRC_IN);
            double starValue = i + 1;
            if (rating >= starValue) stars[i].setImageResource(R.drawable.ic_star_filled);
            else if (rating >= starValue - 0.5) stars[i].setImageResource(R.drawable.ic_star_half);
            else stars[i].setImageResource(R.drawable.ic_star_outline);
        }
    }

    /**
     * Realiza la petición a la API de TMDB para obtener el reparto y el equipo técnico.
     * @param movieId ID de la película en TMDB.
     */
    private void cargarCreditosPelicula(int movieId) {
        TmdbClient.getApiService().getMovieCredits(movieId, API_KEY, LanguageUtils.getApiLanguage()).enqueue(new Callback<MovieCreditsResponse>() {
            @Override public void onResponse(Call<MovieCreditsResponse> call, Response<MovieCreditsResponse> response) { if (response.isSuccessful() && response.body() != null) procesarCreditos(response.body()); }
            @Override public void onFailure(Call<MovieCreditsResponse> call, Throwable t) {}
        });
    }

    /**
     * Procesa la respuesta de créditos para mostrar directores y actores en sus respectivos RecyclerViews.
     * @param credits Objeto que contiene las listas de reparto (cast) y equipo (crew).
     */
    private void procesarCreditos(MovieCreditsResponse credits) {
        List<Movie> directores = new ArrayList<>();
        if (credits.getCrew() != null) for (Movie m : credits.getCrew()) if ("Director".equals(m.getJob())) directores.add(m);
        if (!directores.isEmpty()) {
            lblDirector.setVisibility(View.VISIBLE); recyclerDirector.setVisibility(View.VISIBLE);
            recyclerDirector.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerDirector.setAdapter(new PersonHorizontalAdapter(this, directores));
        }
        List<Movie> actores = credits.getCast();
        if (actores != null && !actores.isEmpty()) {
            if (actores.size() > 15) actores = actores.subList(0, 15);
            lblActors.setVisibility(View.VISIBLE); recyclerActors.setVisibility(View.VISIBLE);
            recyclerActors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            recyclerActors.setAdapter(new PersonHorizontalAdapter(this, actores));
        }
    }

    /**
     * Configura los botones de acción rápida (Favoritas, Vistas, Pendientes) y sus tooltips.
     * Verifica inicialmente si la película ya pertenece a alguna de estas listas.
     * @param movie La película actual.
     */
    private void configurarBotonesAccion(Movie movie) {
        TooltipCompat.setTooltipText(btnFav, getString(R.string.favorites));
        TooltipCompat.setTooltipText(btnWatched, getString(R.string.watched));
        TooltipCompat.setTooltipText(btnWatchlist, getString(R.string.watchlist));
        TooltipCompat.setTooltipText(btnOtherLists, getString(R.string.my_lists));
        checkMovieInList(getString(R.string.favorites), movie.getId(), exists -> actualizarIcono(btnFav, exists, R.drawable.ic_heart, R.drawable.ic_heart_filled, R.color.dark_purple));
        checkMovieInList(getString(R.string.watched), movie.getId(), exists -> actualizarIcono(btnWatched, exists, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green));
        checkMovieInList(getString(R.string.watchlist), movie.getId(), exists -> actualizarIcono(btnWatchlist, exists, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));
        btnFav.setOnClickListener(v -> toggleMovieInList(getString(R.string.favorites), movie, btnFav, R.drawable.ic_heart, R.drawable.ic_heart_filled, R.color.dark_purple));
        btnWatched.setOnClickListener(v -> toggleMovieInList(getString(R.string.watched), movie, btnWatched, R.drawable.ic_eye, R.drawable.ic_eye_filled, R.color.green));
        btnWatchlist.setOnClickListener(v -> toggleMovieInList(getString(R.string.watchlist), movie, btnWatchlist, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));
        btnOtherLists.setOnClickListener(v -> mostrarDialogoOtrasListas(movie));
    }

    /**
     * Consulta en Firebase si una película específica existe en una lista determinada.
     * @param listName Nombre de la lista a consultar.
     * @param movieId ID de la película.
     * @param listener Callback para devolver el resultado booleano.
     */
    private void checkMovieInList(String listName, int movieId, OnCheckListener listener) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(listName).child(String.valueOf(movieId)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { listener.onCheck(s.exists()); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    /**
     * Interfaz para manejar la comprobación asíncrona de existencia de datos.
     */
    interface OnCheckListener { void onCheck(boolean exists); }

    /**
     * Actualiza el estado visual de un ImageView (cambio de icono y color) según si el elemento está seleccionado.
     * @param img El ImageView a modificar.
     * @param filled Booleano que indica si debe mostrarse el estado "activo/relleno".
     * @param resNormal Recurso del icono para estado desactivado.
     * @param resFilled Recurso del icono para estado activado.
     * @param colorResId Color para el estado activado.
     */
    private void actualizarIcono(ImageView img, boolean filled, int resNormal, int resFilled, int colorResId) {
        if (filled) { img.setImageResource(resFilled); img.setColorFilter(ContextCompat.getColor(this, colorResId), PorterDuff.Mode.SRC_IN); }
        else { img.setImageResource(resNormal); img.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN); img.clearColorFilter(); }
        img.setTag(filled);
    }

    /**
     * Alterna la presencia de una película en una lista de Firebase (la añade si no está, la quita si está).
     * Maneja la lógica de exclusión entre "Vistas" y "Pendientes".
     * @param listName Nombre de la lista.
     * @param movie Película a procesar.
     * @param img Botón que disparó la acción para actualizar su estado visual.
     * @param resNormal Icono por defecto.
     * @param resFilled Icono seleccionado.
     * @param colorResId Color seleccionado.
     */
    private void toggleMovieInList(String listName, Movie movie, ImageView img, int resNormal, int resFilled, int colorResId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference listRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(listName);
        DatabaseReference movieRef = listRef.child(String.valueOf(movie.getId()));

        boolean isAdded = (boolean) (img.getTag() != null ? img.getTag() : false);

        if (isAdded) {
            movieRef.removeValue().addOnSuccessListener(v -> actualizarIcono(img, false, resNormal, resFilled, colorResId));
        } else {
            movie.setTimestampAdded(System.currentTimeMillis());
            movieRef.setValue(movie).addOnSuccessListener(v -> {
                actualizarIcono(img, true, resNormal, resFilled, colorResId);
                if (listName.equals(getString(R.string.watched))) {
                    DatabaseReference watchlistRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists").child(getString(R.string.watchlist)).child(String.valueOf(movie.getId()));
                    watchlistRef.removeValue().addOnSuccessListener(v2 -> actualizarIcono(btnWatchlist, false, R.drawable.ic_clock, R.drawable.ic_clock_filled, R.color.blue));
                }
            });
        }
    }

    /**
     * Muestra un diálogo que permite gestionar la película en listas personalizadas del usuario.
     * Permite crear nuevas listas dinámicamente.
     * @param movie La película actual.
     */
    private void mostrarDialogoOtrasListas(Movie movie) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userListsRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("lists");
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_to_custom_list, null);
        LinearLayout containerLists = dialogView.findViewById(R.id.container_custom_lists);
        EditText etNewList = dialogView.findViewById(R.id.et_new_list_quick);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        userListsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) { containerLists.removeAllViews(); for (DataSnapshot data : snapshot.getChildren()) { String listName = data.getKey(); if (listName != null && !listName.equals(getString(R.string.favorites)) && !listName.equals(getString(R.string.watched)) && !listName.equals(getString(R.string.watchlist))) agregarCheckBoxLista(containerLists, listName, movie, userListsRef); } }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        etNewList.setOnEditorActionListener((v, actionId, event) -> { if (actionId == EditorInfo.IME_ACTION_DONE) { crearListaDesdeInput(etNewList, containerLists, movie, userListsRef); InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); if (imm != null) imm.hideSoftInputFromWindow(etNewList.getWindowToken(), 0); etNewList.clearFocus(); return true; } return false; });
        dialog.show();
    }

    /**
     * Crea una nueva lista personalizada a partir del texto ingresado en el campo de texto.
     * @param editText Campo de texto con el nombre de la nueva lista.
     * @param container Contenedor visual donde se añadirá el nuevo CheckBox.
     * @param movie Película actual.
     * @param userListsRef Referencia de Firebase a las listas del usuario.
     */
    private void crearListaDesdeInput(EditText editText, LinearLayout container, Movie movie, DatabaseReference userListsRef) {
        String newListName = editText.getText().toString().trim();
        if (!newListName.isEmpty()) { userListsRef.child(newListName).setValue("created"); agregarCheckBoxLista(container, newListName, movie, userListsRef); editText.setText(""); }
    }

    /**
     * Crea y añade un CheckBox dinámicamente para una lista específica.
     * Maneja la lógica de añadir/quitar la película de dicha lista al marcar/desmarcar.
     * @param container Vista padre donde se inserta el CheckBox.
     * @param listName Nombre de la lista personalizada.
     * @param movie Película a gestionar.
     * @param userListsRef Referencia de Firebase a las listas del usuario.
     */
    private void agregarCheckBoxLista(LinearLayout container, String listName, Movie movie, DatabaseReference userListsRef) {
        CheckBox checkBox = new CheckBox(this); checkBox.setText(listName); checkBox.setTextSize(16); checkBox.setPadding(16, 16, 16, 16);
        userListsRef.child(listName).child(String.valueOf(movie.getId())).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { checkBox.setOnCheckedChangeListener(null); checkBox.setChecked(s.exists()); checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> { DatabaseReference movieRef = userListsRef.child(listName).child(String.valueOf(movie.getId())); if (isChecked) movieRef.setValue(movie); else movieRef.removeValue().addOnSuccessListener(a -> { userListsRef.child(listName).addListenerForSingleValueEvent(new ValueEventListener() { @Override public void onDataChange(@NonNull DataSnapshot sn) { if (!sn.exists()) userListsRef.child(listName).setValue("created"); } @Override public void onCancelled(@NonNull DatabaseError e) {} }); }); }); }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
        container.addView(checkBox);
    }
}