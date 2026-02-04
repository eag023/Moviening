package com.amgoapps.moviening;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Gestor encargado de crear y controlar una guía interactiva (tutorial) para el usuario.
 * Permite resaltar vistas específicas mediante un recorte en un overlay oscuro y mostrar
 * mensajes explicativos con indicadores visuales.
 */
public class InteractiveGuideManager {

    private final Activity activity;
    private FrameLayout overlayLayout;
    private View bubbleView;
    private ImageView arrowView;
    private TextView txtBubble, txtTouch;
    private int step = 1;

    private View currentTarget;
    private final Paint eraserPaint;

    /**
     * Constructor de la clase. Inicializa el pincel con modo CLEAR para realizar
     * el efecto de recorte (hollow) sobre el overlay.
     * * @param activity Referencia a la actividad donde se mostrará la guía.
     */
    public InteractiveGuideManager(Activity activity) {
        this.activity = activity;
        this.eraserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    /**
     * Prepara e infla la interfaz de la guía, configura el overlay con dibujo personalizado
     * y lo añade a la ventana principal de la aplicación.
     */
    public void iniciarGuia() {
        overlayLayout = new FrameLayout(activity) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                int grayColor = ContextCompat.getColor(activity, R.color.dark_gray);
                int overlayColor = (grayColor & 0x00FFFFFF) | (0xB3000000);
                canvas.drawColor(overlayColor);

                if (currentTarget != null) {
                    int[] loc = new int[2];
                    currentTarget.getLocationInWindow(loc);

                    RectF rect = new RectF(
                            loc[0] - 15,
                            loc[1] - 15,
                            loc[0] + currentTarget.getWidth() + 15,
                            loc[1] + currentTarget.getHeight() + 15
                    );
                    canvas.drawRoundRect(rect, 30, 30, eraserPaint);
                }
                super.dispatchDraw(canvas);
            }
        };

        overlayLayout.setClickable(true);
        overlayLayout.setFocusable(true);
        overlayLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        overlayLayout.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bubbleView = LayoutInflater.from(activity).inflate(R.layout.layout_bubble_guide, overlayLayout, false);
        txtBubble = bubbleView.findViewById(R.id.txt_bubble_content);
        txtTouch = bubbleView.findViewById(R.id.txt_touch_continue);
        txtTouch.setText(activity.getString(R.string.touch));

        arrowView = new ImageView(activity);
        arrowView.setImageResource(R.drawable.ic_arrow_guide);
        arrowView.setLayoutParams(new FrameLayout.LayoutParams(120, 120));
        arrowView.setVisibility(View.GONE);

        overlayLayout.addView(bubbleView);
        overlayLayout.addView(arrowView);

        ((ViewGroup) activity.getWindow().getDecorView()).addView(overlayLayout);

        overlayLayout.setOnClickListener(v -> siguientePaso());
        mostrarPaso();
    }

    /**
     * Controla el flujo de la guía basándose en el valor de la variable 'step'.
     * Ejecuta las animaciones de transición entre textos y objetivos.
     */
    private void mostrarPaso() {
        bubbleView.animate().cancel();
        bubbleView.setAlpha(0f);

        switch (step) {
            case 1:
                currentTarget = null;
                configurarBurbuja(activity.getString(R.string.start_guide), Gravity.CENTER, 0, 0, false);
                break;
            case 2:
                apuntarAView(activity.findViewById(R.id.nav_search), activity.getString(R.string.search_guide));
                break;
            case 3:
                apuntarAView(activity.findViewById(R.id.nav_recomend), activity.getString(R.string.recommend_guide));
                break;
            case 4:
                apuntarAView(activity.findViewById(R.id.nav_profile), activity.getString(R.string.profile_guide));
                break;
            case 5:
                RecyclerView rv = activity.findViewById(R.id.recycler_search);
                View firstItem = (rv != null) ? rv.getChildAt(0) : null;
                if (firstItem != null) {
                    apuntarAView(firstItem, activity.getString(R.string.movie_guide));
                } else { saltarPasoInmediato(); return; }
                break;
            case 6:
                apuntarAView(activity.findViewById(R.id.search_view), activity.getString(R.string.search_bar_guide));
                break;
            case 7:
                apuntarAView(activity.findViewById(R.id.btn_filter), activity.getString(R.string.filters_guide));
                break;
            case 8:
                currentTarget = null;
                arrowView.setVisibility(View.GONE);
                configurarBurbuja(activity.getString(R.string.end_guide), Gravity.CENTER, 0, 0, false);
                break;
            case 9:
                finalizarGuia();
                return;
        }

        overlayLayout.invalidate();
        bubbleView.animate().alpha(1f).setDuration(150).start();
    }

    /**
     * Configura la posición y el contenido visual de la burbuja de texto informativa.
     * * @param texto El mensaje a mostrar.
     * @param gravity La gravedad dentro del FrameLayout (ej. Gravity.CENTER o Gravity.TOP).
     * @param x Desplazamiento horizontal (no utilizado directamente en esta lógica pero reservado).
     * @param y Margen superior para posicionar la burbuja verticalmente.
     * @param conFlecha Determina si se debe ocultar la flecha indicadora.
     */
    private void configurarBurbuja(String texto, int gravity, int x, int y, boolean conFlecha) {
        txtBubble.setText(texto);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bubbleView.getLayoutParams();

        params.gravity = gravity;
        if ((gravity & Gravity.CENTER) == Gravity.CENTER) {
            params.topMargin = 0;
        } else {
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.topMargin = y;
        }

        bubbleView.setLayoutParams(params);
        if (!conFlecha) arrowView.setVisibility(View.GONE);
    }

    /**
     * Calcula la posición de una vista objetivo en pantalla para resaltar su ubicación,
     * colocar la flecha indicadora y posicionar la burbuja de texto de forma adyacente.
     * * @param target La vista (View) que se desea resaltar.
     * @param texto El mensaje explicativo relacionado con dicha vista.
     */
    private void apuntarAView(View target, String texto) {
        if (target == null) { saltarPasoInmediato(); return; }
        this.currentTarget = target;

        int[] location = new int[2];
        target.getLocationInWindow(location);
        int targetY = location[1];
        int targetHeight = target.getHeight();
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;

        arrowView.setVisibility(View.VISIBLE);
        arrowView.setX(location[0] + (target.getWidth() / 2f) - 60);

        boolean estaEnParteSuperior = targetY < (screenHeight / 2);

        if (estaEnParteSuperior) {
            arrowView.setRotation(180);
            arrowView.setY(targetY + targetHeight + 30);
            configurarBurbuja(texto, Gravity.TOP, 0, targetY + targetHeight + 200, true);
        } else {
            arrowView.setRotation(0);
            arrowView.setY(targetY - 140);
            configurarBurbuja(texto, Gravity.TOP, 0, targetY - 750, true);
        }

        arrowView.animate().cancel();
        ObjectAnimator animator = ObjectAnimator.ofFloat(arrowView, "translationY", arrowView.getY() - 15, arrowView.getY() + 15);
        animator.setDuration(400);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.start();
    }

    /**
     * Incrementa el contador de pasos y dispara la actualización de la interfaz de la guía.
     */
    private void siguientePaso() {
        step++;
        mostrarPaso();
    }

    /**
     * Realiza una transición de salida y elimina el overlay de la jerarquía de vistas
     * para dar por concluido el tutorial.
     */
    private void finalizarGuia() {
        overlayLayout.setOnClickListener(null);
        overlayLayout.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            decorView.removeView(overlayLayout);
        }).start();
    }

    /**
     * Avanza al siguiente paso de forma inmediata sin esperar a que terminen las animaciones activas.
     */
    private void saltarPasoInmediato() {
        step++;
        mostrarPaso();
    }
}