package com.apasoft.csp.ui.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.apasoft.csp.R;
import com.apasoft.csp.ui.main.MainActivity;

/**
 * Pantalla de bienvenida (splash screen) de la aplicación.
 *
 * <p>Se muestra durante {@value #SPLASH_DURATION_MS} ms antes de navegar
 * automáticamente a {@link MainActivity}. La transición usa un fundido cruzado
 * ({@code fade_in} / {@code fade_out}) para suavizar el cambio de pantalla.</p>
 *
 * <p>La anotación {@code @SuppressLint("CustomSplashScreen")} suprime la advertencia
 * de Lint que recomienda usar la API de Splash Screen de Android 12+ ({@code SplashScreen});
 * se mantiene la implementación manual para compatibilidad con versiones anteriores.</p>
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    /** Duración de la pantalla de bienvenida en milisegundos. */
    private static final long SPLASH_DURATION_MS = 2200;

    /**
     * Infla el layout de splash y programa la navegación a {@link MainActivity}
     * tras {@value #SPLASH_DURATION_MS} ms en el hilo principal.
     *
     * @param savedInstanceState estado guardado de la instancia (no utilizado)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, SPLASH_DURATION_MS);
    }
}