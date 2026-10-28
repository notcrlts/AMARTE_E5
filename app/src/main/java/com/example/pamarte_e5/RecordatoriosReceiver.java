
package com.example.pamarte_e5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

public class RecordatoriosReceiver extends BroadcastReceiver {

    private static final String TAG = "RecordatorioReceiver";
    private static final String CANAL = "recordatorios";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null) {
            return;
        }

        String tipo = intent.getStringExtra("tipo");
        String idEstudiante = intent.getStringExtra("idEstudiante");

        if (tipo == null || idEstudiante == null
                || idEstudiante.trim().isEmpty()) {
            return;
        }

        Usuario.crearCanalNotificaciones(context);
        if ("prueba".equals(tipo)) {
            mostrarNotificacion(
                    context,
                    987654,
                    "Prueba de alarma",
                    "El recordatorio programado está funcionando."
            );
            return;
        }
        DatabaseHelper db = new DatabaseHelper(context);

        try {

            if ("clases".equals(tipo)) {

                String dia = intent.getStringExtra("dia");

                int idNotificacion = 200
                        + Math.abs(idEstudiante.hashCode() % 10000)
                        + (dia == null ? 0
                        : Math.abs(dia.hashCode() % 100));

                mostrarNotificacion(
                        context,
                        idNotificacion,
                        "¿Terminaste tus clases?",
                        "Recuerda registrar las tareas que te dejaron."
                );

                // Consultar el horario actualizado del estudiante.
                String horarioGuardado = null;

                try (Cursor cursor =
                             db.obtenerDatosEstudiante(idEstudiante)) {

                    if (cursor != null && cursor.moveToFirst()) {

                        int columna = cursor.getColumnIndex("horario");

                        if (columna >= 0 && !cursor.isNull(columna)) {
                            horarioGuardado = cursor.getString(columna);
                        }
                    }
                }

                // Programar los siguientes recordatorios semanales.
                if (horarioGuardado != null
                        && !horarioGuardado.trim().isEmpty()) {

                    JSONObject horario = new JSONObject(horarioGuardado);

                    Recordatorios.programarRecordatoriosClases(
                            context,
                            idEstudiante,
                            horario
                    );
                }

            } else if ("tareas".equals(tipo)) {

                int pendientes =
                        db.contarTareasPendientes(idEstudiante);

                if (pendientes > 0) {

                    mostrarNotificacion(
                            context,
                            901 + Math.abs(
                                    idEstudiante.hashCode() % 10000
                            ),
                            "Tienes tareas pendientes",
                            "Te quedan " + pendientes
                                    + " tarea(s) por completar."
                    );

                    // Programar otro aviso dentro de una hora.
                    Recordatorios.programarRecordatorioTareas(
                            context,
                            idEstudiante
                    );
                }

            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Error al procesar el recordatorio",
                    e
            );

        } finally {
            db.close();
        }
    }

    private void mostrarNotificacion(
            Context context,
            int idNotificacion,
            String titulo,
            String mensaje) {

        NotificationCompat.Builder notificacion =
                new NotificationCompat.Builder(context, CANAL)
                        .setSmallIcon(
                                android.R.drawable.ic_dialog_info
                        )
                        .setContentTitle(titulo)
                        .setContentText(mensaje)
                        .setPriority(
                                NotificationCompat.PRIORITY_DEFAULT
                        )
                        .setAutoCancel(true);

        try {

            NotificationManagerCompat
                    .from(context)
                    .notify(idNotificacion, notificacion.build());

        } catch (SecurityException e) {

            Log.w(
                    TAG,
                    "No se concedió permiso para las notificaciones.",
                    e
            );
        }
    }
}
