package com.apasoft.csp.ui.results;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apasoft.csp.R;
import com.apasoft.csp.data.AppDatabase;
import com.apasoft.csp.data.ScenarioDao;
import com.apasoft.csp.data.ScenarioEntity;
import com.apasoft.csp.data.SolutionEntity;
import com.apasoft.csp.model.CutNode;
import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Solution;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Actividad que muestra las soluciones calculadas por el solver CSP.
 *
 * <p>Puede abrirse desde dos contextos:
 * <ul>
 *   <li><strong>Cálculo nuevo</strong> ({@code isFromHistory = false}): las cards
 *       muestran "Mantén pulsado para guardar". La acción de pulsación larga
 *       persiste la solución en Room.</li>
 *   <li><strong>Historial</strong> ({@code isFromHistory = true}): las cards muestran
 *       "Doble toque para exportar". La acción de doble toque abre el diálogo de
 *       exportación JSON/CSV.</li>
 * </ul>
 * </p>
 *
 * <p>Los datos entre actividades se pasan mediante campos estáticos en caché
 * ({@code sCached*}) en lugar de extras del Intent, dado que los objetos
 * {@link Solution} son demasiado grandes para ser serializados en un Bundle.</p>
 *
 * <p>La Gson configurada incluye un adaptador personalizado ({@link CutNodeAdapter})
 * para serializar/deserializar {@link CutNode}, que tiene un campo estático
 * ({@code globalCutCounter}) que no debe ser gestionado automáticamente por Gson.</p>
 */
public class ResultsActivity extends AppCompatActivity {

    // ── Caché estático para transferir datos entre actividades ─────────────

    /** Soluciones a mostrar, transferidas desde la actividad origen. */
    private static List<Solution> sCachedSolutions;
    /** Tipos de corte del escenario actual. */
    private static List<CutType>  sCachedCuts;
    /** Longitud de barra del escenario actual. */
    private static double  sCachedBarLength;
    /** Número de barras disponibles del escenario actual. */
    private static int     sCachedNumBars;
    /** {@code true} si el solver usó reutilización de sobrantes. */
    private static boolean sCachedReuseRemnants;
    /** {@code true} si los datos provienen del historial (no de un cálculo nuevo). */
    private static boolean sCachedIsFromHistory;

    /**
     * Gson con adaptador personalizado para {@link CutNode}.
     * Necesario porque {@code globalCutCounter} es estático y no debe interferir
     * en la serialización de instancias.
     */
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(CutNode.class, new CutNodeAdapter())
            .create();

    private ScenarioDao   dao;
    private List<Solution> solutions;
    private List<CutType>  cuts;
    private double barLength;
    private int    numBars;
    private boolean reuseRemnants;
    private boolean isFromHistory;

    // ── Métodos de entrada (factory methods) ──────────────────────────────

    /**
     * Abre {@link ResultsActivity} con soluciones provenientes del historial.
     * Equivale a llamar a {@link #start(Context, List, double, int, List, boolean, boolean)}
     * con {@code reuseRemnants = false} e {@code isFromHistory = true}.
     *
     * @param ctx       contexto Android
     * @param sols      soluciones a mostrar
     * @param barLength longitud de barra del escenario
     * @param numBars   número de barras del escenario
     * @param cuts      tipos de corte del escenario
     */
    public static void start(Context ctx, List<Solution> sols, double barLength,
                             int numBars, List<CutType> cuts) {
        start(ctx, sols, barLength, numBars, cuts, false, true);
    }

    /**
     * Abre {@link ResultsActivity} con control completo sobre el contexto de origen.
     *
     * @param ctx             contexto Android
     * @param sols            soluciones a mostrar
     * @param barLength       longitud de barra del escenario
     * @param numBars         número de barras del escenario
     * @param cuts            tipos de corte del escenario
     * @param reuseRemnants   {@code true} si el solver usó reutilización de sobrantes
     * @param isFromHistory   {@code true} si se abre desde el historial
     */
    public static void start(Context ctx, List<Solution> sols, double barLength,
                             int numBars, List<CutType> cuts,
                             boolean reuseRemnants, boolean isFromHistory) {
        sCachedSolutions     = sols;
        sCachedCuts          = cuts;
        sCachedBarLength     = barLength;
        sCachedNumBars       = numBars;
        sCachedReuseRemnants = reuseRemnants;
        sCachedIsFromHistory = isFromHistory;
        ctx.startActivity(new Intent(ctx, ResultsActivity.class));
    }

    // ── Ciclo de vida ──────────────────────────────────────────────────────

    /**
     * Inicializa la actividad: lee la caché estática, deduplica las soluciones
     * y configura el RecyclerView con el adaptador correspondiente al modo de resolución.
     *
     * @param savedInstanceState estado guardado (no utilizado)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        dao           = AppDatabase.getInstance(this).scenarioDao();
        isFromHistory = sCachedIsFromHistory;
        reuseRemnants = sCachedReuseRemnants;
        cuts          = sCachedCuts;
        barLength     = sCachedBarLength;
        numBars       = sCachedNumBars;

        // Resetear el contador global de cortes para que la numeración empiece desde 1
        CutNode.resetCounter();
        solutions = deduplicateByEfficiency(sCachedSolutions, barLength);

        TextView tvCount = findViewById(R.id.tvResultsCount);
        if (tvCount != null) {
            String mode = reuseRemnants ? "Con sobrantes" : "Sin sobrantes";
            tvCount.setText(solutions.size() + " solución(es) · " + mode);
        }

        RecyclerView rv = findViewById(R.id.rvSolutions);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Seleccionar el adaptador según el modo de resolución
        if (reuseRemnants) {
            RemnantSolutionAdapter adapter =
                    new RemnantSolutionAdapter(solutions, barLength, cuts, isFromHistory);
            if (isFromHistory) {
                adapter.setOnDoubleTapListener(this::exportSolution);
            } else {
                adapter.setOnLongPressListener(this::saveScenario);
            }
            rv.setAdapter(adapter);
        } else {
            SolutionAdapter adapter =
                    new SolutionAdapter(solutions, barLength, cuts, isFromHistory);
            if (isFromHistory) {
                adapter.setOnDoubleTapListener(this::exportSolution);
            } else {
                adapter.setOnLongPressListener(this::saveScenario);
            }
            rv.setAdapter(adapter);
        }
    }

    /**
     * Libera la caché estática al destruir la actividad para evitar fugas de memoria.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sCachedSolutions = null;
        sCachedCuts      = null;
    }

    // ── Deduplicación ──────────────────────────────────────────────────────

    /**
     * Elimina soluciones duplicadas de la lista basándose en la combinación de
     * barras usadas y eficiencia (redondeada a un decimal).
     * Preserva el orden original de la primera ocurrencia de cada clave única.
     *
     * @param raw    lista de soluciones posiblemente con duplicados
     * @param barLen longitud de barra, usada para calcular la eficiencia si es 0
     * @return lista sin duplicados en orden de aparición
     */
    private List<Solution> deduplicateByEfficiency(List<Solution> raw, double barLen) {
        List<Solution> unique = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (Solution s : raw) {
            double efficiency = s.getEfficiency();
            if (efficiency == 0.0 && s.getBarsUsed() > 0) {
                efficiency = (1 - s.getTotalWasteLength() / (s.getBarsUsed() * barLen)) * 100;
            }
            String key = s.getBarsUsed() + "_" + String.format("%.1f", efficiency);
            if (seen.add(key)) unique.add(s);
        }
        return unique;
    }

    // ── Guardar escenario y solución ───────────────────────────────────────

    /**
     * Persiste el escenario actual y la solución seleccionada en Room.
     * Ejecutado en un hilo de fondo para evitar bloquear el hilo principal.
     *
     * <p>Si ya existe una solución idéntica (mismo hash de escenario y mismo JSON de patrones),
     * muestra un Toast informativo y no inserta duplicados.</p>
     *
     * @param solution solución seleccionada por el usuario con pulsación larga
     */
    private void saveScenario(Solution solution) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String scenarioHash = buildScenarioHash();
            String patternsJson = gson.toJson(solution);

            int duplicates = dao.countDuplicateSolution(scenarioHash, patternsJson);
            if (duplicates > 0) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Esta solución ya está guardada",
                                Toast.LENGTH_SHORT).show());
                return;
            }

            // Reutilizar el escenario si ya existe con el mismo hash
            ScenarioEntity scenario = dao.findByHash(scenarioHash);
            long scenarioId;
            if (scenario != null) {
                scenarioId = scenario.id;
            } else {
                ScenarioEntity s = new ScenarioEntity();
                s.hashKey   = scenarioHash;
                s.numBars   = numBars;
                s.barLength = barLength;
                s.cutsJson  = gson.toJson(cuts);
                s.timestamp = System.currentTimeMillis();
                scenarioId  = dao.insertScenario(s);
            }

            double efficiency = solution.getEfficiency();
            if (efficiency == 0.0 && solution.getBarsUsed() > 0) {
                efficiency = (1 - solution.getTotalWasteLength() /
                        (solution.getBarsUsed() * barLength)) * 100;
            }

            SolutionEntity se = new SolutionEntity();
            se.scenarioId   = scenarioId;
            se.barsUsed     = solution.getBarsUsed();
            se.totalWaste   = solution.getTotalWasteLength();
            se.efficiency   = efficiency;
            se.patternsJson = patternsJson;
            dao.insertSolution(se);

            runOnUiThread(() ->
                    Toast.makeText(this, "✅ Solución guardada", Toast.LENGTH_SHORT).show());
        });
    }

    // ── Exportar solución ──────────────────────────────────────────────────

    /**
     * Muestra un diálogo para elegir el formato de exportación (JSON o CSV)
     * y lanza el intent de compartir con el contenido generado.
     *
     * @param solution solución a exportar, seleccionada con doble toque
     */
    private void exportSolution(Solution solution) {
        String[] formatos = {"JSON", "CSV"};
        new AlertDialog.Builder(this)
                .setTitle("Exportar solución")
                .setItems(formatos, (dialog, which) -> {
                    String contenido = which == 0 ? buildJson(solution) : buildCsv(solution);
                    String extension = which == 0 ? "json" : "csv";
                    String mimeType  = which == 0 ? "application/json" : "text/csv";
                    shareText(contenido, extension, mimeType);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Lanza un {@link Intent#ACTION_SEND} con el contenido de la exportación
     * para que el usuario elija la aplicación de destino (email, Drive, etc.).
     *
     * @param contenido texto a compartir
     * @param extension extensión del archivo ("json" o "csv")
     * @param mimeType  tipo MIME del contenido
     */
    private void shareText(String contenido, String extension, String mimeType) {
        String fecha  = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                .format(new Date());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Solución CSP – " + fecha);
        intent.putExtra(Intent.EXTRA_TEXT, contenido);
        startActivity(Intent.createChooser(intent, "Compartir como " + extension.toUpperCase()));
    }

    /**
     * Genera una representación JSON legible de la solución con los metadatos del escenario.
     *
     * @param s solución a serializar
     * @return cadena JSON con fecha, parámetros del escenario y datos de la solución
     */
    private String buildJson(Solution s) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        return "{\n" +
                "  \"fecha\": \"" + fecha + "\",\n" +
                "  \"numBarras\": " + numBars + ",\n" +
                "  \"longitudBarra\": " + barLength + ",\n" +
                "  \"cortes\": " + gson.toJson(cuts) + ",\n" +
                "  \"barrasUsadas\": " + s.getBarsUsed() + ",\n" +
                "  \"desperdicio\": " + String.format(Locale.US, "%.4f", s.getTotalWasteLength()) + ",\n" +
                "  \"eficiencia\": " + String.format(Locale.US, "%.2f", s.getEfficiency()) + ",\n" +
                "  \"patrones\": " + gson.toJson(s.getAssignments()) + "\n}";
    }

    /**
     * Genera una representación CSV de la solución con cabecera de metadatos.
     *
     * @param s solución a serializar
     * @return cadena CSV con separador {@code ;}
     */
    private String buildCsv(Solution s) {
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        StringBuilder sb = new StringBuilder();
        sb.append("# Solución exportada;").append(fecha).append("\n");
        sb.append("# Barras disponibles;").append(numBars).append("\n");
        sb.append("# Longitud de barra;").append(barLength).append("\n\n");
        sb.append("Barras usadas;").append(s.getBarsUsed()).append("\n");
        sb.append("Desperdicio total (mm);")
                .append(String.format(Locale.getDefault(), "%.4f", s.getTotalWasteLength())).append("\n");
        sb.append("Eficiencia (%);")
                .append(String.format(Locale.getDefault(), "%.2f", s.getEfficiency())).append("\n");
        sb.append("Patron;Veces;Desperdicio patron (mm)\n");
        if (s.getAssignments() != null) {
            for (Solution.Assignment a : s.getAssignments()) {
                sb.append(gson.toJson(a.getPattern())).append(";")
                        .append(a.getTimes()).append(";")
                        .append(String.format(Locale.getDefault(), "%.4f", a.getPatternWasteLength()))
                        .append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Construye el hash único que identifica el escenario actual.
     * Incluye el número de barras, la longitud, los tipos de corte serializados
     * y el modo de resolución.
     *
     * @return cadena hash del escenario
     */
    private String buildScenarioHash() {
        return numBars + "_" + barLength + "_" + gson.toJson(cuts) + "_reuse_" + reuseRemnants;
    }

    // ── Adaptador Gson para CutNode ────────────────────────────────────────

    /**
     * Serializador/deserializador Gson personalizado para {@link CutNode}.
     *
     * <p>Necesario porque Gson no puede manejar correctamente el campo estático
     * {@code globalCutCounter}. Este adaptador serializa solo los campos de instancia
     * y reconstruye el objeto usando el constructor que acepta un {@code cutNumber}
     * explícito (sin incrementar el contador global).</p>
     */
    private static class CutNodeAdapter
            implements JsonSerializer<CutNode>, JsonDeserializer<CutNode> {

        /**
         * Serializa un {@link CutNode} a JSON con todos sus campos de instancia.
         */
        @Override
        public JsonElement serialize(CutNode src, Type t, JsonSerializationContext ctx) {
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("cutTypeIndex",      src.getCutTypeIndex());
            o.addProperty("cutLength",         src.getCutLength());
            o.addProperty("pieceNumber",       src.getPieceNumber());
            o.addProperty("remainingAfterCut", src.getRemainingAfterCut());
            o.addProperty("wasteIfStop",       src.getWasteIfStop());
            o.addProperty("cutNumber",         src.getCutNumber());
            return o;
        }

        /**
         * Deserializa un {@link CutNode} desde JSON usando el constructor con
         * {@code cutNumber} explícito para no alterar el contador global.
         *
         * @throws JsonParseException si falta algún campo requerido en el JSON
         */
        @Override
        public CutNode deserialize(JsonElement json, Type t, JsonDeserializationContext ctx)
                throws JsonParseException {
            com.google.gson.JsonObject o = json.getAsJsonObject();
            return new CutNode(
                    o.get("cutTypeIndex").getAsInt(),
                    o.get("cutLength").getAsDouble(),
                    o.get("pieceNumber").getAsInt(),
                    o.get("remainingAfterCut").getAsDouble(),
                    o.get("wasteIfStop").getAsDouble(),
                    o.get("cutNumber").getAsInt()
            );
        }
    }
}