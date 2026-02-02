package com.amgoapps.moviening;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;

/**
 * Adaptador para mostrar una lista horizontal de personas (reparto o equipo técnico).
 * Utiliza un diseño circular para las imágenes de perfil y muestra el nombre real
 * junto con el nombre del personaje si está disponible.
 */
public class PersonHorizontalAdapter extends RecyclerView.Adapter<PersonHorizontalAdapter.PersonViewHolder> {

    private List<Movie> personList;
    private Context context;

    /**
     * Constructor del adaptador.
     * @param context Contexto de la aplicación.
     * @param personList Lista de objetos Movie que representan a las personas.
     */
    public PersonHorizontalAdapter(Context context, List<Movie> personList) {
        this.context = context;
        this.personList = personList;
    }

    /**
     * Infla el diseño circular para los elementos de la lista de personas.
     * @param parent El ViewGroup donde se alojará la nueva vista.
     * @param viewType El tipo de vista.
     * @return Una nueva instancia de PersonViewHolder.
     */
    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_person_circle, parent, false);
        return new PersonViewHolder(view);
    }

    /**
     * Vincula los datos de la persona (nombre, personaje y foto) con las vistas correspondientes.
     * @param holder El ViewHolder que debe ser actualizado.
     * @param position La posición del elemento en la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Movie person = personList.get(position);

        holder.txtName.setText(person.getName());

        if (person.getCharacter() != null && !person.getCharacter().isEmpty()) {
            holder.txtCharacter.setText(person.getCharacter());
            holder.txtCharacter.setVisibility(View.VISIBLE);
        } else {
            holder.txtCharacter.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(person.getFullPosterUrl())
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.placeholder_user)
                .into(holder.imgProfile);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PersonDetailActivity.class);
            intent.putExtra("PERSON_ID", person.getId());
            context.startActivity(intent);
        });
    }

    /**
     * Devuelve el número de elementos en la lista de personas.
     * @return Cantidad de elementos o 0 si la lista es nula.
     */
    @Override
    public int getItemCount() {
        return personList != null ? personList.size() : 0;
    }

    /**
     * Clase interna para mantener las referencias a los componentes visuales de cada persona.
     */
    public static class PersonViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView txtName;
        TextView txtCharacter;

        /**
         * Constructor del ViewHolder.
         * @param itemView Vista del elemento de la lista.
         */
        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.img_item_profile);
            txtName = itemView.findViewById(R.id.txt_item_name);
            txtCharacter = itemView.findViewById(R.id.txt_person_character);
        }
    }
}