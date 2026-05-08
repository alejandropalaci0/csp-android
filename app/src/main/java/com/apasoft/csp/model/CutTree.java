package com.apasoft.csp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Árbol de corte para una sola barra o remanente.
 *
 * <p>Representa la secuencia ordenada de cortes ({@link CutNode}) realizados
 * sobre una barra (nueva o remanente) en el modo con reutilización de sobrantes.
 * Proporciona utilidades para calcular el material consumido, el remanente final
 * y generar una representación en árbol legible.</p>
 *
 * <p>El método {@link #clone()} produce una copia profunda segura para usar
 * durante el backtracking del solver.</p>
 */
public class CutTree {

    /** Identificador numérico de la barra (0-indexado). */
    private final int barId;

    /** Longitud inicial de la barra o remanente en mm. */
    private final double initialLength;

    /**
     * {@code true} si esta entrada del árbol representa un remanente reutilizado
     * de una barra anterior; {@code false} si es una barra nueva.
     */
    private final boolean isRemnant;

    /** Lista ordenada de cortes realizados sobre esta barra. */
    private final List<CutNode> cuts;

    /** Desperdicio final de esta barra en mm (0 si ≤ umbral). */
    private double waste;

    /** Tolerancia numérica para comparaciones de punto flotante. */
    private static final double EPSILON = 1e-9;

    /**
     * Construye un árbol de corte vacío para una barra nueva o remanente.
     *
     * @param barId         identificador de la barra (0-indexado)
     * @param initialLength longitud inicial disponible en mm
     * @param isRemnant     {@code true} si es un remanente reutilizado
     */
    public CutTree(int barId, double initialLength, boolean isRemnant) {
        this.barId = barId;
        this.initialLength = initialLength;
        this.isRemnant = isRemnant;
        this.cuts = new ArrayList<>();
        this.waste = 0.0;
    }

    /**
     * Añade un nodo de corte al final de la secuencia.
     *
     * @param node nodo de corte a registrar
     */
    public void addCut(CutNode node) {
        cuts.add(node);
    }

    /**
     * Elimina el último corte de la secuencia (operación de backtrack).
     * No tiene efecto si la lista ya está vacía.
     */
    public void removeLastCut() {
        if (!cuts.isEmpty()) {
            cuts.remove(cuts.size() - 1);
        }
    }

    /**
     * Fija el desperdicio final de esta barra.
     *
     * @param waste desperdicio en mm (0 si queda por debajo del umbral)
     */
    public void setWaste(double waste) {
        this.waste = waste;
    }

    /** @return identificador de la barra (0-indexado) */
    public int getBarId() { return barId; }

    /** @return longitud inicial de la barra o remanente en mm */
    public double getInitialLength() { return initialLength; }

    /** @return {@code true} si es un remanente reutilizado */
    public boolean isRemnant() { return isRemnant; }

    /**
     * Devuelve una copia defensiva de la lista de cortes.
     *
     * @return nueva lista con los mismos nodos (los nodos no se clonan)
     */
    public List<CutNode> getCuts() {
        return new ArrayList<>(cuts);
    }

    /** @return desperdicio final de la barra en mm */
    public double getWaste() { return waste; }

    /**
     * Calcula el mapa de frecuencias de tipos de corte en esta barra.
     *
     * @return mapa {@code typeIndex → cantidad de piezas de ese tipo}
     */
    public Map<Integer, Integer> getPieceCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (CutNode node : cuts) {
            int type = node.getCutTypeIndex();
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        return counts;
    }

    /**
     * Calcula la suma de todas las longitudes cortadas en esta barra.
     *
     * @return longitud total consumida en mm
     */
    public double getTotalCutLength() {
        double total = 0.0;
        for (CutNode node : cuts) {
            total += node.getCutLength();
        }
        return total;
    }

    /**
     * Calcula el remanente final disponible de la barra.
     *
     * @return {@code initialLength - getTotalCutLength()} en mm
     */
    public double getFinalRemnant() {
        return initialLength - getTotalCutLength();
    }

    /**
     * Crea una copia profunda de este árbol, clonando también cada nodo.
     * Imprescindible durante el backtracking para no contaminar ramas distintas.
     *
     * @return nueva instancia de {@link CutTree} con los mismos datos
     */
    public CutTree clone() {
        CutTree cloned = new CutTree(barId, initialLength, isRemnant);
        for (CutNode node : cuts) {
            cloned.addCut(node.clone());
        }
        cloned.waste = this.waste;
        return cloned;
    }

    /**
     * Genera una representación en árbol legible de los cortes de esta barra.
     * Incluye iconos y flechas para facilitar la inspección en la UI.
     *
     * @param cutTypes lista de tipos de corte, usada para obtener la longitud de cada pieza
     * @return cadena multilínea con el árbol de cortes
     */
    public String toTreeString(List<CutType> cutTypes) {
        StringBuilder sb = new StringBuilder();
        if (isRemnant) {
            sb.append("   ↻ Remanente de ").append(formatLength(initialLength)).append(" mm\n");
        } else {
            sb.append("📦 Barra ").append(barId + 1).append(": ")
                    .append(formatLength(initialLength)).append(" mm\n");
        }

        for (int i = 0; i < cuts.size(); i++) {
            CutNode node = cuts.get(i);
            boolean isLast = (i == cuts.size() - 1);
            String prefix = isLast ? "   └── " : "   ├── ";
            String pieceLabel = getPieceLabel(node.getCutTypeIndex(), cutTypes);

            sb.append(prefix)
                    .append("Corte ").append(node.getCutNumber()).append(": ")
                    .append(formatLength(node.getCutLength())).append(" mm")
                    .append(" → ").append(pieceLabel);

            if (node.getRemainingAfterCut() > EPSILON) {
                sb.append("\n   │   └── Remanente: ")
                        .append(formatLength(node.getRemainingAfterCut())).append(" mm");
            }
            sb.append("\n");
        }

        if (waste > EPSILON) {
            sb.append("   └── ⚠️ Desperdicio: ").append(formatLength(waste)).append(" mm\n");
        } else if (getFinalRemnant() <= EPSILON) {
            sb.append("   └── ✓ Sin desperdicio\n");
        }

        return sb.toString();
    }

    /**
     * Formatea una longitud con dos decimales.
     *
     * @param length valor a formatear
     * @return cadena con formato {@code "%.2f"}
     */
    private String formatLength(double length) {
        return String.format("%.2f", length);
    }

    /**
     * Construye la etiqueta de una pieza a partir de su índice de tipo.
     * Si el índice es válido, muestra la longitud en mm; en caso contrario
     * usa una letra correlativa (A, B, C…).
     *
     * @param typeIndex índice en la lista de tipos de corte
     * @param cutTypes  lista de tipos disponibles
     * @return etiqueta legible de la pieza
     */
    private String getPieceLabel(int typeIndex, List<CutType> cutTypes) {
        if (typeIndex >= 0 && typeIndex < cutTypes.size()) {
            double length = cutTypes.get(typeIndex).getLength();
            return String.format("%.0f mm", length);
        }
        return "Pieza " + (char) ('A' + typeIndex);
    }

    /**
     * Representación resumida del árbol, útil para depuración.
     *
     * @return cadena con el id de barra, número de cortes y desperdicio
     */
    @Override
    public String toString() {
        return String.format("CutTree{barId=%d, cuts=%d, waste=%.2f}",
                barId, cuts.size(), waste);
    }
}