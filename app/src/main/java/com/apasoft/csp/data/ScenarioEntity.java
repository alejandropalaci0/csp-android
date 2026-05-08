package com.apasoft.csp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que persiste un escenario de cálculo en la tabla {@code scenarios}.
 *
 * <p>Un escenario define los parámetros de entrada de una resolución CSP:
 * número y longitud de barras disponibles, tipos de corte requeridos y un
 * hash único que permite detectar escenarios duplicados sin repetir el cálculo.</p>
 */
@Entity(tableName = "scenarios")
public class ScenarioEntity {

    /**
     * Identificador autogenerado por Room (clave primaria).
     * Se asigna automáticamente al insertar la fila.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Hash único del escenario, construido a partir de sus parámetros de entrada.
     * Permite buscar escenarios idénticos con {@link ScenarioDao#findByHash(String)}
     * y evitar duplicados en el historial.
     */
    public String hashKey;

    /** Número de barras disponibles para este escenario. */
    public int numBars;

    /** Longitud de cada barra en las unidades de trabajo (mm, cm, etc.). */
    public double barLength;

    /**
     * Lista de tipos de corte serializada como JSON con Gson.
     * Se deserializa a {@code List<CutType>} al recuperar el escenario del historial.
     */
    public String cutsJson;

    /** Marca de tiempo de creación del escenario (milisegundos desde epoch). */
    public long timestamp;
}