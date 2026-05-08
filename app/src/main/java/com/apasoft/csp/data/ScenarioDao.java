package com.apasoft.csp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * DAO (Data Access Object) Room para las tablas {@code scenarios} y {@code solutions}.
 *
 * <p>Centraliza todas las operaciones de persistencia de escenarios y soluciones.
 * Todas las llamadas deben ejecutarse en un hilo de fondo (p. ej., con
 * {@code Executors.newSingleThreadExecutor()}) ya que Room prohíbe acceso
 * a la BD en el hilo principal.</p>
 */
@Dao
public interface ScenarioDao {

    /**
     * Inserta un nuevo escenario y devuelve el {@code id} generado por Room.
     *
     * @param scenario entidad a insertar
     * @return id autogenerado de la nueva fila
     */
    @Insert
    long insertScenario(ScenarioEntity scenario);

    /**
     * Inserta una nueva solución vinculada a un escenario existente.
     *
     * @param solution entidad a insertar (debe tener {@code scenarioId} válido)
     */
    @Insert
    void insertSolution(SolutionEntity solution);

    /**
     * Recupera todos los escenarios guardados ordenados del más reciente al más antiguo.
     *
     * @return lista de escenarios en orden descendente por {@code id}
     */
    @Query("SELECT * FROM scenarios ORDER BY id DESC")
    List<ScenarioEntity> getAllScenarios();

    /**
     * Encuentra la primera solución guardada para un escenario concreto.
     * Útil para mostrar un resumen rápido en el historial.
     *
     * @param scenarioId id del escenario
     * @return primera {@link SolutionEntity} encontrada, o {@code null} si no existe
     */
    @Query("SELECT * FROM solutions WHERE scenarioId = :scenarioId LIMIT 1")
    SolutionEntity findSolutionForScenario(long scenarioId);

    /**
     * Recupera todas las soluciones de un escenario ordenadas de mayor a menor eficiencia.
     *
     * @param scenarioId id del escenario
     * @return lista de soluciones ordenadas por eficiencia descendente
     */
    @Query("SELECT * FROM solutions WHERE scenarioId = :scenarioId ORDER BY efficiency DESC")
    List<SolutionEntity> findAllSolutionsForScenario(long scenarioId);

    /**
     * Elimina todos los escenarios (y en cascada todas las soluciones).
     * Usado por el botón "Limpiar historial" de {@link com.apasoft.csp.ui.history.HistoryActivity}.
     */
    @Query("DELETE FROM scenarios")
    void deleteAll();

    /**
     * Elimina un escenario concreto por su id (y en cascada sus soluciones).
     *
     * @param id id del escenario a eliminar
     */
    @Query("DELETE FROM scenarios WHERE id = :id")
    void deleteScenario(long id);

    /**
     * Busca un escenario por su hash único de parámetros.
     * Permite reutilizar un escenario ya existente en lugar de crear uno duplicado.
     *
     * @param hash hash del escenario construido en {@link com.apasoft.csp.ui.results.ResultsActivity}
     * @return el {@link ScenarioEntity} encontrado, o {@code null} si no existe
     */
    @Query("SELECT * FROM scenarios WHERE hashKey = :hash LIMIT 1")
    ScenarioEntity findByHash(String hash);

    /**
     * Cuenta cuántas soluciones idénticas existen para un mismo escenario.
     * Una solución es duplicada si coinciden el hash del escenario y el JSON de patrones.
     * Se usa para evitar guardar la misma solución dos veces.
     *
     * @param scenarioHash hash del escenario
     * @param patternsJson JSON serializado de los patrones de la solución
     * @return número de soluciones duplicadas encontradas (0 si no hay ninguna)
     */
    @Query("SELECT COUNT(*) FROM solutions s " +
            "INNER JOIN scenarios sc ON s.scenarioId = sc.id " +
            "WHERE sc.hashKey = :scenarioHash AND s.patternsJson = :patternsJson")
    int countDuplicateSolution(String scenarioHash, String patternsJson);
}