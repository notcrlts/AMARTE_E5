
package com.example.pamarte_e5;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;
import org.json.JSONObject;
import android.widget.Toast;

public class Dash extends Fragment {

    private static final String ARG_ID_ESTUDIANTE = "ID_ESTUDIANTE";

    private String idEstudianteActual;

    private DatabaseHelper dbHelper;

    private TextView txtNombreDash;
    private TextView txtPorcentaje;
    private TextView txtTareasPendientes;
    private TextView txtTareasCompletadas;

    private CircularProgressIndicator progressTareas;

    public Dash() {
        // Constructor vacío requerido.
    }

    public static Dash newInstance(String idEstudiante) {
        Dash fragment = new Dash();

        Bundle args = new Bundle();
        args.putString(ARG_ID_ESTUDIANTE, idEstudiante);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            idEstudianteActual =
                    getArguments().getString(ARG_ID_ESTUDIANTE);
        }

        // Recuperar el ID de la sesión si no llegó como argumento.
        if (idEstudianteActual == null ||
                idEstudianteActual.trim().isEmpty()) {

            if (requireContext()
                    .getSharedPreferences("SesionApp",
                            android.content.Context.MODE_PRIVATE)
                    .contains("ID_ESTUDIANTE")) {

                idEstudianteActual = requireContext()
                        .getSharedPreferences("SesionApp",
                                android.content.Context.MODE_PRIVATE)
                        .getString("ID_ESTUDIANTE", null);
            }
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_dash,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        txtNombreDash = view.findViewById(R.id.txtNombreDash);
        txtPorcentaje = view.findViewById(R.id.txtPorcentaje);
        txtTareasPendientes = view.findViewById(R.id.txtTareasPendientes);
        txtTareasCompletadas = view.findViewById(R.id.txtTareasCompletadas);
        progressTareas = view.findViewById(R.id.progressTareas);

        actualizarDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();

        actualizarDashboard();
        sincronizarRecordatorios();
    }

    private void actualizarDashboard() {

        if (dbHelper == null ||
                idEstudianteActual == null ||
                idEstudianteActual.trim().isEmpty() ||
                txtPorcentaje == null) {
            return;
        }

        cargarNombreEstudiante(idEstudianteActual);

        double porcentaje =
                dbHelper.calcularPorcentTareas(idEstudianteActual);

        int pendientes = 0;
        int completadas = 0;

        try (Cursor cursor =
                     dbHelper.obtenerTareas(idEstudianteActual)) {

            if (cursor != null && cursor.moveToFirst()) {

                int columnaEstado =
                        cursor.getColumnIndexOrThrow("completado");

                do {
                    int estado = cursor.getInt(columnaEstado);

                    if (estado == 1) {
                        completadas++;
                    } else {
                        pendientes++;
                    }

                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            return;
        }

        // Evitar valores fuera del rango del indicador.
        int progreso = (int) Math.round(porcentaje);
        progreso = Math.max(0, Math.min(100, progreso));

        txtPorcentaje.setText(
                String.format(
                        Locale.getDefault(),
                        "%.0f%% completado",
                        porcentaje
                )
        );

        if (progressTareas != null) {
            progressTareas.setProgress(progreso);
        }

        if (txtTareasPendientes != null) {
            txtTareasPendientes.setText(
                    String.valueOf(pendientes)
            );
        }

        if (txtTareasCompletadas != null) {
            txtTareasCompletadas.setText(
                    String.valueOf(completadas)
            );
        }
    }

    private void cargarNombreEstudiante(String idEstudiante) {

        try (Cursor cursor =
                     dbHelper.obtenerDatosEstudiante(idEstudiante)) {

            if (cursor != null && cursor.moveToFirst()) {

                String nombre = cursor.getString(
                        cursor.getColumnIndexOrThrow("nombre")
                );

                if (txtNombreDash != null) {
                    if (nombre == null || nombre.trim().isEmpty()) {
                        txtNombreDash.setText("Usuario");
                    } else {
                        txtNombreDash.setText(nombre);
                    }
                }

            } else if (txtNombreDash != null) {
                txtNombreDash.setText("Usuario");
            }

        } catch (Exception e) {

            if (txtNombreDash != null) {
                txtNombreDash.setText("Usuario");
            }
        }
    }

    @Override
    public void onDestroyView() {

        txtNombreDash = null;
        txtPorcentaje = null;
        txtTareasPendientes = null;
        txtTareasCompletadas = null;
        progressTareas = null;

        super.onDestroyView();
    }

    private void sincronizarRecordatorios() {

        if (dbHelper == null
                || idEstudianteActual == null
                || idEstudianteActual.trim().isEmpty()) {
            return;
        }

        String id = idEstudianteActual.trim();

        // Programar o actualizar los recordatorios de tareas.
        Recordatorios.programarRecordatorioTareas(
                requireContext(),
                id
        );

        // Recuperar el horario guardado del estudiante.
        try (Cursor cursor = dbHelper.obtenerDatosEstudiante(id)) {

            if (cursor != null && cursor.moveToFirst()) {

                int columnaHorario =
                        cursor.getColumnIndex("horario");

                if (columnaHorario != -1) {

                    String horarioGuardado =
                            cursor.getString(columnaHorario);

                    if (horarioGuardado != null
                            && !horarioGuardado.trim().isEmpty()) {

                        JSONObject horario =
                                new JSONObject(horarioGuardado);

                        Recordatorios.programarRecordatoriosClases(
                                requireContext(),
                                id,
                                horario
                        );
                    }
                }

            }

        } catch (Exception e) {
            android.util.Log.e(
                    "Dash",
                    "Error al sincronizar recordatorios",
                    e
            );
        }
    }
}
