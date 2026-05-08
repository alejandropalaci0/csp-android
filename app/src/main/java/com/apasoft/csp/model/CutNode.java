package com.apasoft.csp.model;

/**
 * Nodo de corte dentro de un {@link CutTree}.
 *
 * <p>Representa el acto de extraer una sola pieza de una barra (o remanente).
 * Almacena el tipo de pieza cortada, las longitudes antes y después del corte
 * y un número de corte global para identificar el orden de la operación.</p>
 *
 * <p><strong>Nota sobre el contador global:</strong> {@code globalCutCounter} es
 * un campo estático compartido por todas las instancias de la aplicación.
 * Debe resetearse con {@link #resetCounter()} al iniciar una nueva resolución
 * para que la numeración comience desde 1.</p>
 */
public class CutNode {

    /** Índice del tipo de corte en la lista de {@link CutType} del solver. */
    private final int cutTypeIndex;

    /** Longitud de la pieza extraída en este corte (mm). */
    private final double cutLength;

    /**
     * Número ordinal de esta pieza dentro de su tipo en la barra actual
     * (p. ej., "segunda pieza de tipo A en esta barra").
     */
    private final int pieceNumber;

    /** Longitud de material restante en la barra tras realizar este corte (mm). */
    private final double remainingAfterCut;

    /**
     * Longitud que se desperdiciaría si la barra se cerrase en este punto (mm).
     * Se fija a 0 cuando el sobrante es menor que el umbral de desperdicio configurado.
     */
    private final double wasteIfStop;

    /** Contador global compartido que asigna un número único a cada corte. */
    private static int globalCutCounter = 0;

    /** Número de corte único y correlativo asignado a esta operación. */
    private final int cutNumber;

    /**
     * Construye un nodo de corte asignándole el siguiente número global.
     *
     * @param cutTypeIndex      índice del tipo de corte
     * @param cutLength         longitud de la pieza cortada (mm)
     * @param pieceNumber       ordinal de la pieza dentro de su tipo en la barra
     * @param remainingAfterCut longitud restante tras el corte (mm)
     * @param wasteIfStop       desperdicio si la barra se cierra aquí (mm)
     */
    public CutNode(int cutTypeIndex, double cutLength, int pieceNumber,
                   double remainingAfterCut, double wasteIfStop) {
        this.cutTypeIndex = cutTypeIndex;
        this.cutLength = cutLength;
        this.pieceNumber = pieceNumber;
        this.remainingAfterCut = remainingAfterCut;
        this.wasteIfStop = wasteIfStop;
        this.cutNumber = ++globalCutCounter;
    }

    /**
     * Construye un nodo de corte con un número de corte explícito.
     * Usado principalmente para deserialización y clonado.
     *
     * @param cutTypeIndex      índice del tipo de corte
     * @param cutLength         longitud de la pieza cortada (mm)
     * @param pieceNumber       ordinal de la pieza dentro de su tipo en la barra
     * @param remainingAfterCut longitud restante tras el corte (mm)
     * @param wasteIfStop       desperdicio si la barra se cierra aquí (mm)
     * @param cutNumber         número de corte ya asignado (no incrementa el contador global)
     */
    public CutNode(int cutTypeIndex, double cutLength, int pieceNumber,
                   double remainingAfterCut, double wasteIfStop, int cutNumber) {
        this.cutTypeIndex = cutTypeIndex;
        this.cutLength = cutLength;
        this.pieceNumber = pieceNumber;
        this.remainingAfterCut = remainingAfterCut;
        this.wasteIfStop = wasteIfStop;
        this.cutNumber = cutNumber;
    }

    /** @return índice del tipo de corte en la lista del solver */
    public int getCutTypeIndex() { return cutTypeIndex; }

    /** @return longitud de la pieza extraída en mm */
    public double getCutLength() { return cutLength; }

    /** @return ordinal de esta pieza dentro de su tipo en la barra */
    public int getPieceNumber() { return pieceNumber; }

    /** @return longitud restante de la barra después del corte en mm */
    public double getRemainingAfterCut() { return remainingAfterCut; }

    /** @return desperdicio potencial si se cierra la barra en este punto (mm) */
    public double getWasteIfStop() { return wasteIfStop; }

    /** @return número de corte único y correlativo de esta operación */
    public int getCutNumber() { return cutNumber; }

    /**
     * Crea una copia exacta de este nodo con el mismo número de corte.
     * No incrementa el contador global.
     *
     * @return nuevo {@link CutNode} con los mismos valores
     */
    public CutNode clone() {
        return new CutNode(cutTypeIndex, cutLength, pieceNumber,
                remainingAfterCut, wasteIfStop, cutNumber);
    }

    /**
     * Reinicia el contador global de cortes a cero.
     * Debe llamarse antes de comenzar una nueva resolución para que la numeración
     * empiece desde 1.
     */
    public static void resetCounter() {
        globalCutCounter = 0;
    }

    /**
     * Devuelve el valor actual del contador global (último número de corte asignado).
     *
     * @return número de cortes creados desde el último {@link #resetCounter()}
     */
    public static int getGlobalCutCounter() {
        return globalCutCounter;
    }
}