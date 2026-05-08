package com.apasoft.csp.ui.history;

import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apasoft.csp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador para el RecyclerView del historial en {@link HistoryActivity}.
 *
 * <p>Muestra una card por cada {@link HistoryItem} con la fecha, el número de escenario,
 * los parámetros de entrada y las métricas de la mejor solución guardada.</p>
 *
 * <p>Gestiona dos gestos mediante {@link GestureDetector}:
 * <ul>
 *   <li><strong>Toque simple confirmado</strong>: abre las soluciones del escenario
 *       en {@link com.apasoft.csp.ui.results.ResultsActivity} vía {@link OnClick}.</li>
 *   <li><strong>Doble toque</strong>: acción opcional delegada a {@link OnDoubleTap}
 *       (actualmente no usada en {@link HistoryActivity}).</li>
 * </ul>
 * </p>
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.Holder> {

    /**
     * Callback para el toque simple sobre una card del historial.
     */
    public interface OnClick {
        /**
         * Llamado cuando el usuario toca una entrada del historial.
         *
         * @param item ítem seleccionado
         */
        void onClick(HistoryItem item);
    }

    /**
     * Callback para el doble toque sobre una card del historial.
     */
    public interface OnDoubleTap {
        /**
         * Llamado cuando el usuario hace doble toque sobre una entrada del historial.
         *
         * @param item ítem seleccionado
         */
        void onDoubleTap(HistoryItem item);
    }

    /** Lista de ítems del historial a mostrar. */
    private final List<HistoryItem> items;
    /** Listener para el toque simple. */
    private final OnClick listener;
    /** Listener para el doble toque (puede ser {@code null}). */
    private final OnDoubleTap doubleTapListener;

    /**
     * Construye el adaptador con los datos del historial y los listeners de gestos.
     *
     * @param items             lista de ítems a mostrar
     * @param listener          listener de toque simple (no puede ser {@code null})
     * @param doubleTapListener listener de doble toque, o {@code null} si no se usa
     */
    public HistoryAdapter(List<HistoryItem> items, OnClick listener, OnDoubleTap doubleTapListener) {
        this.items = items;
        this.listener = listener;
        this.doubleTapListener = doubleTapListener;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new Holder(v);
    }

    /**
     * Rellena la card del historial con los datos del ítem correspondiente
     * y configura el {@link GestureDetector} para distinguir toque simple y doble toque.
     *
     * @param h   ViewHolder a configurar
     * @param pos posición en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        HistoryItem item = items.get(pos);

        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(item.scenario.timestamp));

        h.tvDate.setText(date);
        h.tvTitle.setText("Escenario " + (pos + 1));
        h.tvInfo.setText("Barras: " + item.scenario.numBars +
                " · Longitud: " + item.scenario.barLength + " m");

        int numSols = item.allSolutions != null ? item.allSolutions.size() : 1;
        String solLabel = numSols == 1 ? "1 opción guardada" : numSols + " opciones guardadas";
        h.tvResult.setText("Desp.: " +
                String.format(Locale.getDefault(), "%.2f", item.solutionEntity.totalWaste) +
                " m · Efic.: " +
                String.format(Locale.getDefault(), "%.1f", item.solutionEntity.efficiency) +
                "% · " + solLabel);

        // GestureDetector para distinguir toque simple confirmado de doble toque
        GestureDetector gd = new GestureDetector(h.itemView.getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e) {
                        listener.onClick(item);
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (doubleTapListener != null) doubleTapListener.onDoubleTap(item);
                        return true;
                    }
                });

        h.itemView.setOnTouchListener((v, event) -> {
            gd.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) v.performClick();
            return true;
        });
        h.itemView.setOnClickListener(null);
    }

    /** {@inheritDoc} */
    @Override
    public int getItemCount() { return items.size(); }

    /**
     * ViewHolder para una card del historial con referencias a sus TextViews.
     */
    static class Holder extends RecyclerView.ViewHolder {
        /** Fecha y hora de creación del escenario. */
        TextView tvDate;
        /** Título del escenario (número ordinal). */
        TextView tvTitle;
        /** Parámetros de entrada (barras, longitud). */
        TextView tvInfo;
        /** Métricas de la mejor solución (desperdicio, eficiencia, opciones). */
        TextView tvResult;

        Holder(View v) {
            super(v);
            tvDate   = v.findViewById(R.id.tvDate);
            tvTitle  = v.findViewById(R.id.tvTitle);
            tvInfo   = v.findViewById(R.id.tvInfo);
            tvResult = v.findViewById(R.id.tvResult);
        }
    }
}