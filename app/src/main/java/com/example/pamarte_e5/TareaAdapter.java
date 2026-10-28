
package com.example.pamarte_e5;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TareaAdapter extends RecyclerView.Adapter<TareaAdapter.TareaViewHolder> {

    private Cursor cursor;

    public TareaAdapter(Cursor cursor) {
        this.cursor = cursor;
    }

    public void actualizarCursor(Cursor nuevoCursor) {
        if (cursor != null && cursor != nuevoCursor && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = nuevoCursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tarea, parent, false);

        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TareaViewHolder holder,
            int position) {

        if (cursor == null || cursor.isClosed()
                || !cursor.moveToPosition(position)) {
            return;
        }

        String idAct = cursor.getString(
                cursor.getColumnIndexOrThrow("id_act"));

        String nomAct = cursor.getString(
                cursor.getColumnIndexOrThrow("nom_act"));

        String descripAct = cursor.getString(
                cursor.getColumnIndexOrThrow("descrip_act"));

        String fEnt = cursor.getString(
                cursor.getColumnIndexOrThrow("f_ent"));

        String prioridad = cursor.getString(
                cursor.getColumnIndexOrThrow("prioridad"));

        int completado = cursor.getInt(
                cursor.getColumnIndexOrThrow("completado"));

        holder.txtNomAct.setText(nomAct);
        holder.txtDescripAct.setText(
                descripAct != null && !descripAct.trim().isEmpty()
                        ? descripAct
                        : "Sin descripción"
        );

        holder.txtFechaEntrega.setText("Entrega: " + fEnt);
        holder.txtPrioridad.setText("Prioridad: " + prioridad);

        if (completado == 1) {
            holder.txtEstadoTarea.setText("Completada");
            holder.txtNomAct.setTypeface(null, Typeface.NORMAL);
            holder.txtEstadoTarea.setAlpha(0.7f);
        } else {
            holder.txtEstadoTarea.setText("Pendiente");
            holder.txtNomAct.setTypeface(null, Typeface.BOLD);
            holder.txtEstadoTarea.setAlpha(1.0f);
        }

        // Guardar el ID de esta tarea para abrir su detalle.
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(
                    v.getContext(),
                    DetalleTarea.class
            );

            intent.putExtra("id_act", idAct);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null || cursor.isClosed()
                ? 0
                : cursor.getCount();
    }

    public static class TareaViewHolder extends RecyclerView.ViewHolder {

        TextView txtNomAct;
        TextView txtDescripAct;
        TextView txtFechaEntrega;
        TextView txtPrioridad;
        TextView txtEstadoTarea;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);

            txtNomAct = itemView.findViewById(R.id.txtNomAct);
            txtDescripAct = itemView.findViewById(R.id.txtDescripAct);
            txtFechaEntrega = itemView.findViewById(R.id.txtFechaEntrega);
            txtPrioridad = itemView.findViewById(R.id.txtPrioridad);
            txtEstadoTarea = itemView.findViewById(R.id.txtEstadoTarea);
        }
    }

    public Cursor getCursorActual() {
        return cursor;
    }
}
