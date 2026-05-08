package com.apasoft.csp.ui.history;

import com.apasoft.csp.data.ScenarioEntity;
import com.apasoft.csp.data.SolutionEntity;
import com.apasoft.csp.model.Solution;

import java.util.List;

/**
 * Modelo de vista para una entrada del historial.
 *
 * <p>Agrupa toda la información necesaria para mostrar una card en
 * {@link HistoryAdapter}: el escenario guardado, la primera solución
 * (para el resumen de la card) y la lista completa de soluciones
 * (para poder abrirlas en {@link com.apasoft.csp.ui.results.ResultsActivity}).</p>
 */
public class HistoryItem {

    /** Escenario persistido en la base de datos (parámetros de entrada). */
    public final ScenarioEntity scenario;

    /**
     * Primera solución guardada para este escenario (la de mayor eficiencia).
     * Se usa para mostrar el resumen (desperdicio, eficiencia) en la card del historial.
     */
    public final SolutionEntity solutionEntity;

    /**
     * Primera solución deserializada a objeto de dominio.
     * Disponible para acceso rápido sin necesidad de re-parsear el JSON.
     */
    public final Solution solution;

    /**
     * Lista completa de soluciones guardadas para este escenario.
     * Se pasa a {@link com.apasoft.csp.ui.results.ResultsActivity} al abrir la entrada.
     */
    public final List<Solution> allSolutions;

    /**
     * Construye un ítem del historial con todos sus datos asociados.
     *
     * @param s       escenario de la base de datos
     * @param se      entidad de la primera solución
     * @param sol     primera solución deserializada
     * @param allSols lista con todas las soluciones del escenario
     */
    public HistoryItem(ScenarioEntity s, SolutionEntity se, Solution sol, List<Solution> allSols) {
        this.scenario = s;
        this.solutionEntity = se;
        this.solution = sol;
        this.allSolutions = allSols;
    }
}