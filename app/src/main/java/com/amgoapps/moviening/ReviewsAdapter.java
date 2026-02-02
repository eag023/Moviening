package com.amgoapps.moviening;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para gestionar la visualización de las reseñas de otros usuarios.
 * Se encarga de mostrar el nombre, la valoración, el comentario y de recuperar
 * la imagen de perfil del autor desde Firebase Storage.
 */
public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private List<Review> reviews = new ArrayList<>();
    private Context context;

    /**
     * Constructor del adaptador.
     * @param context Contexto de la aplicación o actividad.
     */
    public ReviewsAdapter(Context context) {
        this.context = context;
    }

    /**
     * Actualiza el conjunto de datos de reseñas y refresca la interfaz.
     * @param reviews Nueva lista de objetos Review.
     */
    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    /**
     * Infla la vista para el elemento individual de la reseña.
     * @param parent Contenedor donde se insertará la nueva vista.
     * @param viewType Tipo de la vista.
     * @return Nueva instancia del ViewHolder.
     */
    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    /**
     * Vincula los datos de la reseña con los componentes visuales.
     * Gestiona la lógica de descarga de la foto de perfil del usuario mediante una URL
     * de descarga generada desde Firebase Storage.
     * @param holder El ViewHolder a actualizar.
     * @param position La posición del elemento en la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);

        holder.txtUsername.setText(review.getUsername() != null ? review.getUsername() : "Usuario");
        holder.txtRating.setText(String.format("%.1f", review.getRating()));

        if (review.getComment() != null && !review.getComment().isEmpty()) {
            holder.txtComment.setText(review.getComment());
            holder.txtComment.setVisibility(View.VISIBLE);
        } else {
            holder.txtComment.setVisibility(View.GONE);
        }

        String userId = review.getUserId();

        Glide.with(context)
                .load(R.drawable.placeholder_user)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.imgUser);

        if (userId != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("profile_images/" + userId + ".jpg");

            storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    if (context != null) {
                        Glide.with(context)
                                .load(uri)
                                .apply(RequestOptions.circleCropTransform())
                                .placeholder(R.drawable.placeholder_user)
                                .error(R.drawable.placeholder_user)
                                .into(holder.imgUser);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                }
            });
        }
    }

    /**
     * Obtiene el número total de reseñas en la lista.
     * @return Cantidad de elementos.
     */
    @Override
    public int getItemCount() { return reviews.size(); }

    /**
     * Clase interna para referenciar las vistas de cada elemento de reseña.
     */
    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUser;
        TextView txtUsername, txtRating, txtComment;

        /**
         * Constructor del ViewHolder de reseñas.
         * @param itemView Vista inflada del elemento.
         */
        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUser = itemView.findViewById(R.id.img_review_user);
            txtUsername = itemView.findViewById(R.id.txt_review_username);
            txtRating = itemView.findViewById(R.id.txt_review_rating);
            txtComment = itemView.findViewById(R.id.txt_review_comment);
        }
    }
}