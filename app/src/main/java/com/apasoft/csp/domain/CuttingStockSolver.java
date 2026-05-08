package com.apasoft.csp.domain;

import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Pattern;
import com.apasoft.csp.model.Solution;

import java.util.*;

/**
 * Solver del Problema de Corte de Barras (CSP) <strong>sin</strong> reutilización de sobrantes.
 *
 * <p>Cada barra se planifica de forma completamente independiente: el material
 * sobrante de una barra <em>no</em> puede usarse para satisfacer la demanda de
 * otras barras. Esto simplifica el espacio de búsqueda y garantiza soluciones
 * exactas para instancias de tamaño moderado.</p>
 *
 * <h3>Algoritmo</h3>
 * <ol>
 *   <li><strong>Generación de patrones</strong> ({@link #generatePatterns()}):
 *       enumeración exhaustiva mediante backtracking de todos los patrones
 *       de corte válidos para una sola barra entera.</li>
 *   <li><strong>Búsqueda de combinaciones</strong> ({@link #search}):
 *       Branch &amp; Bound simplificado que prueba combinaciones de patrones
 *       hasta cubrir la demanda sin exceder el número de barras disponibles.</li>
 * </ol>
 *
 * <h3>Advertencia de rendimiento</h3>
 * La complejidad crece exponencialmente con el número de tipos de corte y de barras.
 * Se recomienda no superar 6-7 tipos de corte ni 50 barras.
 */
public class CuttingStockSolver {

    /**
     * Umbral de desperdicio por defecto en mm.
     * Los sobrantes por debajo de este valor se consideran desperdicio cero.
     */
    public static final double DEFAULT_WASTE_THRESHOLD_MM = 50.0;

    /** Número máximo de barras disponibles. */
    private final int numBars;

    /** Longitud de cada barra en mm. */
    private final double barLength;

    /** Tipos de corte requeridos. */
    private final List<CutType> cutTypes;

    /**
     * Sobrante mínimo que se contabiliza como desperdicio real.
     * Por debajo de este umbral se trata como cero.
     */
    private final double wasteThresholdMm;

    /** Patrones generados para una barra entera. */
    private final List<Pattern> patterns = new ArrayList<>();

    /** Conjunto de claves de soluciones ya encontradas para evitar duplicados. */
    private final Set<String> seenSolutions = new HashSet<>();

    /** Soluciones válidas encontradas durante la búsqueda. */
    private final List<Solution> solutions = new ArrayList<>();

    /**
     * Construye el solver con el umbral de desperdicio por defecto.
     *
     * @param numBars   número de barras disponibles
     * @param barLength longitud de cada barra en mm
     * @param cutTypes  lista de tipos de corte requeridos
     */
    public CuttingStockSolver(int numBars, double barLength, List<CutType> cutTypes) {
        this(numBars, barLength, cutTypes, DEFAULT_WASTE_THRESHOLD_MM);
    }

    /**
     * Construye el solver con un umbral de desperdicio personalizado.
     *
     * @param numBars          número de barras disponibles
     * @param barLength        longitud de cada barra en mm
     * @param cutTypes         lista de tipos de corte requeridos
     * @param wasteThresholdMm sobrante mínimo para considerarse desperdicio (mm)
     */
    public CuttingStockSolver(int numBars, double barLength, List<CutType> cutTypes,
                              double wasteThresholdMm) {
        this.numBars = numBars;
        this.barLength = barLength;
        this.cutTypes = cutTypes;
        this.wasteThresholdMm = wasteThresholdMm;
    }

    /**
     * Ejecuta la resolución del CSP y devuelve todas las soluciones encontradas,
     * ordenadas de menor a mayor número de barras usadas y, en caso de empate,
     * de menor a mayor desperdicio total.
     *
     * @return lista ordenada de {@link Solution}; vacía si no se encontró ninguna
     */
    public List<Solution> solve() {
        generatePatterns();
        int nTypes = cutTypes.size();
        int[] demand = new int[nTypes];
        for (int i = 0; i < nTypes; i++) {
            demand[i] = cutTypes.get(i).getQuantity();
        }
        search(0, demand, 0, new ArrayList<>());

        solutions.sort(Comparator
                .comparingInt(Solution::getBarsUsed)
                .thenComparingDouble(Solution::getTotalWasteLength));

        return solutions;
    }

    /**
     * Genera todos los patrones de corte posibles para una sola barra entera
     * y los almacena en {@link #patterns}.
     *
     * <p>Limpia la lista antes de generar para permitir reutilizar el solver.</p>
     */
    private void generatePatterns() {
        patterns.clear();
        int n = cutTypes.size();
        int[] current = new int[n];
        backtrackPattern(0, barLength, current);
    }

    /**
     * Genera patrones de corte por backtracking recursivo.
     *
     * <p>En cada nivel se decide cuántas piezas del tipo {@code idx} (o superior)
     * incluir en el patrón, respetando la longitud residual de la barra.
     * Se permite repetir el mismo tipo ({@code backtrackPattern(i, …)}) para
     * obtener múltiples piezas del mismo tipo en un patrón.</p>
     *
     * @param idx       índice del tipo de corte a considerar en este nivel
     * @param remaining longitud de barra aún disponible en mm
     * @param current   array mutable con las cantidades actuales por tipo
     */
    private void backtrackPattern(int idx, double remaining, int[] current) {
        // Registrar el patrón parcial si ya tiene al menos una pieza
        boolean anyPositive = false;
        for (int c : current) {
            if (c > 0) { anyPositive = true; break; }
        }
        if (anyPositive) {
            double effectiveWaste = (remaining <= wasteThresholdMm + 1e-9)
                    ? 0.0
                    : round(remaining, 10);
            patterns.add(new Pattern(Arrays.copyOf(current, current.length), effectiveWaste));
        }

        int n = cutTypes.size();
        for (int i = idx; i < n; i++) {
            double cutLen = cutTypes.get(i).getLength();
            if (cutLen <= remaining + 1e-9) {
                current[i]++;
                backtrackPattern(i, remaining - cutLen, current);
                current[i]--;
            }
        }
    }

    /**
     * Busca combinaciones de patrones que cubran la demanda restante mediante
     * Branch &amp; Bound simplificado.
     *
     * <p>Estrategia de poda:
     * <ul>
     *   <li>Se abandona la rama si se supera el número máximo de barras.</li>
     *   <li>Se abandona si alguna demanda se vuelve negativa (sobreproducción innecesaria).</li>
     *   <li>Solo se exploran patrones con índice ≥ {@code patternIndex} para evitar
     *       permutaciones equivalentes de la misma combinación.</li>
     * </ul>
     * </p>
     *
     * @param patternIndex    índice mínimo de patrón a usar en este nivel (evita duplicados)
     * @param remainingDemand demanda pendiente por tipo de corte
     * @param barsUsed        número de barras consumidas hasta este punto
     * @param assignments     asignaciones acumuladas en la rama actual
     */
    private void search(int patternIndex, int[] remainingDemand, int barsUsed,
                        List<Solution.Assignment> assignments) {
        if (barsUsed > numBars) return;

        // Comprobar si toda la demanda está cubierta
        boolean allZero = true;
        for (int p : remainingDemand) {
            if (p != 0) { allZero = false; break; }
        }

        if (allZero) {
            String key = normalizeKey(assignments);
            if (!seenSolutions.contains(key)) {
                seenSolutions.add(key);
                double totalWaste = 0.0;
                for (Solution.Assignment a : assignments) {
                    totalWaste += a.getPatternWasteLength() * a.getTimes();
                }
                solutions.add(new Solution(barsUsed, totalWaste, new ArrayList<>(assignments)));
            }
            return;
        }

        // Podar si hay sobreproducción
        for (int p : remainingDemand) {
            if (p < 0) return;
        }

        for (int i = patternIndex; i < patterns.size(); i++) {
            Pattern pattern = patterns.get(i);
            int[] pieces = pattern.getPieces();
            double patternWaste = pattern.getWasteLength();

            // Solo usar patrones que reduzcan al menos una demanda pendiente
            boolean useful = false;
            for (int j = 0; j < pieces.length; j++) {
                if (pieces[j] > 0 && remainingDemand[j] > 0) { useful = true; break; }
            }
            if (!useful) continue;

            // Calcular el máximo de veces que tiene sentido aplicar este patrón
            int maxTimes = numBars - barsUsed;
            for (int j = 0; j < pieces.length; j++) {
                if (pieces[j] > 0) {
                    int needed = remainingDemand[j];
                    int times = (int) Math.ceil((double) needed / pieces[j]) + 1;
                    maxTimes = Math.min(maxTimes, times);
                }
            }

            for (int v = 1; v <= maxTimes; v++) {
                int[] newDemand = new int[remainingDemand.length];
                for (int j = 0; j < remainingDemand.length; j++) {
                    newDemand[j] = remainingDemand[j] - pieces[j] * v;
                }
                assignments.add(new Solution.Assignment(pattern, patternWaste, v));
                search(i, newDemand, barsUsed + v, assignments);
                assignments.remove(assignments.size() - 1);
            }
        }
    }

    /**
     * Genera una clave canónica y ordenada para una lista de asignaciones,
     * de modo que dos combinaciones equivalentes (mismo conjunto de patrones
     * en distinto orden) produzcan la misma clave.
     *
     * @param assignments lista de asignaciones a normalizar
     * @return clave de deduplicación como cadena de texto
     */
    private String normalizeKey(List<Solution.Assignment> assignments) {
        List<String> parts = new ArrayList<>();
        for (Solution.Assignment a : assignments) {
            parts.add(Arrays.toString(a.getPattern().getPieces()) + "x" + a.getTimes());
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }

    /**
     * Redondea un valor de punto flotante a un número de decimales dado.
     *
     * @param value    valor a redondear
     * @param decimals número de decimales
     * @return valor redondeado
     */
    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}