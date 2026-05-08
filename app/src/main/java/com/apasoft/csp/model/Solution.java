package com.apasoft.csp.model;

import java.util.List;

/**
 * Solución completa al problema de corte de barras (CSP).
 *
 * <p>Encapsula el conjunto de asignaciones de patrones a barras que cubre
 * la demanda completa, el número de barras consumidas, el desperdicio total
 * y, opcionalmente, los árboles de corte para el modo con reutilización
 * de remanentes.</p>
 */
public class Solution {

    /** Número total de barras físicas consumidas por esta solución. */
    private final int barsUsed;

    /** Suma del desperdicio de todas las barras, en mm. */
    private final double totalWasteLength;

    /**
     * Lista de asignaciones patrón → número de veces aplicado.
     * Presente siempre; en el modo con remanentes cada {@link Assignment}
     * lleva además un {@link CutTree} propio.
     */
    private final List<Assignment> assignments;

    /**
     * Árboles de corte por barra, generados por {@link com.apasoft.csp.domain.RemnantCuttingStockSolver}.
     * {@code null} en el modo sin reutilización de sobrantes.
     */
    private final List<CutTree> cutTrees;

    /**
     * Porcentaje de eficiencia de la solución (0–100).
     * Calculado como {@code (1 - desperdicio / (barras × longitud)) × 100}.
     */
    private final double efficiency;

    // ── Clase anidada: Assignment ──────────────────────────────────────────

    /**
     * Asignación de un {@link Pattern} a un número determinado de barras.
     *
     * <p>En el modo sin remanentes, {@code times} indica cuántas barras idénticas
     * se cortan con ese patrón. En el modo con remanentes, {@code times} es siempre 1
     * y la asignación incluye un {@link CutTree} con el detalle de cortes.</p>
     */
    public static class Assignment {

        /** Patrón de corte asociado. */
        private final Pattern pattern;

        /** Desperdicio generado por este patrón en cada barra (mm). */
        private final double patternWasteLength;

        /** Número de barras a las que se aplica este patrón. */
        private final int times;

        /**
         * Árbol de corte para representación jerárquica de remanentes.
         * {@code null} en el modo sin reutilización.
         */
        private final CutTree cutTree;

        /**
         * Construye una asignación sin árbol de corte (modo sin remanentes).
         *
         * @param pattern            patrón de corte
         * @param patternWasteLength desperdicio del patrón en mm
         * @param times              número de barras con este patrón
         */
        public Assignment(Pattern pattern, double patternWasteLength, int times) {
            this(pattern, patternWasteLength, times, null);
        }

        /**
         * Construye una asignación con árbol de corte opcional (modo con remanentes).
         *
         * @param pattern            patrón de corte
         * @param patternWasteLength desperdicio del patrón en mm
         * @param times              número de barras con este patrón (normalmente 1)
         * @param cutTree            árbol de decisión de cortes, o {@code null}
         */
        public Assignment(Pattern pattern, double patternWasteLength, int times, CutTree cutTree) {
            this.pattern = pattern;
            this.patternWasteLength = patternWasteLength;
            this.times = times;
            this.cutTree = cutTree;
        }

        /** @return el patrón de corte de esta asignación */
        public Pattern getPattern() { return pattern; }

        /** @return el desperdicio por barra de este patrón en mm */
        public double getPatternWasteLength() { return patternWasteLength; }

        /** @return número de barras a las que se aplica el patrón */
        public int getTimes() { return times; }

        /** @return el árbol de corte asociado, o {@code null} si no existe */
        public CutTree getCutTree() { return cutTree; }

        /** @return {@code true} si esta asignación tiene árbol de corte */
        public boolean hasCutTree() { return cutTree != null; }
    }

    // ── Constructores ──────────────────────────────────────────────────────

    /**
     * Construye una solución sin árboles de corte (modo sin reutilización de sobrantes).
     *
     * @param barsUsed         número de barras usadas
     * @param totalWasteLength desperdicio total en mm
     * @param assignments      lista de asignaciones patrón → repeticiones
     */
    public Solution(int barsUsed, double totalWasteLength, List<Assignment> assignments) {
        this(barsUsed, totalWasteLength, assignments, null, 0.0);
    }

    /**
     * Construye una solución completa con árboles de corte y eficiencia calculada.
     *
     * @param barsUsed         número de barras usadas
     * @param totalWasteLength desperdicio total en mm
     * @param assignments      lista de asignaciones
     * @param cutTrees         árboles de corte por barra, o {@code null}
     * @param efficiency       porcentaje de eficiencia (0–100)
     */
    public Solution(int barsUsed, double totalWasteLength, List<Assignment> assignments,
                    List<CutTree> cutTrees, double efficiency) {
        this.barsUsed = barsUsed;
        this.totalWasteLength = totalWasteLength;
        this.assignments = assignments;
        this.cutTrees = cutTrees;
        this.efficiency = efficiency;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    /** @return número total de barras físicas utilizadas */
    public int getBarsUsed() { return barsUsed; }

    /** @return desperdicio acumulado de todas las barras en mm */
    public double getTotalWasteLength() { return totalWasteLength; }

    /** @return lista de asignaciones de patrones a barras */
    public List<Assignment> getAssignments() { return assignments; }

    /** @return lista de árboles de corte por barra, o {@code null} */
    public List<CutTree> getCutTrees() { return cutTrees; }

    /** @return eficiencia de la solución en porcentaje (0–100) */
    public double getEfficiency() { return efficiency; }

    /**
     * Indica si la solución contiene árboles de corte.
     *
     * @return {@code true} cuando existen árboles de corte (modo con remanentes)
     */
    public boolean hasCutTrees() {
        return cutTrees != null && !cutTrees.isEmpty();
    }
}