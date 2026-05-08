package com.apasoft.csp.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apasoft.csp.R;
import com.apasoft.csp.domain.CuttingStockSolver;
import com.apasoft.csp.domain.RemnantCuttingStockSolver;
import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Solution;
import com.apasoft.csp.ui.history.HistoryActivity;
import com.apasoft.csp.ui.results.ResultsActivity;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

/**
 * Actividad principal de la aplicación CSP.
 *
 * <p>Permite al usuario configurar los parámetros del problema:
 * <ul>
 *   <li>Número de barras disponibles.</li>
 *   <li>Longitud de cada barra.</li>
 *   <li>Umbral de desperdicio aceptable (opcional).</li>
 *   <li>Lista de tipos de corte (longitud + cantidad requerida).</li>
 *   <li>Modo de resolución: con o sin reutilización de sobrantes.</li>
 * </ul>
 * </p>
 *
 * <p>Al pulsar "Calcular", la resolución se ejecuta en un hilo de fondo para
 * no bloquear el hilo principal. Durante el cálculo se muestra un overlay
 * de progreso. Al finalizar, los resultados se pasan a {@link ResultsActivity}.</p>
 */
public class MainActivity extends AppCompatActivity {

    /** Campo de texto para el número de barras disponibles. */
    private EditText etNumBars;
    /** Campo de texto para la longitud de cada barra. */
    private EditText etBarLength;
    /** Campo de texto para el umbral de desperdicio aceptable. */
    private EditText etWasteThreshold;
    /** Switch para activar/desactivar la reutilización de sobrantes. */
    private SwitchMaterial switchReuseRemnants;
    /** RecyclerView que muestra la lista de tipos de corte. */
    private RecyclerView rvCuts;
    /** Adaptador que gestiona las filas de tipos de corte. */
    private CutTypeAdapter cutTypeAdapter;

    /**
     * Inicializa la UI, conecta los listeners de los botones y configura el RecyclerView
     * de tipos de corte.
     *
     * @param savedInstanceState estado guardado (no utilizado explícitamente)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etNumBars        = findViewById(R.id.etNumBars);
        etBarLength      = findViewById(R.id.etBarLength);
        etWasteThreshold = findViewById(R.id.etWasteThreshold);
        switchReuseRemnants = findViewById(R.id.switchReuseRemnants);

        rvCuts = findViewById(R.id.rvCuts);
        rvCuts.setLayoutManager(new LinearLayoutManager(this));
        cutTypeAdapter = new CutTypeAdapter();
        rvCuts.setAdapter(cutTypeAdapter);

        findViewById(R.id.btnAddCut).setOnClickListener(v -> cutTypeAdapter.addEmptyRow());
        findViewById(R.id.btnHistory).setOnClickListener(
                v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btnInfo).setOnClickListener(v -> showInfoDialog());

        Button btnSolve = findViewById(R.id.btnSolve);
        btnSolve.setOnClickListener(v -> solve());
    }

    /**
     * Muestra un diálogo informativo con la descripción del algoritmo, el modo de uso
     * y las advertencias de rendimiento de la aplicación.
     */
    private void showInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("ℹ️ Acerca de esta app")
                .setMessage(
                        "CSP — 1D Cutting Stock Problem\n\n" +
                                "¿Qué hace?\n" +
                                "Calcula cómo cortar barras de longitud fija para obtener " +
                                "las piezas que necesitas, minimizando el material desperdiciado.\n\n" +
                                "¿Cómo funciona?\n" +
                                "• Sin sobrantes: genera todos los patrones posibles de corte " +
                                "y busca la combinación óptima mediante programación dinámica " +
                                "con fuerza bruta acotada.\n" +
                                "• Con sobrantes: construye un árbol de decisión que reutiliza " +
                                "los remanentes de cada barra para cortes posteriores " +
                                "(backtracking con poda).\n\n" +
                                "⚠️ Aviso de rendimiento\n" +
                                "La complejidad crece exponencialmente con el número de " +
                                "tipos de corte y de barras. Con más de 6-7 tipos distintos " +
                                "o más de 50 barras el cálculo puede tardar varios segundos. " +
                                "El modo 'Con sobrantes' es especialmente costoso. " +
                                "Empieza con pocos tipos de corte para obtener resultados rápidos."
                )
                .setPositiveButton("Entendido", null)
                .show();
    }

    /**
     * Valida los campos de entrada, construye el solver correspondiente y lanza
     * la resolución en un hilo de fondo.
     *
     * <p>Validaciones realizadas:
     * <ul>
     *   <li>Número de barras: no vacío y mayor que 0.</li>
     *   <li>Longitud de barra: no vacío y mayor que 0.</li>
     *   <li>Lista de tipos de corte: al menos un tipo válido.</li>
     * </ul>
     * </p>
     *
     * <p>Si la lista de soluciones resultante está vacía, se muestra un Toast
     * indicando que no se encontró solución. En caso contrario, se navega a
     * {@link ResultsActivity}.</p>
     */
    private void solve() {
        String numBarsStr   = etNumBars.getText().toString().trim();
        String barLengthStr = etBarLength.getText().toString().trim();

        if (numBarsStr.isEmpty()) {
            etNumBars.setError("Introduce el número de barras");
            etNumBars.requestFocus();
            return;
        }
        if (barLengthStr.isEmpty()) {
            etBarLength.setError("Introduce la longitud de barra");
            etBarLength.requestFocus();
            return;
        }

        final int    numBars   = Integer.parseInt(numBarsStr);
        final double barLength = Double.parseDouble(barLengthStr);

        if (numBars <= 0) {
            etNumBars.setError("Debe ser mayor que 0");
            etNumBars.requestFocus();
            return;
        }
        if (barLength <= 0) {
            etBarLength.setError("Debe ser mayor que 0");
            etBarLength.requestFocus();
            return;
        }

        final List<CutType> cuts = cutTypeAdapter.getCutTypes();
        if (cuts.isEmpty()) {
            Toast.makeText(this, "Añade al menos un tipo de corte", Toast.LENGTH_SHORT).show();
            return;
        }

        // Leer umbral de desperdicio personalizado, o usar el valor por defecto
        double wasteThreshold = CuttingStockSolver.DEFAULT_WASTE_THRESHOLD_MM;
        String thresholdStr   = etWasteThreshold.getText().toString().trim();
        if (!thresholdStr.isEmpty()) {
            try {
                wasteThreshold = Double.parseDouble(thresholdStr);
                if (wasteThreshold < 0) wasteThreshold = 0;
            } catch (NumberFormatException ignored) { }
        }
        final double finalWasteThreshold = wasteThreshold;
        final boolean reuseRemnants      = switchReuseRemnants.isChecked();

        // Mostrar overlay de progreso y ejecutar el solver en un hilo de fondo
        findViewById(R.id.progressOverlay).setVisibility(android.view.View.VISIBLE);

        new Thread(() -> {
            List<Solution> solutions;

            if (reuseRemnants) {
                RemnantCuttingStockSolver solver = new RemnantCuttingStockSolver(
                        numBars, barLength, cuts, finalWasteThreshold, true);
                solutions = solver.solve();
            } else {
                CuttingStockSolver solver = new CuttingStockSolver(
                        numBars, barLength, cuts, finalWasteThreshold);
                solutions = solver.solve();
            }

            final List<Solution> finalSolutions = solutions;
            runOnUiThread(() -> {
                findViewById(R.id.progressOverlay).setVisibility(android.view.View.GONE);
                if (finalSolutions.isEmpty()) {
                    Toast.makeText(MainActivity.this,
                            "No se encontró solución con los parámetros dados",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                ResultsActivity.start(MainActivity.this,
                        finalSolutions, barLength, numBars, cuts, reuseRemnants, false);
            });
        }).start();
    }
}