package com.apasoft.csp.ui.main;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.apasoft.csp.R;
import com.apasoft.csp.model.CutType;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para el RecyclerView de tipos de corte en {@link MainActivity}.
 *
 * <p>Gestiona una lista dinámica de filas ({@link RowData}), cada una con un campo
 * de longitud y uno de cantidad. Los datos se almacenan en el modelo de la fila,
 * no en las vistas, lo que evita el problema clásico de pérdida de datos al
 * reciclar ViewHolders.</p>
 *
 * <p>Los {@link TextWatcher} se asignan y desasignan en cada {@code onBindViewHolder}
 * para que siempre apunten a la {@link RowData} correcta para la posición actual.</p>
 */
public class CutTypeAdapter extends RecyclerView.Adapter<CutTypeAdapter.CutViewHolder> {

    /**
     * Modelo de datos de cada fila: almacena los valores en texto para
     * preservarlos durante el reciclaje de vistas.
     */
    private final List<RowData> rows = new ArrayList<>();

    /**
     * Construye el adaptador con una fila vacía inicial para que el usuario
     * pueda empezar a introducir datos de inmediato.
     */
    public CutTypeAdapter() {
        addEmptyRow();
    }

    /**
     * Añade una nueva fila vacía al final de la lista y notifica al RecyclerView.
     */
    public void addEmptyRow() {
        rows.add(new RowData("", ""));
        notifyItemInserted(rows.size() - 1);
    }

    /**
     * Recopila y valida todas las filas, devolviendo solo los {@link CutType}
     * con longitud &gt; 0 y cantidad &gt; 0.
     * Las filas vacías o con datos inválidos se ignoran silenciosamente.
     *
     * @return lista de tipos de corte válidos en el momento de la llamada
     */
    public List<CutType> getCutTypes() {
        List<CutType> result = new ArrayList<>();
        for (RowData row : rows) {
            if (row.length.isEmpty() || row.quantity.isEmpty()) continue;
            try {
                double len = Double.parseDouble(row.length);
                int qty    = Integer.parseInt(row.quantity);
                if (len > 0 && qty > 0) result.add(new CutType(len, qty));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public CutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cut_type, parent, false);
        return new CutViewHolder(v);
    }

    /**
     * Vincula los datos de {@link RowData} al ViewHolder, reinstalando los
     * {@link TextWatcher} para que apunten siempre a la fila correcta.
     *
     * <p>El botón de eliminar se oculta cuando solo queda una fila para evitar
     * dejar la lista vacía.</p>
     *
     * @param holder   ViewHolder a configurar
     * @param position posición de la fila en la lista
     */
    @Override
    public void onBindViewHolder(@NonNull CutViewHolder holder, int position) {
        RowData row = rows.get(position);

        // Desconectar watchers previos antes de setText para no disparar eventos espurios
        holder.etLength.removeTextChangedListener(holder.lengthWatcher);
        holder.etQuantity.removeTextChangedListener(holder.quantityWatcher);

        holder.etLength.setText(row.length);
        holder.etQuantity.setText(row.quantity);

        // Nuevos watchers que actualizan el RowData en la posición actual del adaptador
        holder.lengthWatcher = new SimpleWatcher() {
            @Override public void onChanged(String s) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_ID) rows.get(pos).length = s;
            }
        };
        holder.quantityWatcher = new SimpleWatcher() {
            @Override public void onChanged(String s) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_ID) rows.get(pos).quantity = s;
            }
        };

        holder.etLength.addTextChangedListener(holder.lengthWatcher);
        holder.etQuantity.addTextChangedListener(holder.quantityWatcher);

        // Eliminar fila al pulsar el botón; no permitir borrar si solo queda una
        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_ID && rows.size() > 1) {
                rows.remove(pos);
                notifyItemRemoved(pos);
            }
        });
        holder.btnRemove.setVisibility(rows.size() > 1 ? View.VISIBLE : View.INVISIBLE);
    }

    /** {@inheritDoc} */
    @Override
    public int getItemCount() { return rows.size(); }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    /**
     * ViewHolder para una fila de tipo de corte.
     * Mantiene referencias a los campos de entrada y los {@link TextWatcher} actuales.
     */
    static class CutViewHolder extends RecyclerView.ViewHolder {
        /** Campo de texto para la longitud de la pieza. */
        EditText etLength;
        /** Campo de texto para la cantidad requerida. */
        EditText etQuantity;
        /** Botón para eliminar esta fila. */
        ImageButton btnRemove;
        /** Watcher activo del campo de longitud. */
        SimpleWatcher lengthWatcher;
        /** Watcher activo del campo de cantidad. */
        SimpleWatcher quantityWatcher;

        CutViewHolder(@NonNull View itemView) {
            super(itemView);
            etLength   = itemView.findViewById(R.id.etCutLength);
            etQuantity = itemView.findViewById(R.id.etCutQuantity);
            btnRemove  = itemView.findViewById(R.id.btnRemoveCut);
        }
    }

    // ── Modelo de fila ───────────────────────────────────────────────────────

    /**
     * Modelo mutable de una fila del adaptador.
     * Los campos son {@code String} para reflejar exactamente lo que el usuario escribe
     * y permitir estados intermedios (p. ej. "1." mientras se escribe "1.5").
     */
    private static class RowData {
        /** Texto del campo de longitud. */
        String length;
        /** Texto del campo de cantidad. */
        String quantity;

        RowData(String l, String q) { length = l; quantity = q; }
    }

    // ── Helper TextWatcher ───────────────────────────────────────────────────

    /**
     * Implementación simplificada de {@link TextWatcher} que solo requiere
     * implementar {@link #onChanged(String)}.
     * Los otros dos métodos ({@code beforeTextChanged}, {@code onTextChanged})
     * se dejan vacíos intencionalmente.
     */
    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        /**
         * Llamado tras cada cambio de texto con el nuevo valor completo.
         *
         * @param s nuevo contenido del campo de texto
         */
        @Override public void afterTextChanged(Editable s) { onChanged(s.toString()); }

        /**
         * Método a implementar por las subclases anónimas para reaccionar al cambio.
         *
         * @param s nuevo valor del campo como {@link String}
         */
        abstract void onChanged(String s);
    }
}