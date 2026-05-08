package com.apasoft.csp.domain;

import com.apasoft.csp.model.CutNode;
import com.apasoft.csp.model.CutTree;
import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Pattern;
import com.apasoft.csp.model.Solution;

import java.util.*;

/**
 * Solver del Problema de Corte de Barras (CSP) <strong>con</strong> reutilización de sobrantes.
 *
 * <p>A diferencia de {@link CuttingStockSolver}, los remanentes de una barra
 * <em>sí</em> pueden usarse para satisfacer cortes de piezas posteriores,
 * lo que en la práctica reduce el número de barras nuevas necesarias.</p>
 *
 * <h3>Algoritmo en dos fases</h3>
 * <ol>
 *   <li><strong>Greedy Best-Fit Decreasing (BFD)</strong>: las piezas se ordenan
 *       de mayor a menor longitud. Cada pieza se asigna al remanente disponible
 *       más ajustado (aquel cuyo espacio libre excede la pieza por el menor margen).
 *       Si ningún remanente es suficiente, se abre una barra nueva.</li>
 *   <li><strong>Búsqueda local</strong> ({@link #localSearch}): hasta
 *       {@value #MAX_LOCAL_SEARCH_ITERATIONS} iteraciones en las que se intenta
 *       vaciar la barra menos llena moviendo sus piezas a remanentes de otras barras
 *       y, si no es posible, se intercambian piezas entre barras para mejorar el
 *       aprovechamiento.</li>
 * </ol>
 *
 * <h3>Advertencia de rendimiento</h3>
 * La búsqueda local y la construcción del árbol de corte son costosas.
 * Se recomienda limitar el uso a instancias con pocos tipos de corte distintos.
 */
public class RemnantCuttingStockSolver {

    /** Umbral de desperdicio por defecto en mm. */
    public static final double DEFAULT_WASTE_THRESHOLD_MM = 50.0;

    /** Tolerancia numérica para comparaciones de punto flotante. */
    private static final double EPSILON = 1e-9;

    /** Número máximo de iteraciones de la búsqueda local. */
    private static final int MAX_LOCAL_SEARCH_ITERATIONS = 100;

    /** Número máximo de barras que puede usar la solución. */
    private final int maxBars;

    /** Longitud de cada barra en mm. */
    private final double barLength;

    /** Tipos de corte requeridos. */
    private final List<CutType> cutTypes;

    /** Sobrante mínimo que se contabiliza como desperdicio real en mm. */
    private final double wasteThresholdMm;

    /**
     * Construye el solver con el umbral de desperdicio por defecto.
     *
     * @param maxBars   número máximo de barras disponibles
     * @param barLength longitud de cada barra en mm
     * @param cutTypes  tipos de corte requeridos
     */
    public RemnantCuttingStockSolver(int maxBars, double barLength, List<CutType> cutTypes) {
        this(maxBars, barLength, cutTypes, DEFAULT_WASTE_THRESHOLD_MM);
    }

    /**
     * Constructor de compatibilidad con el parámetro {@code reuseRemnants}.
     * La reutilización de remanentes está siempre activada en este solver;
     * el parámetro se conserva únicamente por compatibilidad con llamadas externas.
     *
     * @param maxBars          número máximo de barras disponibles
     * @param barLength        longitud de cada barra en mm
     * @param cutTypes         tipos de corte requeridos
     * @param wasteThresholdMm umbral de desperdicio en mm
     * @param reuseRemnants    ignorado; siempre {@code true}
     */
    public RemnantCuttingStockSolver(int maxBars, double barLength, List<CutType> cutTypes,
                                     double wasteThresholdMm, boolean reuseRemnants) {
        this(maxBars, barLength, cutTypes, wasteThresholdMm);
    }

    /**
     * Construye el solver con umbral de desperdicio personalizado.
     *
     * @param maxBars          número máximo de barras disponibles
     * @param barLength        longitud de cada barra en mm
     * @param cutTypes         tipos de corte requeridos
     * @param wasteThresholdMm umbral de desperdicio en mm
     */
    public RemnantCuttingStockSolver(int maxBars, double barLength, List<CutType> cutTypes,
                                     double wasteThresholdMm) {
        this.maxBars = maxBars;
        this.barLength = barLength;
        this.cutTypes = new ArrayList<>(cutTypes);
        this.wasteThresholdMm = wasteThresholdMm;
    }

    /**
     * Resuelve el CSP con reutilización de remanentes y devuelve la solución encontrada.
     *
     * <p>El proceso es:
     * <ol>
     *   <li>Expandir la demanda en piezas individuales ordenadas de mayor a menor.</li>
     *   <li>Asignar cada pieza al remanente más ajustado (Best-Fit Decreasing).</li>
     *   <li>Mejorar la asignación con búsqueda local.</li>
     *   <li>Convertir las barras internas al modelo de dominio ({@link Solution}).</li>
     * </ol>
     * </p>
     *
     * @return lista con una única {@link Solution} o vacía si el problema es infeasible
     *         (no hay barras suficientes para colocar todas las piezas)
     */
    public List<Solution> solve() {
        // Expandir demanda en piezas individuales
        List<Piece> pieces = new ArrayList<>();
        for (int i = 0; i < cutTypes.size(); i++) {
            CutType ct = cutTypes.get(i);
            for (int j = 0; j < ct.getQuantity(); j++) {
                pieces.add(new Piece(i, ct.getLength()));
            }
        }

        // Ordenar de mayor a menor longitud para reducir fragmentación (BFD)
        pieces.sort((a, b) -> Double.compare(b.length, a.length));

        List<Bar> bars = new ArrayList<>();

        // ── FASE 1: Greedy Best-Fit Decreasing ────────────────────────────
        for (Piece piece : pieces) {
            Bar bestBar = null;
            double smallestFit = Double.MAX_VALUE;

            // Buscar el remanente que deja el menor espacio sobrante tras el corte
            for (Bar bar : bars) {
                double remaining = bar.remainingLength;
                if (remaining >= piece.length - EPSILON) {
                    double fit = remaining - piece.length;
                    if (fit < smallestFit) {
                        smallestFit = fit;
                        bestBar = bar;
                    }
                }
            }

            if (bestBar != null) {
                bestBar.addCut(piece.typeIndex, piece.length);
            } else if (bars.size() < maxBars) {
                Bar newBar = new Bar(bars.size());
                newBar.addCut(piece.typeIndex, piece.length);
                bars.add(newBar);
            } else {
                return new ArrayList<>(); // Infeasible: límite de barras alcanzado
            }
        }

        // ── FASE 2: Búsqueda local para consolidar barras ─────────────────
        localSearch(bars);

        // Eliminar barras vacías resultantes de la búsqueda local
        bars.removeIf(b -> b.cuts.isEmpty());

        // Ordenar por aprovechamiento (las más llenas primero)
        bars.sort(Comparator.comparingDouble(b -> b.remainingLength));

        List<Solution> result = new ArrayList<>();
        result.add(convertToSolution(bars));
        return result;
    }

    // ── Búsqueda local ─────────────────────────────────────────────────────

    /**
     * Mejora iterativamente la asignación de piezas a barras con dos estrategias:
     * <ol>
     *   <li><strong>Vaciado</strong>: intenta mover todas las piezas de la barra
     *       menos llena a los remanentes de otras barras para eliminar esa barra.</li>
     *   <li><strong>Intercambio</strong>: intercambia piezas de diferente longitud
     *       entre dos barras cuando el intercambio es factible, mejorando el
     *       aprovechamiento global.</li>
     * </ol>
     *
     * @param bars lista de barras a optimizar (se modifica in place)
     */
    private void localSearch(List<Bar> bars) {
        for (int iter = 0; iter < MAX_LOCAL_SEARCH_ITERATIONS; iter++) {
            boolean improved = false;

            // ── Intento de vaciado de la barra menos llena ────────────────
            if (bars.size() > 1) {
                bars.sort((a, b) -> Double.compare(b.remainingLength, a.remainingLength));
                Bar leastFull = bars.get(0);
                List<CutRecord> toMove = new ArrayList<>(leastFull.cuts);
                boolean canEmpty = true;

                for (CutRecord cut : toMove) {
                    boolean moved = false;
                    for (int k = 1; k < bars.size(); k++) {
                        Bar target = bars.get(k);
                        if (target.remainingLength >= cut.length - EPSILON) {
                            target.addCut(cut.typeIndex, cut.length);
                            moved = true;
                            break;
                        }
                    }
                    if (!moved) { canEmpty = false; break; }
                }

                if (canEmpty && !toMove.isEmpty()) {
                    leastFull.clear();
                    bars.remove(0);
                    improved = true;
                } else {
                    // Revertir movimientos parciales recalculando los remanentes
                    for (Bar bar : bars) bar.recalculateRemaining();
                }
            }

            // ── Intercambio de piezas entre barras ────────────────────────
            if (!improved) {
                outer:
                for (int i = 0; i < bars.size(); i++) {
                    Bar barI = bars.get(i);
                    for (int j = 0; j < barI.cuts.size(); j++) {
                        CutRecord cutI = barI.cuts.get(j);
                        for (int k = i + 1; k < bars.size(); k++) {
                            Bar barK = bars.get(k);
                            for (int l = 0; l < barK.cuts.size(); l++) {
                                CutRecord cutK = barK.cuts.get(l);
                                double newRI = barI.remainingLength + cutI.length - cutK.length;
                                double newRK = barK.remainingLength + cutK.length - cutI.length;
                                if (newRI >= -EPSILON && newRK >= -EPSILON
                                        && Math.abs(cutI.length - cutK.length) > EPSILON) {
                                    barI.removeCut(j);
                                    barK.removeCut(l);
                                    barI.addCut(cutK.typeIndex, cutK.length);
                                    barK.addCut(cutI.typeIndex, cutI.length);
                                    improved = true;
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }

            if (!improved) break;
        }
    }

    // ── Conversión al modelo de dominio ────────────────────────────────────

    /**
     * Convierte la representación interna de barras ({@link Bar}/{@link CutRecord})
     * al modelo de dominio ({@link Solution} con {@link CutTree} por barra).
     *
     * @param bars lista de barras ya optimizadas
     * @return {@link Solution} lista con todos los árboles de corte y métricas calculadas
     */
    private Solution convertToSolution(List<Bar> bars) {
        List<CutTree> cutTrees = new ArrayList<>();
        List<Solution.Assignment> assignments = new ArrayList<>();
        double totalWasteEffective = 0.0;

        for (int barIdx = 0; barIdx < bars.size(); barIdx++) {
            Bar bar = bars.get(barIdx);
            CutTree tree = new CutTree(barIdx, barLength, false);

            // Ordenar cortes de mayor a menor longitud para la presentación
            List<CutRecord> sortedCuts = new ArrayList<>(bar.cuts);
            sortedCuts.sort((a, b) -> Double.compare(b.length, a.length));

            double runningLength = 0;
            for (CutRecord record : sortedCuts) {
                runningLength += record.length;
                double remainingAfter = barLength - runningLength;
                CutNode node = new CutNode(
                        record.typeIndex,
                        record.length,
                        getPieceNumber(record.typeIndex, sortedCuts, record),
                        remainingAfter,
                        remainingAfter <= wasteThresholdMm + EPSILON ? 0.0 : remainingAfter
                );
                tree.addCut(node);
            }

            double finalWaste = bar.remainingLength <= wasteThresholdMm + EPSILON
                    ? 0.0
                    : bar.remainingLength;
            tree.setWaste(finalWaste);
            cutTrees.add(tree);
            totalWasteEffective += finalWaste;

            // Construir patrón a partir del recuento de piezas de la barra
            Map<Integer, Integer> pieceCounts = new HashMap<>();
            for (CutRecord record : bar.cuts) {
                pieceCounts.put(record.typeIndex,
                        pieceCounts.getOrDefault(record.typeIndex, 0) + 1);
            }
            int[] pieces = new int[cutTypes.size()];
            for (Map.Entry<Integer, Integer> entry : pieceCounts.entrySet()) {
                pieces[entry.getKey()] = entry.getValue();
            }
            Pattern pattern = new Pattern(pieces, finalWaste);
            assignments.add(new Solution.Assignment(pattern, finalWaste, 1, tree));
        }

        double efficiency = bars.size() > 0
                ? (1 - totalWasteEffective / (bars.size() * barLength)) * 100
                : 0;

        return new Solution(bars.size(), totalWasteEffective, assignments, cutTrees, efficiency);
    }

    /**
     * Calcula el número ordinal de una pieza dentro de su tipo en una lista de cortes.
     * P. ej., si hay tres piezas de tipo 0, la primera es 1, la segunda 2, etc.
     *
     * @param typeIndex índice del tipo de corte
     * @param cuts      lista de cortes de la barra
     * @param target    corte objetivo cuyo ordinal se quiere conocer
     * @return número ordinal (1-indexado) del corte objetivo
     */
    private int getPieceNumber(int typeIndex, List<CutRecord> cuts, CutRecord target) {
        int count = 0;
        for (CutRecord record : cuts) {
            if (record.typeIndex == typeIndex) {
                count++;
                if (record == target) break;
            }
        }
        return count;
    }

    // ── Clases internas ────────────────────────────────────────────────────

    /**
     * Representa una pieza individual expandida de la demanda.
     * Se usa solo durante la fase greedy.
     */
    private static class Piece {
        /** Índice del tipo de corte en la lista del solver. */
        final int typeIndex;
        /** Longitud de la pieza en mm. */
        final double length;

        Piece(int typeIndex, double length) {
            this.typeIndex = typeIndex;
            this.length = length;
        }
    }

    /**
     * Representa una barra de material durante la resolución interna.
     * Mantiene la lista de cortes realizados y la longitud restante.
     */
    private class Bar {
        /** Identificador de la barra (0-indexado). */
        final int id;
        /** Lista de cortes realizados sobre esta barra. */
        final List<CutRecord> cuts = new ArrayList<>();
        /** Longitud de material aún disponible en mm. */
        double remainingLength;

        Bar(int id) {
            this.id = id;
            this.remainingLength = barLength;
        }

        /**
         * Añade un corte a la barra y reduce su longitud restante.
         *
         * @param typeIndex índice del tipo de corte
         * @param length    longitud del corte en mm
         */
        void addCut(int typeIndex, double length) {
            cuts.add(new CutRecord(typeIndex, length));
            remainingLength -= length;
        }

        /**
         * Elimina el corte en la posición indicada y devuelve su longitud
         * al remanente de la barra.
         *
         * @param index posición del corte a eliminar (0-indexado)
         */
        void removeCut(int index) {
            CutRecord removed = cuts.remove(index);
            remainingLength += removed.length;
        }

        /**
         * Vacía la barra, eliminando todos los cortes y restaurando la longitud completa.
         */
        void clear() {
            cuts.clear();
            remainingLength = barLength;
        }

        /**
         * Recalcula la longitud restante a partir de los cortes actuales.
         * Necesario tras revertir movimientos parciales en la búsqueda local.
         */
        void recalculateRemaining() {
            remainingLength = barLength;
            for (CutRecord cut : cuts) remainingLength -= cut.length;
        }
    }

    /**
     * Registro inmutable de un corte realizado sobre una barra.
     */
    private static class CutRecord {
        /** Índice del tipo de corte en la lista del solver. */
        final int typeIndex;
        /** Longitud del corte en mm. */
        final double length;

        CutRecord(int typeIndex, double length) {
            this.typeIndex = typeIndex;
            this.length = length;
        }
    }
}