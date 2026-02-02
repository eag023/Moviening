package com.amgoapps.moviening;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adaptador para el RecyclerView encargado de mostrar la lista de películas o personas.
 *
 * Funcionalidades principales:
 * 1. Carga de imágenes con Glide.
 * 2. Gestión de placeholders para películas sin póster.
 * 3. Lógica de visualización de fechas (Estrenadas vs "Próximamente").
 * 4. Gestión de eventos de Clic y Long Click.
 */
public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private List<Movie> movieList;
    private Context context;

    private OnItemClickListener listener;
    private OnMovieLongClickListener longClickListener;

    /**
     * Interfaz para gestionar clics simples en un elemento (abrir detalles).
     */
    public interface OnItemClickListener {
        void onItemClick(Movie movie);
    }

    /**
     * Interfaz para gestionar pulsaciones largas (abrir menú rápido/contextual).
     */
    public interface OnMovieLongClickListener {
        void onMovieLongClick(Movie movie, View view);
    }

    // Setters para los listeners
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnMovieLongClickListener(OnMovieLongClickListener listener) {
        this.longClickListener = listener;
    }

    public MovieAdapter(Context context, List<Movie> movieList) {
        this.context = context;
        this.movieList = movieList;
    }

    /**
     * Actualiza la lista de datos del adaptador y notifica a la vista para refrescarse.
     * @param newMovies Nueva lista de objetos Movie.
     */
    public void setMovies(List<Movie> newMovies) {
        this.movieList = newMovies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @SuppressLint("ResourceType")
    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        // Configuración del Título
        String displayText = movie.getTitle() != null ? movie.getTitle() : movie.getName();
        holder.txtTitle.setText(displayText);

        // Lógica de Subtítulo
        if ("person".equals(movie.getMediaType())) {
            holder.txtYear.setText(obtenerRol(movie));
            holder.txtYear.setVisibility(View.VISIBLE);
        } else {
            String dateStr = movie.getReleaseDate();
            holder.txtYear.setVisibility(View.VISIBLE);

            if (dateStr == null || dateStr.isEmpty()) {
                holder.txtYear.setText(context.getString(R.string.coming_soon));
            } else {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                try {
                    java.util.Date releaseDate = sdf.parse(dateStr);
                    java.util.Date today = new java.util.Date();

                    if (releaseDate != null && releaseDate.after(today)) {
                        String year = (dateStr.length() >= 4) ? dateStr.substring(0, 4) : "";
                        if (!year.isEmpty()) {
                            holder.txtYear.setText(context.getString(R.string.coming_soon) + " (" + year + ")");
                        } else {
                            holder.txtYear.setText(R.string.coming_soon);
                        }
                    } else {
                        if (dateStr.length() >= 4) {
                            holder.txtYear.setText(dateStr.substring(0, 4));
                        } else {
                            holder.txtYear.setText("");
                        }
                    }
                } catch (java.text.ParseException e) {
                    if (dateStr.length() >= 4) {
                        holder.txtYear.setText(dateStr.substring(0, 4));
                    } else {
                        holder.txtYear.setText("");
                    }
                }
            }
        }

        // Lógica de Imagen
        String posterUrl = movie.getFullPosterUrl();

        if (posterUrl != null && !posterUrl.isEmpty()) {
            holder.imgPoster.setScaleType(ImageView.ScaleType.FIT_XY);
            holder.imgPoster.setPadding(0, 0, 0, 0);
            holder.imgPoster.setBackground(null);
            holder.imgPoster.clearColorFilter();

            Glide.with(context)
                    .load(posterUrl)
                    .placeholder(R.color.white)
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            int paddingDp = 20;
            float density = context.getResources().getDisplayMetrics().density;
            int paddingPixel = (int) (paddingDp * density);

            holder.imgPoster.setPadding(paddingPixel, paddingPixel, paddingPixel, paddingPixel);
            holder.imgPoster.setImageResource(R.drawable.moviening_icon);
        }

        // Configuración de Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(movie);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMovieLongClick(movie, holder.itemView);
                return true;
            }
            return false;
        });
    }

    /**
     * Determina el rol de una persona basándose en su departamento y género.
     */
    private String obtenerRol(Movie p) {
        String dept = p.getKnownForDepartment();
        int gender = p.getGender();
        String rol = "";

        if ("Acting".equals(dept)) {
            if (gender == 1) rol = context.getString(R.string.actress);
            else rol = context.getString(R.string.actor);
        } else if ("Directing".equals(dept)) {
            if (gender == 1) rol = context.getString(R.string.female_director);
            else rol = context.getString(R.string.male_director);
        } else {
            rol = dept != null ? dept : "";
        }
        return rol;
    }

    @Override
    public int getItemCount() {
        return movieList != null ? movieList.size() : 0;
    }

    public List<Movie> getMovies() {
        return movieList;
    }

    /**
     * ViewHolder que mantiene las referencias a las vistas de cada elemento.
     */
    public static class MovieViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtYear;
        ImageView imgPoster;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_movie_title);
            txtYear = itemView.findViewById(R.id.txt_movie_year);
            imgPoster = itemView.findViewById(R.id.img_movie_poster);
        }
    }
}