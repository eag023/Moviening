package com.amgoapps.moviening;

import java.util.Locale;

/**
 * Clase de utilidad para gestionar la configuración de idioma y localización.
 * <p>
 * Se encarga de formatear los códigos de idioma basándose en la configuración
 * del dispositivo para realizar peticiones correctas a la API externa.
 * </p>
 */
public class LanguageUtils {

    /**
     * Obtiene el código de idioma y región formateado para las peticiones a la API.
     *
     * <p>El método consulta el idioma por defecto del dispositivo mediante {@link Locale#getDefault()}.
     * Actualmente soporta:</p>
     * <ul>
     * <li>Español (retorna {@code "es-ES"} o {@code "es-MX"} según el país)</li>
     * <li>Italiano (retorna {@code "it-IT"})</li>
     * <li>Alemán (retorna {@code "de-DE"})</li>
     * <li>Francés (retorna {@code "fr-FR"})</li>
     * </ul>
     * <p>Si el idioma del dispositivo no es ninguno de los anteriores, se utiliza
     * Inglés ({@code "en-US"}) como idioma de respaldo (fallback).</p>
     *
     * @return Un {@code String} que representa el código de idioma-país (ej. "es-ES").
     */
    public static String getApiLanguage() {
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();

        switch (language) {
            case "es":
                if (country.equals("ES")) {
                    return "es-ES";
                } else {
                    return "es-MX";
                }
            case "it":
                return "it-IT";
            case "de":
                return "de-DE";
            case "fr":
                return "fr-FR";
            default:
                return "en-US";
        }
    }
}