package com.example.pamarte_e5;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DatabaseHelper extends SQLiteOpenHelper{
    public static final String BASE_DATOS_NOM = "amarte_tareas";
    public static final int BASE_DATOS_VER = 1;

    public DatabaseHelper(Context context){
        super (context, BASE_DATOS_NOM, null, BASE_DATOS_VER);
    }

    @Override
    public void onConfigure(SQLiteDatabase db){
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
    @Override
    public void onCreate(SQLiteDatabase db){
        String tab_estudiante = "CREATE TABLE estudiante(" +
                "id CHAR(32) PRIMARY KEY," +
                "id_estudiante CHAR(32) NOT NULL," +
                "usuario VARCHAR(32) NOT NULL," +
                "nombre TEXT," +
                "correo VARCHAR(255) NOT NULL," +
                "passw CHAR(255) NOT NULL," +
                "status ENUM( 'ACTIVO', 'INACTIVO') DEFUALT 'ACTIVO';" +
                "horario TEXT," +
                "pendientes int," +
                ");";

        String tab_acts = "CREATE TABLE actividades(" +
                "id_act CHAR(32) PRIMARY KEY," +
                "id_estudiante CHAR(32) NOT NULL," +
                "f_asign DATE NOT NULL," +
                "f_ent DATE NOT NULL," +
                "nom_act TEXT NOT NULL," +
                "descrip_act TEXT," +
                "pasos TEXT," +
                "prioridad ENUM('ALTA', 'MEDIA', 'BAJA')," +
                "FOREIGN KEY (id_estudiante) REFERENCES estudiante(id_estudiante) ON DELETE CASCADE" +
                ");";

        db.execSQL(tab_estudiante);
        db.execSQL(tab_acts);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int nVersion){
        db.execSQL("DROP TABLE IF EXISTS estudiante");
        db.execSQL("DROP TABLE IF EXISTS actividades");
        onCreate(db);
    }

}
