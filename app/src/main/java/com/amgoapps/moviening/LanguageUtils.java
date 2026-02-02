package com.amgoapps.moviening;

import java.util.Locale;

public class LanguageUtils {
    public static String getApiLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (language.equals("es")) {
            return "es-ES";
        } else {
            return "en-US";
        }
    }
}