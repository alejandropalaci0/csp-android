package com.apasoft.csp.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que persiste una solución CSP en la tabla {@code solutions}.
 *
 * <p>Cada fila representa una solución (no necesariamente la óptima) asociada
 * a un {@link ScenarioEntity}. Se pueden guardar varias soluciones por escenario,
 * lo que permite al usuario comparar opciones desde el historial.</p>
 *
 * <p>La clave foránea {@code scenarioId → scenarios.id} con {@code CASCADE} garantiza
 * que al eliminar un escenario se borren también todas sus soluciones.</p>
 */
@Entity(
        tableName = "solutions",
        foreignKeys = @ForeignKey(
                entity = ScenarioEntity.class,
                parentColumns = "id",
                childColumns = "scenarioId",
                onDelete = ForeignKey.CASCADE
        )
)
public class SolutionEntity {

    /**
     * Identificador autogenerado por Room (clave primaria).
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Referencia al escenario al que pertenece esta solución. */
    public long scenarioId;

    /** Número de barras físicas utilizadas por esta solución. */
    public int barsUsed;

    /** Desperdicio total acumulado de todas las barras (mm). */
    public double totalWaste;

    /**
     * Porcentaje de eficiencia calculado: {@code (1 - desperdicio / (barras × longitud)) × 100}.
     * Permite ordenar soluciones por eficiencia directamente en la consulta SQL.
     */
    public double efficiency;

    /**
     * Serialización JSON de los patrones y asignaciones de la solución (Gson).
     * Se deserializa a {@link com.apasoft.csp.model.Solution} al mostrar los resultados.
     */
    public String patternsJson;
}