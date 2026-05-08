package com.apasoft.csp.ui.history;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apasoft.csp.R;
import com.apasoft.csp.data.AppDatabase;
import com.apasoft.csp.data.ScenarioDao;
import com.apasoft.csp.data.ScenarioEntity;
import com.apasoft.csp.data.SolutionEntity;
import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Solution;
import com.apasoft.csp.ui.results.ResultsActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Actividad que muestra el historial de escenarios y soluciones guardadas.
 *
 * <p>Funcionalidades principales:
 * <ul>
 *   <li>Carga asíncrona del historial desde Room al crearse la actividad.</li>
 *   <li>Apertura de un escenario guardado en {@link ResultsActivity} al tocarlo.</li>
 *   <li>Borrado individual de un escenario mediante deslizamiento a la izquierda
 *       ({@link ItemTouchHelper}).</li>
 *   <li>Borrado de todo el historial con el botón "Limpiar historial".</li>
 * </ul>
 * </p>
 *
 * <p>Todas las operaciones de base de datos se ejecutan en hilos de fondo
 * con {@code Executors.newSingleThreadExecutor()} y los cambios en la UI
 * se aplican con {@code runOnUiThread}.</p>
 */
public class HistoryActivity extends AppCompatActivity {

    /** Instancia de Gson para deserializar soluciones y tipos de corte desde JSON. */
    private final Gson gson = new Gson();
    /** Adaptador del RecyclerView del historial. */
    private HistoryAdapter adapter;
    /** Lista observable de ítems del historial mostrada en la UI. */
    private final List<HistoryItem> items = new ArrayList<>();
    /** DAO para acceder a las tablas de escenarios y soluciones. */
    private ScenarioDao dao;

    /**
     * Inicializa la UI, configura el RecyclerView, los listeners de botones
     * y el soporte de deslizamiento para eliminar entradas.
     *
     * @param savedInstanceState estado guardado (no utilizado)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Botón para limpiar todo el historial
        TextView btnClear = findViewById(R.id.btnClearHistory);
        btnClear.setOnClickListener(v ->
                Executors.newSingleThreadExecutor().execute(() -> {
                    dao.deleteAll();
                    runOnUiThread(() -> {
                        items.clear();
                        adapter.notifyDataSetChanged();
                    });
                })
        );

        RecyclerView rv = findViewById(R.id.rvHistory);
        rv.setLayoutManager(new LinearLayoutManager(this));
        dao = AppDatabase.getInstance(this).scenarioDao();

        // doubleTapListener se pasa como null: la exportación se gestiona dentro de ResultsActivity
        adapter = new HistoryAdapter(items, this::openSolution, null);
        rv.setAdapter(adapter);

        loadHistoryAsync();

        // Deslizamiento a la izquierda para eliminar un escenario individual
        ItemTouchHelper.SimpleCallback swipe =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                    @Override
                    public boolean onMove(RecyclerView rv,
                                          RecyclerView.ViewHolder vh,
                                          RecyclerView.ViewHolder target) { return false; }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                        int pos = vh.getAdapterPosition();
                        HistoryItem item = items.get(pos);
                        Executors.newSingleThreadExecutor().execute(() -> {
                            dao.deleteScenario(item.scenario.id);
                            runOnUiThread(() -> {
                                items.remove(pos);
                                adapter.notifyItemRemoved(pos);
                            });
                        });
                    }
                };
        new ItemTouchHelper(swipe).attachToRecyclerView(rv);
    }

    /**
     * Carga todos los escenarios y sus soluciones desde Room en un hilo de fondo,
     * deserializa el JSON de cada solución y actualiza la lista de la UI.
     *
     * <p>Los escenarios se ordenan del más reciente al más antiguo por {@code id} descendente.</p>
     */
    private void loadHistoryAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ScenarioEntity> scenarios = dao.getAllScenarios();
            List<HistoryItem> temp = new ArrayList<>();

            for (ScenarioEntity s : scenarios) {
                List<SolutionEntity> solEntities = dao.findAllSolutionsForScenario(s.id);
                if (!solEntities.isEmpty()) {
                    SolutionEntity firstEntity  = solEntities.get(0);
                    Solution firstSolution      = gson.fromJson(firstEntity.patternsJson, Solution.class);
                    List<Solution> allSolutions = new ArrayList<>();
                    for (SolutionEntity se : solEntities) {
                        allSolutions.add(gson.fromJson(se.patternsJson, Solution.class));
                    }
                    temp.add(new HistoryItem(s, firstEntity, firstSolution, allSolutions));
                }
            }

            // Ordenar del más reciente al más antiguo
            Collections.sort(temp, (a, b) -> Long.compare(b.scenario.id, a.scenario.id));

            runOnUiThread(() -> {
                items.clear();
                items.addAll(temp);
                adapter.notifyDataSetChanged();
            });
        });
    }

    /**
     * Navega a {@link ResultsActivity} con las soluciones del escenario seleccionado.
     * El flag {@code isFromHistory = true} hace que ResultsActivity muestre el
     * gesto de doble toque para exportar en lugar del de guardar.
     *
     * @param item ítem del historial seleccionado por el usuario
     */
    private void openSolution(HistoryItem item) {
        int numBars      = item.scenario.numBars;
        double barLength = item.scenario.barLength;
        Type cutType     = new TypeToken<List<CutType>>(){}.getType();
        List<CutType> cuts = gson.fromJson(item.scenario.cutsJson, cutType);
        ResultsActivity.start(this, item.allSolutions, barLength, numBars, cuts, false, true);
    }
}