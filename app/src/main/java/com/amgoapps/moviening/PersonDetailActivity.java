package com.amgoapps.moviening;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.jsibbold.zoomage.ZoomageView;
import com.amgoapps.moviening.api.TmdbClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Actividad que muestra los detalles detallados de una persona (actor, director, etc.).
 * Incluye biografía expandible, visualización de imagen de perfil en pantalla completa
 * y acceso a sus créditos en películas.
 */
public class PersonDetailActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private TextView txtName, txtBirth, txtBio, btnExpandBio;
    private Button btnCast, btnCrew;

    private FrameLayout overlayContainer;
    private ZoomageView imgFullScreen;
    private ImageButton btnCloseOverlay;

    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";
    private boolean isBioExpanded = false;

    /**
     * Inicializa la actividad, vincula las vistas y configura los listeners de interacción.
     * @param savedInstanceState Estado de la instancia guardada.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_detail);

        int personId = getIntent().getIntExtra("PERSON_ID", -1);
        if (personId == -1) { finish(); return; }

        imgProfile = findViewById(R.id.img_person_profile);
        txtName = findViewById(R.id.txt_person_name);
        txtBirth = findViewById(R.id.txt_person_birth);
        txtBio = findViewById(R.id.txt_person_bio);
        btnExpandBio = findViewById(R.id.btn_expand_bio);
        btnCast = findViewById(R.id.btn_cast_list);
        btnCrew = findViewById(R.id.btn_crew_list);

        overlayContainer = findViewById(R.id.overlay_container);
        imgFullScreen = findViewById(R.id.img_full_screen);
        btnCloseOverlay = findViewById(R.id.btn_close_overlay);

        btnExpandBio.setOnClickListener(v -> {
            if (isBioExpanded) {
                txtBio.setMaxLines(4);
                btnExpandBio.setText(getString(R.string.show_more));
                isBioExpanded = false;
            } else {
                txtBio.setMaxLines(Integer.MAX_VALUE);
                btnExpandBio.setText(getString(R.string.show_less));
                isBioExpanded = true;
            }
        });

        View.OnClickListener closeListener = v -> {
            overlayContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                overlayContainer.setVisibility(View.GONE);
            }).start();
        };
        btnCloseOverlay.setOnClickListener(closeListener);
        overlayContainer.setOnClickListener(closeListener);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (overlayContainer.getVisibility() == View.VISIBLE) {
                    btnCloseOverlay.performClick();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        cargarInfoPersonal(personId);
        comprobarCreditos(personId);
    }

    /**
     * Obtiene y muestra la información personal de la persona desde la API de TMDB.
     * Gestiona dinámicamente la visibilidad del botón de expansión de biografía.
     * @param id ID único de la persona en TMDB.
     */
    private void cargarInfoPersonal(int id) {
        TmdbClient.getApiService().getPersonDetails(id, API_KEY, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<PersonDetailsResponse>() {
                    @Override
                    public void onResponse(Call<PersonDetailsResponse> call, Response<PersonDetailsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PersonDetailsResponse p = response.body();

                            txtName.setText(p.getName());
                            txtBirth.setText(p.getPlaceOfBirth());

                            String bioText = p.getBiography() != null && !p.getBiography().isEmpty() ? p.getBiography() : getString(R.string.no_bio);
                            txtBio.setText(bioText);

                            txtBio.setMaxLines(Integer.MAX_VALUE);

                            txtBio.post(() -> {
                                if (txtBio.getLineCount() > 4) {
                                    txtBio.setMaxLines(4);
                                    btnExpandBio.setVisibility(View.VISIBLE);
                                } else {
                                    btnExpandBio.setVisibility(View.GONE);
                                }
                            });

                            String fullImageUrl = "https://image.tmdb.org/t/p/w500" + p.getProfilePath();
                            Glide.with(PersonDetailActivity.this).load(fullImageUrl).into(imgProfile);

                            imgProfile.setOnClickListener(v -> {
                                Glide.with(PersonDetailActivity.this).load(fullImageUrl).into(imgFullScreen);
                                overlayContainer.setAlpha(0f);
                                overlayContainer.setVisibility(View.VISIBLE);
                                overlayContainer.animate().alpha(1f).setDuration(300).start();
                            });
                        }
                    }
                    @Override
                    public void onFailure(Call<PersonDetailsResponse> call, Throwable t) {}
                });
    }

    /**
     * Verifica la disponibilidad de créditos (reparto o dirección) para habilitar
     * los botones correspondientes en la interfaz.
     * @param id ID único de la persona en TMDB.
     */
    private void comprobarCreditos(int id) {
        btnCast.setVisibility(View.GONE);
        btnCrew.setVisibility(View.GONE);

        TmdbClient.getApiService().getPersonCredits(String.valueOf(id), API_KEY, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<PersonCreditsResponse>() {
                    @Override
                    public void onResponse(Call<PersonCreditsResponse> call, Response<PersonCreditsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {

                            if (response.body().getCast() != null && !response.body().getCast().isEmpty()) {
                                btnCast.setVisibility(View.VISIBLE);
                                btnCast.setOnClickListener(v -> abrirListaPeliculas(id, txtName.getText().toString(), "cast"));
                            }

                            boolean tienePeliculasComoDirector = false;
                            if (response.body().getCrew() != null) {
                                for (Movie m : response.body().getCrew()) {
                                    if ("Director".equals(m.getJob())) {
                                        tienePeliculasComoDirector = true;
                                        break;
                                    }
                                }
                            }

                            if (tienePeliculasComoDirector) {
                                btnCrew.setVisibility(View.VISIBLE);
                                btnCrew.setOnClickListener(v -> abrirListaPeliculas(id, txtName.getText().toString(), "crew"));
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<PersonCreditsResponse> call, Throwable t) {}
                });
    }

    /**
     * Inicia la actividad que muestra la lista de películas asociadas a la persona.
     * @param id ID único de la persona.
     * @param name Nombre de la persona.
     * @param type Tipo de crédito a mostrar.
     */
    private void abrirListaPeliculas(int id, String name, String type) {
        Intent intent = new Intent(PersonDetailActivity.this, PersonMoviesActivity.class);
        intent.putExtra("ID", id);
        intent.putExtra("NAME", name);
        intent.putExtra("TYPE", type);
        startActivity(intent);
    }
}