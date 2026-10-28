
package com.example.pamarte_e5;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class Recordatorios {

    private static final int ID_CLASES = 100;
    private static final int ID_TAREAS = 900;

    private static final String[] DIAS = {
            "Lunes",
            "Martes",
            "Miercoles",
            "Jueves",
            "Viernes",
            "Sabado",
            "Domingo"
    };

    private static final int[] DIAS_CALENDAR = {
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
    };

    public static void programarRecordatoriosClases(
            Context contexto,
            String idEstudiante,
            JSONObject horario) {

        if (idEstudiante == null || idEstudiante.trim().isEmpty()
                || horario == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager)
                contexto.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        for (int i = 0; i < DIAS.length; i++) {

            int requestCode = obtenerCodigoClase(idEstudiante, i);

            PendingIntent anterior = crearPendingIntentClase(
                    contexto,
                    idEstudiante,
                    DIAS[i],
                    requestCode
            );

            // Elimina el recordatorio anterior de ese día.
            alarmManager.cancel(anterior);

            JSONArray clases = horario.optJSONArray(DIAS[i]);

            if (clases == null || clases.length() == 0) {
                continue;
            }

            String ultimaHora = "";

            for (int j = 0; j < clases.length(); j++) {

                JSONObject clase = clases.optJSONObject(j);

                if (clase == null) {
                    continue;
                }

                String horaFin = clase.optString("hora_fin", "");

                if (horaFin.matches("\\d{2}:\\d{2}")
                        && horaValida(horaFin)
                        && horaFin.compareTo(ultimaHora) > 0) {

                    ultimaHora = horaFin;
                }
            }

            if (ultimaHora.isEmpty()) {
                continue;
            }

            String[] partes = ultimaHora.split(":");

            int hora = Integer.parseInt(partes[0]);
            int minuto = Integer.parseInt(partes[1]);

            Calendar fecha = Calendar.getInstance();

            fecha.set(Calendar.DAY_OF_WEEK, DIAS_CALENDAR[i]);
            fecha.set(Calendar.HOUR_OF_DAY, hora);
            fecha.set(Calendar.MINUTE, minuto);
            fecha.set(Calendar.SECOND, 0);
            fecha.set(Calendar.MILLISECOND, 0);

            // Recordatorio cinco minutos después de la última clase.
            fecha.add(Calendar.MINUTE, 5);

            // Si ya pasó la hora, programa el aviso para la próxima semana.
            if (fecha.getTimeInMillis() <= System.currentTimeMillis()) {
                fecha.add(Calendar.WEEK_OF_YEAR, 1);
            }

            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    fecha.getTimeInMillis(),
                    anterior
            );
        }
    }

    private static int obtenerCodigoClase(
            String idEstudiante,
            int indiceDia) {

        return ID_CLASES
                + Math.abs(idEstudiante.hashCode() % 100000) * 7
                + indiceDia;
    }

    private static boolean horaValida(String hora) {

        try {
            String[] partes = hora.split(":");

            int h = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]);

            return h >= 0 && h <= 23
                    && m >= 0 && m <= 59;

        } catch (Exception e) {
            return false;
        }
    }

    private static PendingIntent crearPendingIntentClase(
            Context contexto,
            String idEstudiante,
            String dia,
            int requestCode) {

        Intent intent = new Intent(
                contexto,
                RecordatoriosReceiver.class
        );

        intent.putExtra("tipo", "clases");
        intent.putExtra("idEstudiante", idEstudiante);
        intent.putExtra("dia", dia);

        return PendingIntent.getBroadcast(
                contexto,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static void programarRecordatorioTareas(
            Context contexto,
            String idEstudiante) {

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager)
                contexto.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(
                contexto,
                RecordatoriosReceiver.class
        );

        intent.putExtra("tipo", "tareas");
        intent.putExtra("idEstudiante", idEstudiante);

        int requestCodeTareas = ID_TAREAS
                + Math.abs(idEstudiante.hashCode() % 100000);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                contexto,
                requestCodeTareas,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        DatabaseHelper db = new DatabaseHelper(contexto);
        int pendientes;

        try {
            pendientes = db.contarTareasPendientes(idEstudiante);
        } finally {
            db.close();
        }

        if (pendientes <= 0) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        // Primer aviso dentro de una hora.
        long siguienteAviso = System.currentTimeMillis()
                + 60L * 60L * 1000L;

        alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                siguienteAviso,
                pendingIntent
        );
    }
}
