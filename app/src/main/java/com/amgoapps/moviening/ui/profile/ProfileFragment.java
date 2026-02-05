package com.amgoapps.moviening.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.amgoapps.moviening.LoginActivity;
import com.amgoapps.moviening.MovieListActivity;
import com.amgoapps.moviening.MyReviewsActivity;
import com.amgoapps.moviening.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.jsibbold.zoomage.ZoomageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragmento que gestiona el perfil del usuario.
 * Funcionalidades principales:
 * 1. Visualización y edición de datos del usuario.
 * 2. Gestión de listas de películas (Favoritas, Vistas, Pendientes, Reseñas y personalizadas).
 * 3. Integración con Firebase.
 */
public class ProfileFragment extends Fragment {

    private ImageView imgProfileSmall;
    private TextView txtName, txtEmail, txtHeaderProfile;
    private RecyclerView recyclerLists;

    private FrameLayout overlayContainer;
    private ZoomageView imgFullScreen;
    private TextView txtNoPhoto;
    private Button btnUploadPhoto, btnDeletePhoto;
    private ImageButton btnCloseOverlay;
    private ProgressBar progressUpload;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private DatabaseReference mListsDbRef;
    private DatabaseReference mUsernamesDbRef;

    private ValueEventListener listsListener;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ListsAdapter listsAdapter;
    private List<String> allLists;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mUsernamesDbRef = FirebaseDatabase.getInstance().getReference("usernames");

        // Inicializamos la lista con las fijas para que se vean de inmediato
        allLists = new ArrayList<>();
        allLists.add("Favorites");
        allLists.add("Watched");
        allLists.add("Watchlist");
        allLists.add("Reviews");

        if (mAuth.getCurrentUser() != null) {
            mListsDbRef = FirebaseDatabase.getInstance().getReference("users")
                    .child(mAuth.getCurrentUser().getUid()).child("lists");
            mListsDbRef.keepSynced(true);
            configurarListenerListas();
        }
    }

    /**
     * Inicializa la vista del fragmento, configura Firebase y los listeners de UI.
     */
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        imgProfileSmall = root.findViewById(R.id.img_profile_small);
        txtName = root.findViewById(R.id.txt_profile_name);
        txtEmail = root.findViewById(R.id.txt_profile_email);
        txtHeaderProfile = root.findViewById(R.id.txt_header_profile);
        recyclerLists = root.findViewById(R.id.recycler_lists);

        overlayContainer = root.findViewById(R.id.overlay_container);
        imgFullScreen = root.findViewById(R.id.img_full_screen);
        txtNoPhoto = root.findViewById(R.id.txt_no_photo);
        btnUploadPhoto = root.findViewById(R.id.btn_upload_photo);
        btnDeletePhoto = root.findViewById(R.id.btn_delete_photo);
        btnCloseOverlay = root.findViewById(R.id.btn_close_overlay);
        progressUpload = root.findViewById(R.id.progress_upload);

        listsAdapter = new ListsAdapter(allLists);
        recyclerLists.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerLists.setAdapter(listsAdapter);

        cargarDatosUsuario();

        txtHeaderProfile.setOnClickListener(v -> mostrarMenuOpciones());
        imgProfileSmall.setOnClickListener(v -> mostrarOverlay());
        btnCloseOverlay.setOnClickListener(v -> cerrarOverlay());
        btnUploadPhoto.setOnClickListener(v -> abrirGaleria());
        btnDeletePhoto.setOnClickListener(v -> borrarFotoPerfil());

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) subirFotoAFirebase(imageUri);
                    }
                }
        );

        return root;
    }

    /**
     * Configurar los listeners de las listas.
     */
    private void configurarListenerListas() {
        listsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allLists.clear();

                allLists.add("Favorites");
                allLists.add("Watched");
                allLists.add("Watchlist");
                allLists.add("Reviews");

                for (DataSnapshot data : snapshot.getChildren()) {
                    String key = data.getKey();
                    if (key != null && !esListaFija(key)) {
                        allLists.add(key);
                    }
                }

                if (listsAdapter != null) {
                    listsAdapter.notifyDataSetChanged();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        mListsDbRef.addValueEventListener(listsListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mListsDbRef != null && listsListener != null) {
            mListsDbRef.removeEventListener(listsListener);
        }
    }

    /**
     * Limpia los listeners de Firebase para evitar fugas de memoria cuando la vista se destruye.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    // =========================================================================
    // MENÚS, CUADROS DE DIÁLOGOS Y SU FUNCIONALIDAD
    // =========================================================================

    /**
     * Muestra un menú emergente para cambiar nombre o cerrar sesión.
     */
    private void mostrarMenuOpciones() {
        View menuView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_profile_menu, null);

        float density = getResources().getDisplayMetrics().density;
        int widthPixels = (int) (250 * density);
        menuView.measure(
                View.MeasureSpec.makeMeasureSpec(widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        int popupWidth = menuView.getMeasuredWidth();
        int[] location = new int[2];
        txtHeaderProfile.getLocationOnScreen(location);
        int x = location[0] + (txtHeaderProfile.getWidth() / 2) - (popupWidth / 2);
        int y = location[1] + txtHeaderProfile.getHeight();

        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                menuView,
                widthPixels,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setElevation(20);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        menuView.findViewById(R.id.option_change_name).setOnClickListener(v -> {
            popupWindow.dismiss();
            mostrarDialogoCambiarNombre();
        });

        menuView.findViewById(R.id.option_logout).setOnClickListener(v -> {
            popupWindow.dismiss();
            mostrarDialogoCerrarSesion();
        });

        popupWindow.showAtLocation(txtHeaderProfile, android.view.Gravity.NO_GRAVITY, x, y);
    }

    /**
     * Muestra un diálogo de confirmación antes de cerrar la sesión del usuario.
     */
    private void mostrarDialogoCerrarSesion() {
        new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.logout_confirm))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Muestra un cuadro de diálogo para que el usuario ingrese un nuevo nombre de usuario.
     */
    private void mostrarDialogoCambiarNombre() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.change_username));

        final EditText input = new EditText(getContext());
        input.setHint(getString(R.string.new_name));
        if (mAuth.getCurrentUser() != null) {
            input.setText(mAuth.getCurrentUser().getDisplayName());
        }

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, 0, margin, 0);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {});
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nuevoNombre = input.getText().toString().trim();
            if (nuevoNombre.isEmpty()) {
                input.setError(getString(R.string.error_username));
                return;
            }
            if (nuevoNombre.equals(mAuth.getCurrentUser().getDisplayName())) {
                dialog.dismiss();
                return;
            }
            verificarYActualizarUsuario(nuevoNombre, dialog);
        });
    }

    /**
     * Comprueba en la base de datos si el nombre de usuario ya existe.
     */
    private void verificarYActualizarUsuario(String nuevoNombre, AlertDialog dialog) {
        mUsernamesDbRef.child(nuevoNombre).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(getContext(), getString(R.string.username_taken), Toast.LENGTH_SHORT).show();
                } else {
                    actualizarNombreReal(nuevoNombre, dialog);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), getString(R.string.error_conexion), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Realiza la actualización del perfil en Auth y en la base de datos de nombres.
     */
    private void actualizarNombreReal(String nuevoNombre, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String nombreAntiguo = user.getDisplayName();
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(nuevoNombre).build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Reservar el nuevo nombre y borrar el antiguo
                mUsernamesDbRef.child(nuevoNombre).setValue(user.getUid());
                if (nombreAntiguo != null && !nombreAntiguo.isEmpty()) {
                    mUsernamesDbRef.child(nombreAntiguo).removeValue();
                }

                actualizarNombreEnResenas(nuevoNombre);

                cargarDatosUsuario();
                Toast.makeText(getContext(), getString(R.string.username_updated), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), getString(R.string.error_username_updating), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Recorre la lista de películas 'Vistas' (Watched) del usuario.
     * Si encuentra una reseña para esa película, actualiza el campo 'username'.
     */
    private void actualizarNombreEnResenas(String nuevoNombre) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        DatabaseReference watchedRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("lists").child("Watched");

        watchedRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                for (DataSnapshot movieSnap : snapshot.getChildren()) {
                    String movieId = movieSnap.getKey();

                    if (movieId != null) {
                        DatabaseReference reviewRef = FirebaseDatabase.getInstance()
                                .getReference("reviews").child(movieId).child(uid);

                        reviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot reviewSnap) {
                                if (reviewSnap.exists()) {
                                    reviewRef.child("username").setValue(nuevoNombre);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Muestra el diálogo que permite modificar el título de una lista personalizada.
     */
    private void mostrarDialogoEditarLista(String actual) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.edit));

        final EditText input = new EditText(getContext());
        input.setText(actual);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, 0, margin, 0);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(getString(R.string.save), (d, w) -> {
            String n = input.getText().toString().trim();
            if (!n.isEmpty() && !n.equals(actual)) {
                mListsDbRef.child(actual).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put(n, s.getValue());
                        updates.put(actual, null);
                        mListsDbRef.updateChildren(updates);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    /**
     * Muestra el diálogo que permite al usuario el nombre de la nueva lista personalizada.
     */
    private void mostrarDialogoNuevaLista() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.new_list));

        final EditText input = new EditText(getContext());
        input.setHint(getString(R.string.name_list));
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});

        FrameLayout container = new FrameLayout(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, 0, margin, 0);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(getString(R.string.create), (d, w) -> {
            String n = input.getText().toString().trim();
            if (!n.isEmpty()) {
                if (esListaFija(n))
                    Toast.makeText(getContext(), getString(R.string.name_list_reserved), Toast.LENGTH_SHORT).show();
                else crearListaEnFirebase(n);
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    // =========================================================================
    // FOTOS Y OVERLAY
    // =========================================================================

    /**
     * Muestra la foto de perfil en pantalla completa con opciones para editarla.
     */
    private void mostrarOverlay() {
        FirebaseUser user = mAuth.getCurrentUser();
        overlayContainer.setAlpha(0f);
        overlayContainer.setVisibility(View.VISIBLE);
        overlayContainer.animate().alpha(1f).setDuration(300).start();

        if (user != null && user.getPhotoUrl() != null) {
            imgFullScreen.setVisibility(View.VISIBLE);
            txtNoPhoto.setVisibility(View.GONE);
            btnDeletePhoto.setVisibility(View.VISIBLE);
            Glide.with(this).load(user.getPhotoUrl()).into(imgFullScreen);
        } else {
            imgFullScreen.setVisibility(View.GONE);
            txtNoPhoto.setVisibility(View.VISIBLE);
            btnDeletePhoto.setVisibility(View.GONE);
        }
    }

    /**
     * Elimina la foto de perfil actual de Firebase Storage.
     */
    private void borrarFotoPerfil() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getPhotoUrl() == null) return;
        progressUpload.setVisibility(View.VISIBLE);
        try {
            FirebaseStorage.getInstance().getReferenceFromUrl(user.getPhotoUrl().toString())
                    .delete().addOnCompleteListener(task -> actualizarPerfilSinFoto());
        } catch (Exception e) { actualizarPerfilSinFoto(); }
    }

    /**
     * Actualiza el perfil del usuario para eliminar la referencia a la foto.
     */
    private void actualizarPerfilSinFoto() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder().setPhotoUri(null).build();
        user.updateProfile(request).addOnCompleteListener(task -> {
            progressUpload.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), getString(R.string.profile_picture_deleted), Toast.LENGTH_SHORT).show();
                imgProfileSmall.setImageResource(R.drawable.baseline_camera_alt_24);
                cerrarOverlay();
            }
        });
    }

    /**
     * Sube la imagen seleccionada a Firebase Storage.
     */
    private void subirFotoAFirebase(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser(); if(user==null) return;
        progressUpload.setVisibility(View.VISIBLE);
        StorageReference ref = mStorageRef.child("profile_images/"+user.getUid()+".jpg");
        ref.putFile(imageUri).addOnSuccessListener(t-> ref.getDownloadUrl().addOnSuccessListener(this::actualizarPerfilAuth))
                .addOnFailureListener(e->progressUpload.setVisibility(View.GONE));
    }

    /**
     * Actualiza la URL de la foto en Auth y en la base de datos Database.
     */
    private void actualizarPerfilAuth(Uri uri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.updateProfile(new UserProfileChangeRequest.Builder().setPhotoUri(uri).build())
                .addOnSuccessListener(v -> {

                    // Actualizar en Realtime Database (para acceso público)
                    FirebaseDatabase.getInstance().getReference("users")
                            .child(user.getUid())
                            .child("photoUrl")
                            .setValue(uri.toString());

                    progressUpload.setVisibility(View.GONE);
                    cargarDatosUsuario();
                    cerrarOverlay();
                    Toast.makeText(getContext(), R.string.profile_picture_updated, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressUpload.setVisibility(View.GONE);
                    Toast.makeText(getContext(), R.string.error_profile_picture_updating, Toast.LENGTH_SHORT).show();
                });
    }

    // =========================================================================
    // LISTAS Y ADAPTADOR
    // =========================================================================

    /**
     * Adaptador personalizado para gestionar la visualización de listas de películas.
     * Maneja dos tipos de vista: Elemento de lista y Botón de "Añadir".
     */
    private class ListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_ADD_BUTTON = 1;
        private List<String> lists;

        public ListsAdapter(List<String> lists) { this.lists = lists; }

        @Override public int getItemViewType(int position) { return (position == lists.size()) ? TYPE_ADD_BUTTON : TYPE_ITEM; }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_list, parent, false));
            } else {
                return new AddButtonViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_list, parent, false));
            }
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_ITEM) {
                ItemViewHolder h = (ItemViewHolder) holder;
                String listName = lists.get(position);
                h.txtListName.setText(listName);

                Context context = h.itemView.getContext();
                String displayTitle = listName;

                if (listName.equals("Reviews")) {
                    h.txtListName.setText(context.getString(R.string.reviews));
                    h.imgIcon.setImageResource(R.drawable.ic_star_outline);
                    h.imgIcon.setColorFilter(ContextCompat.getColor(context, R.color.purple), PorterDuff.Mode.SRC_IN);
                    h.btnDelete.setVisibility(View.GONE);
                    h.itemView.setOnLongClickListener(null);

                    h.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(context, MyReviewsActivity.class);
                        context.startActivity(intent);
                    });
                    return;
                }

                if (listName.equals("Favorites")) {
                    h.txtListName.setText(context.getString(R.string.favorites));
                    displayTitle = context.getString(R.string.favorites);
                    h.imgIcon.setImageResource(R.drawable.ic_heart);
                    h.btnDelete.setVisibility(View.GONE);
                    h.itemView.setOnLongClickListener(null);
                } else if (listName.equals("Watched")) {
                    h.txtListName.setText(context.getString(R.string.watched));
                    displayTitle = context.getString(R.string.watched);
                    h.imgIcon.setImageResource(R.drawable.ic_eye);
                    h.btnDelete.setVisibility(View.GONE);
                    h.itemView.setOnLongClickListener(null);
                } else if (listName.equals("Watchlist")) {
                    h.txtListName.setText(context.getString(R.string.watchlist));
                    displayTitle = context.getString(R.string.watchlist);
                    h.imgIcon.setImageResource(R.drawable.ic_clock);
                    h.btnDelete.setVisibility(View.GONE);
                    h.itemView.setOnLongClickListener(null);
                } else {
                    h.imgIcon.setImageResource(R.drawable.ic_list);
                    h.btnDelete.setVisibility(View.VISIBLE);
                    h.btnDelete.setOnClickListener(v -> borrarListaDeFirebase(listName));
                    h.itemView.setOnLongClickListener(v -> {
                        mostrarDialogoEditarLista(listName);
                        return true;
                    });
                }

                String finalDisplayTitle = displayTitle;
                h.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, MovieListActivity.class);
                    intent.putExtra("LIST_NAME", listName);
                    intent.putExtra("LIST_TITLE", finalDisplayTitle);
                    context.startActivity(intent);
                });

            } else {
                ((AddButtonViewHolder) holder).itemView.setOnClickListener(v -> mostrarDialogoNuevaLista());
            }
        }
        @Override public int getItemCount() { return lists.size() + 1; }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView txtListName; ImageView imgIcon, btnDelete;
            ItemViewHolder(View v) { super(v); txtListName=v.findViewById(R.id.txt_list_name); imgIcon=v.findViewById(R.id.img_list_icon); btnDelete=v.findViewById(R.id.btn_delete_list); }
        }
        class AddButtonViewHolder extends RecyclerView.ViewHolder { AddButtonViewHolder(View v) { super(v); } }
    }

    /**
     * Crea la lista en Firebase.
     */
    private void crearListaEnFirebase(String n) { if(mListsDbRef!=null) mListsDbRef.child(n).setValue("created"); }

    /**
     * Borra la lista en Firebase.
     */
    private void borrarListaDeFirebase(String n) { new AlertDialog.Builder(getContext()).setTitle(getString(R.string.delete)).setMessage(getString(R.string.delete_list) + " '"+n+"'?").setPositiveButton(getString(R.string.yes), (d,w)->mListsDbRef.child(n).removeValue()).setNegativeButton(getString(R.string.no),null).show(); }

    /**
     * Actualiza la UI con los datos del usuario actual de Firebase y la base de datos.
     */
    private void cargarDatosUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            txtName.setText(user.getDisplayName() != null ? user.getDisplayName() : getString(R.string.user));
            txtEmail.setText(user.getEmail());

            FirebaseDatabase.getInstance().getReference("users")
                    .child(user.getUid())
                    .child("photoUrl")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            String url = task.getResult().getValue(String.class);
                            if (url != null && isAdded()) {
                                Glide.with(this).load(url).circleCrop().into(imgProfileSmall);
                            }
                        } else {
                            if (isAdded()) {
                                imgProfileSmall.setImageResource(R.drawable.baseline_camera_alt_24);
                            }
                        }
                    });
        }
    }

    // --- Otros métodos ---
    /**
     * Verifica si una lista es fija (del sistema) independientemente del idioma.
     * Esto evita duplicados en la sección de listas personalizadas.
     */
    private boolean esListaFija(String nombre) {
        return nombre.equals("Favorites") ||
                nombre.equals("Watched") ||
                nombre.equals("Watchlist") ||
                nombre.equals("Reviews");
    }

    private void cerrarOverlay() { overlayContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> overlayContainer.setVisibility(View.GONE)).start(); }

    private void abrirGaleria() { Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI); galleryLauncher.launch(intent); }

}