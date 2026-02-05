package com.amgoapps.moviening;

import java.util.Arrays;
import java.util.List;
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
     * <li>Portugués (retorna {@code "pt-BR"} o {@code "pt-PT"} según el país)</li>
     * <li>Italiano (retorna {@code "it-IT"})</li>
     * <li>Alemán (retorna {@code "de-DE"})</li>
     * <li>Francés (retorna {@code "fr-FR"})</li>
     * <li>Inglés ({@code "en-US"} o {@code "en-GB"} según el país)</li>
     * </ul>
     *
     * @return Un {@code String} que representa el código de idioma-país (ej. "es-ES").
     */
    public static String getApiLanguage() {
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();

        switch (language) {
            case "es":
                if (country.equals("ES")) return "es-ES";
                else return "es-MX";

            case "pt":
                if (country.equals("BR")) return "pt-BR";
                else return "pt-PT";

            case "en":
                List<String> britishGroup = Arrays.asList("GB", "IE", "AU", "NZ", "ZA", "IN", "SG", "MY");

                if (britishGroup.contains(country)) {
                    return "en-GB";
                } else {
                    return "en-US";
                }

            case "it": return "it-IT";
            case "de": return "de-DE";
            case "fr": return "fr-FR";
            default: return "en-US";
        }
    }
}