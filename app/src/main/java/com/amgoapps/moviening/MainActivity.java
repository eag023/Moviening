package com.amgoapps.moviening;

import android.os.Bundle;
import android.view.View;

import com.amgoapps.moviening.ui.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.amgoapps.moviening.databinding.ActivityMainBinding;

/**
 * Actividad Principal de la aplicación.
 * Actúa como contenedor para el componente de navegación y gestiona
 * la barra de navegación inferior.
 * * Funcionalidades clave:
 * 1. Configuración del Navigation Controller.
 * 2. Lógica personalizada para el botón de búsqueda (clic simple vs clic largo).
 * 3. Gestión centralizada del botón "Atrás" para redirigir el flujo de navegación.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_recomend, R.id.nav_search, R.id.nav_profile)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        NavigationUI.setupWithNavController(binding.navView, navController);

        // ========================================================================
        // CÓDIGO DE NAVEGACIÓN
        // ========================================================================

        binding.navView.setOnItemSelectedListener(item -> {
            boolean navigated = NavigationUI.onNavDestinationSelected(item, navController);
            // Si se pulsa "Buscar" (incluso si ya estamos ahí), limpiamos el texto
            if (item.getItemId() == R.id.nav_search) {
                buscarYLimpiarFragmento();
            }
            return navigated;
        });

        // Configuración de pulsación larga en el botón de Búsqueda
        View searchButtonView = binding.navView.findViewById(R.id.nav_search);
        if (searchButtonView != null) {
            searchButtonView.setOnLongClickListener(v -> {
                // Si ya estamos en la pantalla de búsqueda, poner foco en la barra de búsqueda
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == R.id.nav_search) {
                    buscarYPonerCursor();
                }
                // Si estamos en otra pantalla, navegar a búsqueda y poner foco en la barra de búsqueda
                else {
                    Bundle args = new Bundle();
                    args.putBoolean("PONER_CURSOR", true);
                    navController.navigate(R.id.nav_search, args);
                }
                return true;
            });
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int currentId = navController.getCurrentDestination() != null
                        ? navController.getCurrentDestination().getId()
                        : -1;

                // Si estamos en Perfil, ir a Búsqueda
                if (currentId == R.id.nav_profile) {
                    navView.setSelectedItemId(R.id.nav_search);
                }
                else {
                    // Si ya estamos en Búsqueda, cerrar app
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        if (getIntent().getBooleanExtra("SHOW_GUIDE", false)) {
            binding.navView.post(() -> {
                InteractiveGuideManager guia = new InteractiveGuideManager(this);
                guia.iniciarGuia();
            });
        }
    }

    /**
     * Busca la instancia activa de SearchFragment en el NavHost y limpia el buscador.
     */
    private void buscarYLimpiarFragmento() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment != null) {
            for (Fragment fragment : navHostFragment.getChildFragmentManager().getFragments()) {
                if (fragment instanceof SearchFragment) {
                    ((SearchFragment) fragment).limpiarBuscador();
                    break;
                }
            }
        }
    }

    /**
     * Busca la instancia activa de SearchFragment y solicita el foco/teclado.
     */
    private void buscarYPonerCursor() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment != null) {
            for (Fragment fragment : navHostFragment.getChildFragmentManager().getFragments()) {
                if (fragment instanceof SearchFragment) {
                    ((SearchFragment) fragment).ponerCursorEnBarra();
                    break;
                }
            }
        }
    }
}