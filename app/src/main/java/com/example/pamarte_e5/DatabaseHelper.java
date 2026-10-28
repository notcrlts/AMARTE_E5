
package com.example.pamarte_e5;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String BASE_DATOS_NOM = "amarte_tareas";
    public static final int BASE_DATOS_VER = 6;

    private String ultimoError = "";

    public DatabaseHelper(Context context) {
        super(context, BASE_DATOS_NOM, null, BASE_DATOS_VER);
    }

    public String obtenerUltimoError() {
        return ultimoError;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String tabEstudiante =
                "CREATE TABLE estudiante (" +
                        "id CHAR(32) PRIMARY KEY," +
                        "id_estudiante CHAR(32) NOT NULL UNIQUE," +
                        "usuario VARCHAR(32) NOT NULL," +
                        "nombre TEXT," +
                        "correo VARCHAR(255) NOT NULL," +
                        "passw CHAR(255) NOT NULL," +
                        "status TEXT DEFAULT 'ACTIVO' " +
                        "CHECK(status IN ('ACTIVO', 'INACTIVO'))," +
                        "horario TEXT," +
                        "pendientes INTEGER DEFAULT 0, " +
                        "foto_perfil TEXT" +
                        ");";

        String tabActividades =
                "CREATE TABLE actividades (" +
                        "id_act CHAR(32) PRIMARY KEY," +
                        "id_estudiante CHAR(32) NOT NULL," +
                        "f_asign DATE NOT NULL," +
                        "f_ent DATE NOT NULL," +
                        "nom_act TEXT NOT NULL," +
                        "descrip_act TEXT," +
                        "pasos TEXT," +
                        "prioridad TEXT " +
                        "CHECK(prioridad IN ('ALTA', 'MEDIA', 'BAJA'))," +
                        "completado INTEGER DEFAULT 0 " +
                        "CHECK(completado IN (0, 1))," +
                        "FOREIGN KEY (id_estudiante) " +
                        "REFERENCES estudiante(id_estudiante) " +
                        "ON DELETE CASCADE" +
                        ");";

        db.execSQL(tabEstudiante);
        db.execSQL(tabActividades);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 5) {
            Cursor cursor = db.rawQuery(
                    "PRAGMA table_info(actividades)",
                    null
            );

            boolean existeCompletado = false;

            try {
                int columnaNombre = cursor.getColumnIndexOrThrow("name");

                while (cursor.moveToNext()) {
                    String nombreColumna = cursor.getString(columnaNombre);

                    if ("completado".equals(nombreColumna)) {
                        existeCompletado = true;
                        break;
                    }
                }
            } finally {
                cursor.close();
            }

            if (!existeCompletado) {
                db.execSQL(
                        "ALTER TABLE actividades " +
                                "ADD COLUMN completado INTEGER NOT NULL DEFAULT 0 " +
                                "CHECK(completado IN (0, 1))"
                );
            }
        }
        if (oldVersion < 6) {

            // Verificar si existe la columna foto_perfil.
            boolean existeFoto = false;

            try (Cursor cursor = db.rawQuery(
                    "PRAGMA table_info(estudiante)", null)) {

                int indiceNombre = cursor.getColumnIndex("name");

                while (cursor.moveToNext()) {
                    if ("foto_perfil".equals(
                            cursor.getString(indiceNombre))) {
                        existeFoto = true;
                        break;
                    }
                }
            }

            if (!existeFoto) {
                db.execSQL(
                        "ALTER TABLE estudiante ADD COLUMN foto_perfil TEXT"
                );
            }

        }
    }

    public boolean existeEstudiante(String idEstudiante) {

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            return false;
        }

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM estudiante WHERE id_estudiante = ?",
                new String[]{idEstudiante.trim()})) {

            return cursor.moveToFirst();

        } catch (SQLException e) {
            ultimoError = "Error al consultar estudiante: "
                    + e.getMessage();
            return false;
        }
    }

    public boolean insertarEstudiante(
            String usuario,
            String nombre,
            String correo,
            String passw,
            String horario) {

        ultimoError = "";

        SQLiteDatabase db = getWritableDatabase();

        String idInterno = UUID.randomUUID()
                .toString().replace("-", "");

        String idEstudianteAuto = "EST_" +
                UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 8);

        ContentValues valores = new ContentValues();

        valores.put("id", idInterno);
        valores.put("id_estudiante", idEstudianteAuto);
        valores.put("usuario", usuario);
        valores.put("nombre", nombre);
        valores.put("correo", correo);
        valores.put("passw", passw);
        valores.put("status", "ACTIVO");
        valores.put("horario", horario);
        valores.put("pendientes", 0);

        try {
            db.insertOrThrow("estudiante", null, valores);
            return true;

        } catch (SQLException e) {
            ultimoError = "Error al registrar estudiante: "
                    + e.getMessage();
            return false;
        }
    }

    public String verificarUsuario(String correo, String passw) {

        ultimoError = "";

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT id_estudiante FROM estudiante " +
                        "WHERE correo = ? AND passw = ? " +
                        "AND status = 'ACTIVO'",
                new String[]{correo, passw})) {

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }

        } catch (SQLException e) {
            ultimoError = "Error al iniciar sesión: "
                    + e.getMessage();
        }

        return null;
    }

    public boolean insertarTarea(
            String idEstudiante,
            String fAsign,
            String fEnt,
            String nomAct,
            String descripAct,
            String pasosJson,
            String prioridad) {

        ultimoError = "";

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            ultimoError = "El ID del estudiante está vacío.";
            return false;
        }

        idEstudiante = idEstudiante.trim();

        SQLiteDatabase db = getWritableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT 1 FROM estudiante WHERE id_estudiante = ?",
                new String[]{idEstudiante})) {

            if (!cursor.moveToFirst()) {
                ultimoError = "El ID '" + idEstudiante
                        + "' no existe en la tabla estudiante.";
                return false;
            }

        } catch (SQLException e) {
            ultimoError = "Error al comprobar estudiante: "
                    + e.getMessage();
            return false;
        }

        String idActUnico = UUID.randomUUID()
                .toString().replace("-", "");

        ContentValues valores = new ContentValues();

        valores.put("id_act", idActUnico);
        valores.put("id_estudiante", idEstudiante);
        valores.put("f_asign", fAsign);
        valores.put("f_ent", fEnt);
        valores.put("nom_act", nomAct);
        valores.put("descrip_act", descripAct);
        valores.put("pasos", pasosJson);
        valores.put("prioridad", prioridad);
        valores.put("completado", 0);

        try {
            db.insertOrThrow("actividades", null, valores);
            return true;

        } catch (SQLException e) {
            ultimoError = "Error SQLite al guardar tarea: "
                    + e.getMessage();
            return false;
        }
    }

    public int actualizarEstadoTarea(
            String idAct,
            int estadoCompletado) {

        ultimoError = "";

        if (idAct == null ||
                idAct.trim().isEmpty() ||
                (estadoCompletado != 0 && estadoCompletado != 1)) {

            ultimoError = "ID de tarea o estado no válido.";
            return 0;
        }

        SQLiteDatabase db = getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("completado", estadoCompletado);

        try {
            return db.update(
                    "actividades",
                    valores,
                    "id_act = ?",
                    new String[]{idAct}
            );

        } catch (SQLException e) {
            ultimoError = "Error al actualizar el estado: "
                    + e.getMessage();
            return 0;
        }
    }

    public int actualizarPasosTarea(
            String idAct,
            String nuevosPasosJson) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("pasos", nuevosPasosJson);

        return db.update(
                "actividades",
                valores,
                "id_act = ?",
                new String[]{idAct}
        );
    }

    public boolean actualizarDatosTarea(
            String idAct,
            String nomAct,
            String descripAct,
            String prioridad) {

        SQLiteDatabase db = getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("nom_act", nomAct);
        valores.put("descrip_act", descripAct);
        valores.put("prioridad", prioridad);

        return db.update(
                "actividades",
                valores,
                "id_act = ?",
                new String[]{idAct}
        ) > 0;
    }

    public Cursor obtenerTareas(String idEstudiante) {

        SQLiteDatabase db = getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM actividades WHERE id_estudiante = ?",
                new String[]{idEstudiante}
        );
    }

    public Cursor obtenerDatosEstudiante(String idEstudiante) {

        SQLiteDatabase db = getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM estudiante WHERE id_estudiante = ?",
                new String[]{idEstudiante}
        );
    }

    public double calcularPorcentTareas(String idEstudiante) {

        SQLiteDatabase db = getReadableDatabase();

        String consulta =
                "SELECT COUNT(*), " +
                        "COALESCE(SUM(completado), 0) " +
                        "FROM actividades WHERE id_estudiante = ?";

        try (Cursor cursor = db.rawQuery(
                consulta,
                new String[]{idEstudiante})) {

            if (cursor.moveToFirst()) {
                int total = cursor.getInt(0);
                int completadas = cursor.getInt(1);

                if (total > 0) {
                    return completadas * 100.0 / total;
                }
            }

        } catch (SQLException e) {
            ultimoError = "Error al calcular el porcentaje: "
                    + e.getMessage();
        }

        return 0.0;
    }

    public int actualizarHorario(
            String idEstudiante,
            String horarioJson) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues valores = new ContentValues();
        valores.put("horario", horarioJson);

        return db.update(
                "estudiante",
                valores,
                "id_estudiante = ?",
                new String[]{idEstudiante}
        );
    }
    public int contarTareasPendientes(String idEstudiante) {
        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            return 0;
        }

        SQLiteDatabase db = getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM actividades " +
                        "WHERE id_estudiante = ? AND completado = 0",
                new String[]{idEstudiante.trim()})) {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

        } catch (SQLException e) {
            ultimoError = "Error al contar tareas pendientes: " + e.getMessage();
        }

        return 0;
    }


    public int actualizarFotoPerfil(
            String idEstudiante,
            String rutaFoto) {

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            return 0;
        }

        ContentValues valores = new ContentValues();
        valores.put("foto_perfil", rutaFoto);

        SQLiteDatabase db = getWritableDatabase();

        return db.update(
                "estudiante",
                valores,
                "id_estudiante = ?",
                new String[]{idEstudiante.trim()}
        );
    }
}
