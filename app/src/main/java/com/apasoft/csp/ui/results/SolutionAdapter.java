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
import com.apasoft.csp.model.CutType;
import com.apasoft.csp.model.Pattern;
import com.apasoft.csp.model.Solution;

import java.util.List;

/**
 * Adaptador para el RecyclerView de soluciones en modo <strong>sin reutilización de sobrantes</strong>.
 *
 * <p>Cada card muestra:
 * <ul>
 *   <li>Etiqueta de ranking (★ ÓPTIMA / Opción N) junto con barras usadas y desperdicio total.</li>
 *   <li>Porcentaje de eficiencia con código de color (verde ≥ 90%, naranja ≥ 70%, rojo &lt; 70%).</li>
 *   <li>Detalle de cortes por barra con el patrón aplicado y el sobrante.</li>
 *   <li>Resumen de piezas totales producidas.</li>
 *   <li>Hint dinámico según el contexto (guardar vs. exportar).</li>
 * </ul>
 * </p>
 *
 * <p>Los gestos se gestionan con {@link GestureDetector}:
 * <ul>
 *   <li>Desde cálculo nuevo: pulsación larga → {@link OnLongPressListener}.</li>
 *   <li>Desde historial: doble toque → {@link OnDoubleTapListener}.</li>
 * </ul>
 * </p>
 */
public class SolutionAdapter extends RecyclerView.Adapter<SolutionAdapter.Holder> {

    /**
     * Callback para la acción de pulsación larga sobre una solución (guardar).
     */
    public interface OnLongPressListener  {
        /**
         * @param s solución sobre la que el usuario hizo pulsación larga
         */
        void onLongPress(Solution s);
    }

    /**
     * Callback para la acción de doble toque sobre una solución (exportar).
     */
    public interface OnDoubleTapListener  {
        /**
         * @param s solución sobre la que el usuario hizo doble toque
         */
        void onDoubleTap(Solution s);
    }

    private OnLongPressListener longPressListener;
    private OnDoubleTapListener doubleTapListener;

    /** Lista de soluciones deduplicadas a mostrar. */
    private final List<Solution> solutions;
    /** Longitud de barra del escenario (para calcular eficiencia). */
    private final double         barLength;
    /** Tipos de corte del escenario (para mostrar etiquetas de longitud). */
    private final List<CutType>  cuts;
    /**
     * {@code true} si los datos provienen del historial.
     * Controla el hint de la card y el gesto activo.
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
    public SolutionAdapter(List<Solution> solutions, double barLength,
                           List<CutType> cuts, boolean isFromHistory) {
        this.solutions     = solutions;
        this.barLength     = barLength;
        this.cuts          = cuts;
        this.isFromHistory = isFromHistory;
    }

    /**
     * Asigna el listener de pulsación larga (usado cuando {@code isFromHistory = false}).
     *
     * @param l listener a asignar
     */
    public void setOnLongPressListener(OnLongPressListener l)  { longPressListener = l; }

    /**
     * Asigna el listener de doble toque (usado cuando {@code isFromHistory = true}).
     *
     * @param l listener a asignar
     */
    public void setOnDoubleTapListener(OnDoubleTapListener l)  { doubleTapListener = l; }

    /** {@inheritDoc} */
    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_solution, parent, false);
        return new Holder(v);
    }

    /**
     * Rellena la card con los datos de la solución y configura los gestos
     * según el contexto de origen.
     *
     * @param h   ViewHolder a configurar
     * @param pos posición de la solución en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        Solution s      = solutions.get(pos);
        double waste    = s.getTotalWasteLength();
        double efficiency = (1 - waste / (s.getBarsUsed() * barLength)) * 100;

        // Etiqueta de ranking y métricas principales
        String label = pos == 0 ? "★ ÓPTIMA" : "Opción " + (pos + 1);
        h.tvSummary.setText(label + "\n"
                + "Barras: " + s.getBarsUsed()
                + "   Desperdicio: " + String.format("%.1f", waste) + " mm");

        // Eficiencia con código de color semafórico
        h.tvEfficiency.setText(String.format("%.1f%%", efficiency));
        int effColor = efficiency >= 90 ? android.graphics.Color.parseColor("#34C759")
                : efficiency >= 70 ? android.graphics.Color.parseColor("#FF9500")
                :                    android.graphics.Color.parseColor("#FF3B30");
        h.tvEfficiency.setTextColor(effColor);

        // Detalle de cortes: una línea por barra con el patrón y el sobrante
        StringBuilder sb = new StringBuilder();
        int barNum = 1;
        for (Solution.Assignment a : s.getAssignments()) {
            Pattern p      = a.getPattern();
            int[]   pieces = p.getPieces();
            double  wasteBar = a.getPatternWasteLength();
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < pieces.length; i++) {
                if (pieces[i] > 0) {
                    if (line.length() > 0) line.append(" + ");
                    line.append(pieces[i]).append("×")
                            .append(String.format("%.0f", cuts.get(i).getLength())).append("mm");
                }
            }
            for (int t = 0; t < a.getTimes(); t++) {
                sb.append("Barra ").append(barNum++).append(":  ").append(line);
                if (wasteBar > 0)
                    sb.append("  [sobra ").append(String.format("%.0f", wasteBar)).append(" mm]");
                sb.append("\n");
            }
        }
        h.tvProduction.setText(sb.toString().trim());

        // Resumen de piezas producidas totales
        StringBuilder foot = new StringBuilder("Producido: ");
        for (int i = 0; i < cuts.size(); i++) {
            int total = 0;
            for (Solution.Assignment a : s.getAssignments())
                total += a.getPattern().getPieces()[i] * a.getTimes();
            if (total > 0)
                foot.append(total).append("×")
                        .append(String.format("%.0f", cuts.get(i).getLength())).append("mm  ");
        }
        h.tvPatterns.setText(foot.toString().trim());

        // Hint dinámico según contexto
        h.tvHint.setText(isFromHistory
                ? "Doble toque para exportar"
                : "Mantén pulsado para guardar esta solución");

        // Configurar gestos según contexto de origen
        if (isFromHistory) {
            GestureDetector gd = new GestureDetector(h.itemView.getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            if (doubleTapListener != null) doubleTapListener.onDoubleTap(s);
                            return true;
                        }
                    });
            h.itemView.setOnTouchListener((v, event) -> {
                gd.onTouchEvent(event);
                if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
                return true;
            });
            h.itemView.setOnClickListener(null);
            h.itemView.setOnLongClickListener(null);
        } else {
            h.itemView.setOnTouchListener(null);
            h.itemView.setOnLongClickListener(v -> {
                if (longPressListener != null) longPressListener.onLongPress(s);
                return true;
            });
        }
    }

    /** {@inheritDoc} */
    @Override public int getItemCount() { return solutions.size(); }

    /**
     * ViewHolder para una card de solución sin remanentes.
     */
    static class Holder extends RecyclerView.ViewHolder {
        /** Resumen: ranking, barras usadas y desperdicio total. */
        TextView tvSummary;
        /** Porcentaje de eficiencia con color semafórico. */
        TextView tvEfficiency;
        /** Detalle de cortes por barra. */
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