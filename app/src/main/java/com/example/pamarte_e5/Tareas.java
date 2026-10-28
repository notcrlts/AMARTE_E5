
package com.example.pamarte_e5;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Tareas extends Fragment {

    private DatabaseHelper dbHelper;
    private RecyclerView rvFormatos;
    private TareaAdapter tareaAdapter;
    private FloatingActionButton fabPrincipal;

    private String idEstudianteActual = "";
    private String fechaDbSeleccionada = "";

    public Tareas() {
        // Constructor requerido.
    }

    public static Tareas newInstance(String idEstudiante) {
        Tareas fragment = new Tareas();

        Bundle args = new Bundle();
        args.putString("ID_ESTUDIANTE", idEstudiante);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            idEstudianteActual =
                    getArguments().getString("ID_ESTUDIANTE", "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_tareas,
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

        SharedPreferences prefs =
                requireActivity().getSharedPreferences(
                        "SesionApp",
                        Context.MODE_PRIVATE
                );

        String idArgumento = idEstudianteActual == null
                ? ""
                : idEstudianteActual.trim();

        String idSesion = prefs.getString("ID_ESTUDIANTE", "");

        if (idSesion == null) {
            idSesion = "";
        }

        idSesion = idSesion.trim();

        // Primero comprobar la ID recibida por el fragmento.
        if (!idArgumento.isEmpty()
                && dbHelper.existeEstudiante(idArgumento)) {

            idEstudianteActual = idArgumento;

        } else if (!idSesion.isEmpty()
                && dbHelper.existeEstudiante(idSesion)) {

            // Si la anterior no sirve, comprobar la sesión guardada.
            idEstudianteActual = idSesion;

        } else {

            // Ninguna de las dos ID existe en la base de datos.
            idEstudianteActual = "";

            Toast.makeText(
                    requireContext(),
                    "La sesión no corresponde a un estudiante registrado. Cierra sesión e inicia nuevamente.",
                    Toast.LENGTH_LONG
            ).show();
        }

        rvFormatos = view.findViewById(R.id.rvFormatos);
        fabPrincipal = view.findViewById(R.id.fabPrincipal);

        rvFormatos.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        Cursor cursor = dbHelper.obtenerTareas(idEstudianteActual);

        tareaAdapter = new TareaAdapter(cursor);
        rvFormatos.setAdapter(tareaAdapter);

        fabPrincipal.setOnClickListener(v -> {
            if (!comprobarEstudiante()) {
                return;
            }

            mostrarDialogoAgregarTarea();
        });
    }

    private boolean comprobarEstudiante() {

        if (idEstudianteActual == null
                || idEstudianteActual.trim().isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "No se encontró la ID del estudiante. Inicia sesión nuevamente.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        if (!dbHelper.existeEstudiante(idEstudianteActual.trim())) {

            Toast.makeText(
                    requireContext(),
                    "La ID " + idEstudianteActual
                            + " no existe en la tabla estudiante.",
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }

        return true;
    }

    private void mostrarDialogoAgregarTarea() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(requireContext());

        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_agregar_tarea,
                null
        );

        builder.setView(dialogView);

        EditText txteNomAct =
                dialogView.findViewById(R.id.txteNomAct);

        EditText txteDescripAct =
                dialogView.findViewById(R.id.txteDescripAct);

        EditText txteFechaEnt =
                dialogView.findViewById(R.id.txteFechaEnt);

        AutoCompleteTextView txtePrioridad =
                dialogView.findViewById(R.id.txtePrioridad);

        EditText txtePaso =
                dialogView.findViewById(R.id.txtePaso);

        Button btnAgregarPaso =
                dialogView.findViewById(R.id.btnAgregarPaso);

        LinearLayout layoutPasosContainer =
                dialogView.findViewById(R.id.layoutPasosContainer);

        Button btnGuardarTarea =
                dialogView.findViewById(R.id.btnGuardarTarea);

        String[] prioridades = {
                "ALTA", "MEDIA", "BAJA"
        };

        ArrayAdapter<String> adapterPrioridad =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        prioridades
                );

        txtePrioridad.setAdapter(adapterPrioridad);

        txtePrioridad.setOnClickListener(
                v -> txtePrioridad.showDropDown()
        );

        fechaDbSeleccionada = "";

        txteFechaEnt.setFocusable(false);

        txteFechaEnt.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            int anio = calendar.get(Calendar.YEAR);
            int mes = calendar.get(Calendar.MONTH);
            int dia = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog =
                    new DatePickerDialog(
                            requireContext(),
                            (datePicker, year, month, dayOfMonth) -> {

                                String fechaVisual = String.format(
                                        Locale.getDefault(),
                                        "%02d-%02d-%04d",
                                        dayOfMonth,
                                        month + 1,
                                        year
                                );

                                fechaDbSeleccionada = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        year,
                                        month + 1,
                                        dayOfMonth
                                );

                                txteFechaEnt.setText(fechaVisual);
                            },
                            anio,
                            mes,
                            dia
                    );

            datePickerDialog.show();
        });

        JSONArray jsonArrayPasos = new JSONArray();

        btnAgregarPaso.setOnClickListener(v -> {

            String textoPaso =
                    txtePaso.getText().toString().trim();

            if (textoPaso.isEmpty()) {
                Toast.makeText(
                        requireContext(),
                        "Escribe un paso válido",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            try {
                JSONObject pasoObj = new JSONObject();

                pasoObj.put("texto", textoPaso);
                pasoObj.put("completado", false);

                jsonArrayPasos.put(pasoObj);

                TextView tvPasoItem =
                        new TextView(requireContext());

                tvPasoItem.setText("• " + textoPaso);
                tvPasoItem.setTextSize(14);

                layoutPasosContainer.addView(tvPasoItem);

                txtePaso.setText("");

            } catch (Exception e) {

                Toast.makeText(
                        requireContext(),
                        "Error al agregar el paso: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        AlertDialog dialog = builder.create();

        btnGuardarTarea.setOnClickListener(v -> {

            // Verificar que el estudiante exista antes de guardar.
            if (!comprobarEstudiante()) {
                return;
            }

            String nomAct =
                    txteNomAct.getText().toString().trim();

            String descripAct =
                    txteDescripAct.getText().toString().trim();

            String prioridad =
                    txtePrioridad.getText().toString()
                            .trim().toUpperCase(Locale.ROOT);

            if (nomAct.isEmpty()
                    || fechaDbSeleccionada.isEmpty()
                    || prioridad.isEmpty()) {

                Toast.makeText(
                        requireContext(),
                        "Completa el nombre, la fecha y la prioridad",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (!prioridad.equals("ALTA")
                    && !prioridad.equals("MEDIA")
                    && !prioridad.equals("BAJA")) {

                Toast.makeText(
                        requireContext(),
                        "Selecciona una prioridad válida",
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String pasosJsonString = jsonArrayPasos.toString();

            String fechaAsignacion =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    ).format(new Date());

            boolean insertado;

            try {
                insertado = dbHelper.insertarTarea(
                        idEstudianteActual.trim(),
                        fechaAsignacion,
                        fechaDbSeleccionada,
                        nomAct,
                        descripAct,
                        pasosJsonString,
                        prioridad
                );

            } catch (Exception e) {

                Toast.makeText(
                        requireContext(),
                        "Error al guardar: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            if (insertado) {

                Toast.makeText(
                        requireContext(),
                        "¡Tarea creada con éxito!",
                        Toast.LENGTH_SHORT
                ).show();

                Cursor nuevoCursor =
                        dbHelper.obtenerTareas(
                                idEstudianteActual.trim()
                        );

                tareaAdapter.actualizarCursor(nuevoCursor);

                // Actualizar los recordatorios tras crear la tarea.
                sincronizarRecordatorios();

                dialog.dismiss();

            } else {

                String error = dbHelper.obtenerUltimoError();

                if (error == null || error.trim().isEmpty()) {
                    error = "SQLite no pudo insertar la tarea.";
                }

                Toast.makeText(
                        requireContext(),
                        error,
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        actualizarListaTareas();
        sincronizarRecordatorios();
    }

    @Override
    public void onDestroyView() {

        if (tareaAdapter != null) {
            Cursor cursor = tareaAdapter.getCursorActual();

            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        tareaAdapter = null;
        rvFormatos = null;
        fabPrincipal = null;

        super.onDestroyView();
    }

    private void actualizarListaTareas() {

        if (dbHelper == null
                || tareaAdapter == null
                || idEstudianteActual == null
                || idEstudianteActual.trim().isEmpty()) {
            return;
        }

        Cursor nuevoCursor =
                dbHelper.obtenerTareas(idEstudianteActual.trim());

        tareaAdapter.actualizarCursor(nuevoCursor);
    }

    private void sincronizarRecordatorios() {

        if (dbHelper == null
                || idEstudianteActual == null
                || idEstudianteActual.trim().isEmpty()) {
            return;
        }

        String id = idEstudianteActual.trim();

        // No programar recordatorios para estudiantes inexistentes.
        if (!dbHelper.existeEstudiante(id)) {
            return;
        }

        // Actualizar los avisos de tareas pendientes.
        Recordatorios.programarRecordatorioTareas(
                requireContext(),
                id
        );

        // Actualizar los avisos de clases usando el horario guardado.
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
                    "Tareas",
                    "Error al sincronizar recordatorios",
                    e
            );
        }
    }
}
