package com.apasoft.csp.ui.results;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apasoft.csp.R;
import com.apasoft.csp.model.CutTree;
import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Solution;

import java.util.List;

/**
 * Adaptador para el RecyclerView de soluciones en modo <strong>con reutilización de sobrantes</strong>.
 *
 * <p>Variante de {@link SolutionAdapter} específica para soluciones generadas por
 * {@link com.apasoft.csp.domain.RemnantCuttingStockSolver}. Muestra el detalle de
 * cortes usando {@link CutTree#toTreeString(List)} cuando los árboles de corte
 * están disponibles, o un fallback plano de asignaciones para soluciones serializadas
 * sin árbol (compatibilidad hacia atrás).</p>
 *
 * <p>La lógica de gestos y el hint dinámico son idénticos a {@link SolutionAdapter}.</p>
 */
public class RemnantSolutionAdapter extends RecyclerView.Adapter<RemnantSolutionAdapter.Holder> {

    /**
     * Callback para la pulsación larga (guardar solución desde cálculo nuevo).
     */
    public interface OnLongPressListener {
        /** @param solution solución sobre la que el usuario hizo pulsación larga */
        void onLongPress(Solution solution);
    }

    /**
     * Callback para el doble toque (exportar solución desde historial).
     */
    public interface OnDoubleTapListener {
        /** @param solution solución sobre la que el usuario hizo doble toque */
        void onDoubleTap(Solution solution);
    }

    private OnLongPressListener longPressListener;
    private OnDoubleTapListener doubleTapListener;

    /** Lista de soluciones a mostrar. */
    private final List<Solution> solutions;
    /** Longitud de barra del escenario en mm. */
    private final double         barLength;
    /** Tipos de corte del escenario. */
    private final List<CutType>  cuts;
    /**
     * {@code true} si los datos provienen del historial.
     * Determina el gesto activo y el hint de la card.
     */
    private final boolean        isFromHistory;

    /**
     * Construye el adaptador.
     *
     * @param solutions     lista de soluciones a mostrar
     * @param barLength     longitud de barra en mm
     * @param cuts          tipos de corte del escenario
     * @param isFromHistory {@code true} si se abre desde el historial
     */
    public RemnantSolutionAdapter(List<Solution> solutions, double barLength,
                                  List<CutType> cuts, boolean isFromHistory) {
        this.solutions     = solutions;
        this.barLength     = barLength;
        this.cuts          = cuts;
        this.isFromHistory = isFromHistory;
    }

    /**
     * Asigna el listener de pulsación larga.
     *
     * @param l listener a asignar
     */
    public void setOnLongPressListener(OnLongPressListener l) { longPressListener = l; }

    /**
     * Asigna el listener de doble toque.
     *
     * @param l listener a asignar
     */
    public void setOnDoubleTapListener(OnDoubleTapListener l) { doubleTapListener = l; }

    /** {@inheritDoc} */
    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solution_remnant, parent, false);
        return new Holder(v);
    }

    /**
     * Rellena la card con los datos de la solución con remanentes.
     *
     * <p>Si la solución tiene {@link CutTree}s disponibles, el detalle de cortes
     * se genera con {@link CutTree#toTreeString(List)} para una representación
     * jerárquica. En caso contrario se usa el fallback plano de asignaciones
     * para mantener compatibilidad con soluciones deserializadas del historial
     * que no incluyan árboles.</p>
     *
     * @param h   ViewHolder a configurar
     * @param pos posición de la solución en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Solution sol = solutions.get(pos);

        double efficiency = sol.getEfficiency();
        if (efficiency == 0.0 && sol.getBarsUsed() > 0) {
            efficiency = (1 - sol.getTotalWasteLength() / (sol.getBarsUsed() * barLength)) * 100;
        }

        // Cabecera con ranking y métricas
        String label = pos == 0 ? "★ ÓPTIMA (con sobrantes)" : "Opción " + (pos + 1);
        h.tvSummary.setText(label + "\n"
                + "Barras: " + sol.getBarsUsed()
                + "   Desperdicio: " + String.format("%.1f", sol.getTotalWasteLength()) + " mm");

        // Eficiencia con código de color semafórico
        h.tvEfficiency.setText(String.format("%.1f%%", efficiency));
        int effColor = efficiency >= 90 ? android.graphics.Color.parseColor("#34C759")
                : efficiency >= 70 ? android.graphics.Color.parseColor("#FF9500")
                :                    android.graphics.Color.parseColor("#FF3B30");
        h.tvEfficiency.setTextColor(effColor);

        // Detalle de cortes: árbol jerárquico si disponible, fallback plano en caso contrario
        StringBuilder sb = new StringBuilder();
        if (sol.getCutTrees() != null) {
            int barNum = 1;
            for (CutTree tree : sol.getCutTrees()) {
                sb.append("Barra ").append(barNum++).append(":\n");
                sb.append(tree.toTreeString(cuts));
                sb.append("\n");
            }
        } else if (sol.getAssignments() != null) {
            // Fallback para soluciones sin árbol (compatibilidad con versiones anteriores)
            int barNum = 1;
            for (Solution.Assignment a : sol.getAssignments()) {
                int[] pieces = a.getPattern().getPieces();
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < pieces.length; i++) {
                    if (pieces[i] > 0) {
                        if (line.length() > 0) line.append(" + ");
                        line.append(pieces[i]).append("×")
                                .append(String.format("%.0f", cuts.get(i).getLength())).append("mm");
                    }
                }
                double wasteBar = a.getPatternWasteLength();
                for (int t = 0; t < a.getTimes(); t++) {
                    sb.append("Barra ").append(barNum++).append(":  ").append(line);
                    if (wasteBar > 0)
                        sb.append("  [sobra ").append(String.format("%.0f", wasteBar)).append(" mm]");
                    sb.append("\n");
                }
            }
        }
        h.tvProduction.setText(sb.toString().trim());

        // Resumen de piezas totales producidas
        StringBuilder foot = new StringBuilder("Producido: ");
        if (sol.getAssignments() != null) {
            for (int i = 0; i < cuts.size(); i++) {
                int total = 0;
                for (Solution.Assignment a : sol.getAssignments())
                    total += a.getPattern().getPieces()[i] * a.getTimes();
                if (total > 0)
                    foot.append(total).append("×")
                            .append(String.format("%.0f", cuts.get(i).getLength())).append("mm  ");
            }
        }
        h.tvPatterns.setText(foot.toString().trim());

        // Hint dinámico
        h.tvHint.setText(isFromHistory
                ? "Doble toque para exportar"
                : "Mantén pulsado para guardar esta solución");

        // Gestos según contexto de origen
        if (isFromHistory) {
            GestureDetector gd = new GestureDetector(h.itemView.getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (doubleTapListener != null) doubleTapListener.onDoubleTap(sol);
                            return true;
                        }
                    });
            h.itemView.setOnTouchListener((v, event) -> {
                gd.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
                return true;
            });
            h.itemView.setOnLongClickListener(null);
        } else {
            h.itemView.setOnTouchListener(null);
            h.itemView.setOnLongClickListener(v -> {
                if (longPressListener != null) longPressListener.onLongPress(sol);
                return true;
            });
        }
    }

    /** {@inheritDoc} */
    @Override public int getItemCount() { return solutions.size(); }

    /**
     * ViewHolder para una card de solución con remanentes.
     */
    static class Holder extends RecyclerView.ViewHolder {
        /** Resumen: ranking, barras usadas y desperdicio total. */
        TextView tvSummary;
        /** Porcentaje de eficiencia con color semafórico. */
        TextView tvEfficiency;
        /** Detalle de cortes por barra (árbol jerárquico o fallback plano). */
        TextView tvProduction;
        /** Resumen de piezas totales producidas. */
        TextView tvPatterns;
        /** Instrucción de gesto disponible (guardar o exportar). */
        TextView tvHint;

        Holder(View v) {
            super(v);
            tvSummary    = v.findViewById(R.id.tvSummary);
            tvEfficiency = v.findViewById(R.id.tvEfficiency);
            tvProduction = v.findViewById(R.id.tvProduction);
            tvPatterns   = v.findViewById(R.id.tvPatterns);
            tvHint       = v.findViewById(R.id.tvHint);
        }
    }
}