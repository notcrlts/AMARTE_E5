
package com.example.pamarte_e5;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

public class Recordatorios {

    private static final String TAG = "Recordatorios";

    private static final int ID_CLASES = 100;
    private static final int ID_TAREAS = 900;

    private static final String ACCION_CLASES =
            "com.example.pamarte_e5.RECORDATORIO_CLASES";

    private static final String ACCION_TAREAS =
            "com.example.pamarte_e5.RECORDATORIO_TAREAS";

    private static final String[] DIAS = {
            "Lunes", "Martes", "Miercoles", "Jueves",
            "Viernes", "Sabado", "Domingo"
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

    // Programa los recordatorios cinco minutos después
    // de la última clase de cada día.
    public static void programarRecordatoriosClases(
            Context contexto,
            String idEstudiante,
            JSONObject horario) {

        if (idEstudiante == null
                || idEstudiante.trim().isEmpty()
                || horario == null) {
            Log.e(TAG, "No se pudo programar: ID u horario inválido");
            return;
        }

        AlarmManager alarmManager = (AlarmManager)
                contexto.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        for (int i = 0; i < DIAS.length; i++) {

            int requestCode = obtenerCodigoClase(idEstudiante, i);

            PendingIntent pendiente = crearPendingIntentClase(
                    contexto, idEstudiante, DIAS[i], requestCode);

            alarmManager.cancel(pendiente);

            JSONArray clases = horario.optJSONArray(DIAS[i]);

            if (clases == null || clases.length() == 0) {
                Log.d(TAG, "Sin clases el día " + DIAS[i]);
                continue;
            }

            String ultimaHora = "";

            for (int j = 0; j < clases.length(); j++) {

                JSONObject clase = clases.optJSONObject(j);

                if (clase == null) {
                    continue;
                }

                String horaFin = clase.optString("hora_fin", "");

                if (horaValida(horaFin)
                        && horaFin.compareTo(ultimaHora) > 0) {
                    ultimaHora = horaFin;
                }
            }

            if (ultimaHora.isEmpty()) {
                Log.w(TAG, "No se encontró hora_fin para " + DIAS[i]);
                continue;
            }

            String[] partes = ultimaHora.split(":");

            int hora = Integer.parseInt(partes[0]);
            int minuto = Integer.parseInt(partes[1]);

            Calendar fecha = calcularProximaFecha(
                    DIAS_CALENDAR[i], hora, minuto);

            long horaProgramada = fecha.getTimeInMillis();

            Log.d(TAG, "Clase " + DIAS[i]
                    + " | última clase: " + ultimaHora
                    + " | aviso: " + fecha.getTime());

            try {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        horaProgramada,
                        pendiente
                );
            } catch (SecurityException e) {
                Log.e(TAG, "Error al programar clases", e);
            }
        }
    }

    private static Calendar calcularProximaFecha(
            int diaObjetivo,
            int hora,
            int minuto) {

        Calendar ahora = Calendar.getInstance();
        Calendar fecha = Calendar.getInstance();

        fecha.set(Calendar.HOUR_OF_DAY, hora);
        fecha.set(Calendar.MINUTE, minuto);
        fecha.set(Calendar.SECOND, 0);
        fecha.set(Calendar.MILLISECOND, 0);

        int diaActual = ahora.get(Calendar.DAY_OF_WEEK);

        int diferencia = (diaObjetivo - diaActual + 7) % 7;

        fecha.add(Calendar.DAY_OF_YEAR, diferencia);

        // El aviso es cinco minutos después de terminar.
        fecha.add(Calendar.MINUTE, 5);

        // Si el aviso ya pasó, programarlo para la próxima semana.
        if (fecha.getTimeInMillis() <= ahora.getTimeInMillis()) {
            fecha.add(Calendar.DAY_OF_YEAR, 7);
        }

        return fecha;
    }

    private static int obtenerCodigoClase(
            String idEstudiante,
            int indiceDia) {

        return ID_CLASES
                + Math.abs(idEstudiante.hashCode() % 100000) * 7
                + indiceDia;
    }

    private static boolean horaValida(String hora) {

        if (hora == null || !hora.matches("\\d{2}:\\d{2}")) {
            return false;
        }

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
                contexto, RecordatoriosReceiver.class);

        intent.setAction(ACCION_CLASES);
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

    // Programa avisos de tareas pendientes cada 10 minutos.
    public static void programarRecordatorioTareas(
            Context contexto,
            String idEstudiante) {

        if (idEstudiante == null
                || idEstudiante.trim().isEmpty()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager)
                contexto.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(
                contexto, RecordatoriosReceiver.class);

        intent.setAction(ACCION_TAREAS);
        intent.putExtra("tipo", "tareas");
        intent.putExtra("idEstudiante", idEstudiante);

        int requestCode = ID_TAREAS
                + Math.abs(idEstudiante.hashCode() % 100000);

        PendingIntent pendiente = PendingIntent.getBroadcast(
                contexto,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        int tareasPendientes;

        DatabaseHelper db = new DatabaseHelper(contexto);

        try {
            tareasPendientes =
                    db.contarTareasPendientes(idEstudiante);
        } catch (Exception e) {
            Log.e(TAG, "Error al consultar tareas pendientes", e);
            return;
        } finally {
            db.close();
        }

        if (tareasPendientes <= 0) {
            alarmManager.cancel(pendiente);
            Log.d(TAG, "Sin tareas pendientes; alarma cancelada");
            return;
        }

        // Diez minutos desde este momento.
        long siguienteAviso = System.currentTimeMillis()
                + 10L * 60L * 1000L;

        try {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    siguienteAviso,
                    pendiente
            );

            Log.d(TAG, "Aviso de tareas programado en 10 minutos");

        } catch (SecurityException e) {
            Log.e(TAG, "No se pudo programar el aviso de tareas", e);
        }
    }

    // Alarma de prueba para comprobar el receptor.
    public static boolean programarPrueba(Context contexto) {

        Intent intent = new Intent(
                contexto, RecordatoriosReceiver.class);

        intent.setAction(
                "com.example.pamarte_e5.RECORDATORIO_PRUEBA");

        intent.putExtra("tipo", "prueba");
        intent.putExtra("idEstudiante", "prueba");

        PendingIntent pendiente = PendingIntent.getBroadcast(
                contexto,
                987654,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager)
                contexto.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            return false;
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 10_000L,
                    pendiente
            );

            return true;

        } catch (SecurityException e) {
            Log.e(TAG, "Error en alarma de prueba", e);
            return false;
        }
    }
}
