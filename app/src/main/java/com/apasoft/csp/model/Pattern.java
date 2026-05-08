package com.apasoft.csp.model;

import java.util.Arrays;

/**
 * Patrón de corte aplicable a una única barra entera.
 *
 * <p>Un patrón describe cuántas piezas de cada tipo ({@code pieces[i]}) se obtienen
 * de una sola barra, junto con el desperdicio resultante ({@code wasteLength}).
 * Se usa como unidad atómica en {@link com.apasoft.csp.domain.CuttingStockSolver}
 * para construir combinaciones que cubran la demanda total.</p>
 */
public class Pattern {

    /**
     * Número de piezas de cada tipo incluidas en este patrón.
     * El índice {@code i} corresponde al tipo {@code i} en la lista de {@link CutType}.
     */
    private final int[] pieces;

    /**
     * Longitud desperdiciada al aplicar este patrón a una barra completa (en mm).
     * Se fija a 0 cuando el sobrante es inferior al umbral de desperdicio configurado.
     */
    private final double wasteLength;

    /**
     * Construye un patrón de corte.
     *
     * @param pieces      array con el número de piezas por tipo; no debe ser {@code null}
     * @param wasteLength longitud desperdiciada en mm (≥ 0)
     */
    public Pattern(int[] pieces, double wasteLength) {
        this.pieces = pieces;
        this.wasteLength = wasteLength;
    }

    /**
     * Devuelve el array de piezas del patrón.
     * El array es una referencia directa interna; no modificar externamente.
     *
     * @return array de cantidades por tipo de corte
     */
    public int[] getPieces() {
        return pieces;
    }

    /**
     * Devuelve el desperdicio asociado a este patrón en mm.
     *
     * @return longitud desperdiciada
     */
    public double getWasteLength() {
        return wasteLength;
    }

    /**
     * Representación textual del patrón, útil para depuración.
     *
     * @return cadena con el array de piezas y el desperdicio
     */
    @Override
    public String toString() {
        return "Pattern{" +
                "pieces=" + Arrays.toString(pieces) +
                ", wasteLength=" + wasteLength +
                '}';
    }
}