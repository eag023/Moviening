package com.amgoapps.moviening;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.amgoapps.moviening.api.TmdbClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adaptador para gestionar y mostrar la lista de reseñas realizadas por el usuario actual.
 * Proporciona funcionalidades para visualizar detalles de la película y eliminar reseñas.
 */
public class MyReviewsAdapter extends RecyclerView.Adapter<MyReviewsAdapter.ViewHolder> {

    private Context context;
    private List<Review> reviews;
    private static final String API_KEY = "73987aabdaf7db8fdb77f48a49fba2ee";

    /**
     * Constructor del adaptador.
     * @param context Contexto de la aplicación o actividad.
     * @param reviews Lista de objetos Review a mostrar.
     */
    public MyReviewsAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    /**
     * Actualiza la lista de reseñas y notifica al adaptador los cambios.
     * @param reviews Nueva lista de reseñas.
     */
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    /**
     * Infla el diseño de la vista para cada elemento de la lista.
     * @param parent El ViewGroup en el que se añadirá la nueva vista.
     * @param viewType El tipo de vista de la nueva vista.
     * @return Una nueva instancia de ViewHolder.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_review, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Vincula los datos de una reseña específica con los componentes de la interfaz.
     * @param holder El ViewHolder que debe ser actualizado.
     * @param position La posición del elemento dentro del conjunto de datos.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.txtTitle.setText(review.getMovieTitle());
        holder.txtRating.setText(String.format("%.1f", review.getRating()));
        holder.txtComment.setText(review.getComment());

        String posterUrl = "https://image.tmdb.org/t/p/w500" + review.getMoviePoster();
        Glide.with(context)
                .load(posterUrl)
                .placeholder(R.drawable.moviening_icon)
                .into(holder.imgPoster);

        holder.itemView.setOnClickListener(v -> {
            try {
                int movieId = Integer.parseInt(review.getMovieId());
                abrirDetallePeliculaConDatosCompletos(movieId);
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Error ID Película", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            mostrarPopupEliminar(v, review, position);
            return true;
        });
    }

    /**
     * Realiza una petición a la API de TMDB para obtener la información completa de una película
     * y abre la actividad de detalles.
     * @param movieId ID de la película en TMDB.
     */
    private void abrirDetallePeliculaConDatosCompletos(int movieId) {
        TmdbClient.getApiService().getMovieDetails(movieId, API_KEY, LanguageUtils.getApiLanguage())
                .enqueue(new Callback<Movie>() {
                    @Override
                    public void onResponse(Call<Movie> call, Response<Movie> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Movie fullMovie = response.body();
                            Intent intent = new Intent(context, MovieDetailActivity.class);
                            intent.putExtra("MOVIE_DATA", fullMovie);
                            context.startActivity(intent);
                        } else {
                            Toast.makeText(context, R.string.error_loading, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Movie> call, Throwable t) {
                        Toast.makeText(context, R.string.error_conexion, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Muestra una ventana emergente para confirmar la eliminación de una reseña.
     * @param anchorView Vista que actúa como referencia para posicionar el popup.
     * @param review El objeto Review que se desea eliminar.
     * @param position La posición del elemento en la lista.
     */
    private void mostrarPopupEliminar(View anchorView, Review review, int position) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_delete_item, null);

        final PopupWindow popupWindow = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setElevation(20);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);

        popupView.findViewById(R.id.btn_delete_item).setOnClickListener(v -> {
            popupWindow.dismiss();
            borrarResenaDeFirebase(review, position);
        });

        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);

        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY,
                location[0] + (anchorView.getWidth() / 4),
                location[1] + (anchorView.getHeight() / 2));
    }

    /**
     * Elimina de forma definitiva la reseña de la base de datos Firebase y actualiza la UI.
     * @param review Reseña a eliminar.
     * @param position Posición del elemento en la lista local.
     */
    private void borrarResenaDeFirebase(Review review, int position) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("reviews")
                .child(review.getMovieId())
                .child(uid)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    reviews.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, reviews.size());
                    Toast.makeText(context, R.string.review_deleted, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Devuelve el número total de reseñas en la lista.
     * @return Tamaño de la lista de reseñas.
     */
    @Override
    public int getItemCount() { return reviews.size(); }

    /**
     * Clase interna que contiene las referencias a las vistas de cada elemento del RecyclerView.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView txtTitle, txtRating, txtComment;

        /**
         * Constructor del ViewHolder.
         * @param v Vista del elemento inflado.
         */
        ViewHolder(View v) {
            super(v);
            imgPoster = v.findViewById(R.id.img_review_poster);
            txtTitle = v.findViewById(R.id.txt_review_movie_title);
            txtRating = v.findViewById(R.id.txt_review_rating);
            txtComment = v.findViewById(R.id.txt_review_comment);
        }
    }
}