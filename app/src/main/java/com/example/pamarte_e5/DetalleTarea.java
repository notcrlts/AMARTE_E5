
package com.example.pamarte_e5;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class DetalleTarea extends AppCompatActivity {

    private TextView txtNomActDetalle;
    private TextView txtDescDetalle;
    private TextView txtPrioridadDetalle;
    private TextView txtFechaDetalle;

    private EditText etNomActDetalle;
    private EditText etDescActDetalle;

    private Spinner spinnerPrioridad;

    private Button btnEditarTarea;
    private Button btnGuardarCambios;

    private LinearLayout layoutPasosContainer;

    private CheckBox chkTareaCompletada;

    private DatabaseHelper dbHelper;
    private String idAct;

    private boolean esModoEdicion = false;
    private boolean actualizandoEstado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_tarea);

        View vistaPrincipal = findViewById(R.id.main);

        if (vistaPrincipal != null) {
            ViewCompat.setOnApplyWindowInsetsListener(
                    vistaPrincipal,
                    (v, insets) -> {
                        Insets systemBars = insets.getInsets(
                                WindowInsetsCompat.Type.systemBars()
                        );

                        v.setPadding(
                                systemBars.left,
                                systemBars.top,
                                systemBars.right,
                                systemBars.bottom
                        );

                        return insets;
                    }
            );
        }

        dbHelper = new DatabaseHelper(this);
        idAct = getIntent().getStringExtra("id_act");

        // Vincular vistas de lectura.
        txtNomActDetalle = findViewById(R.id.txtNomActDetalle);
        txtDescDetalle = findViewById(R.id.txtDescDetalle);
        txtPrioridadDetalle = findViewById(R.id.txtPrioridadDetalle);
        txtFechaDetalle = findViewById(R.id.txtFechaDetalle);

        chkTareaCompletada = findViewById(R.id.chkTareaCompletada);

        // Vincular vistas de edición.
        etNomActDetalle = findViewById(R.id.etNomActDetalle);
        etDescActDetalle = findViewById(R.id.etDescDetalle);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);

        // Botones y contenedor de pasos.
        btnEditarTarea = findViewById(R.id.btnEditarTarea);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        layoutPasosContainer = findViewById(R.id.layoutPasosContainer);

        // Configurar prioridades.
        String[] prioridades = {"ALTA", "MEDIA", "BAJA"};

        ArrayAdapter<String> adapterPrioridad = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                prioridades
        );

        adapterPrioridad.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerPrioridad.setAdapter(adapterPrioridad);

        if (idAct == null || idAct.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No se recibió el ID de la tarea",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Cargar primero el estado guardado y los datos.
        boolean tareaCargada = cargarDatosTarea();

        if (!tareaCargada) {
            Toast.makeText(
                    this,
                    "No se encontró la tarea en la base de datos",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        // Guardar el estado general de la tarea.
        chkTareaCompletada.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (actualizandoEstado) {
                        return;
                    }

                    int estado = isChecked ? 1 : 0;

                    boolean actualizado = dbHelper.actualizarEstadoTarea(
                            idAct,
                            estado
                    ) > 0;

                    if (actualizado) {
                        setResult(RESULT_OK);

                        Toast.makeText(
                                this,
                                isChecked
                                        ? "Tarea completada"
                                        : "Tarea pendiente",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {
                        actualizandoEstado = true;
                        chkTareaCompletada.setChecked(!isChecked);
                        actualizandoEstado = false;

                        Toast.makeText(
                                this,
                                "No se pudo guardar el estado de la tarea",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );

        // Botón Editar / Cancelar.
        btnEditarTarea.setOnClickListener(v -> {

            esModoEdicion = !esModoEdicion;
            setModoEdicion(esModoEdicion);

        });

        // Guardar cambios de nombre, descripción y prioridad.
        btnGuardarCambios.setOnClickListener(v -> {

            String nuevoNombre = etNomActDetalle
                    .getText().toString().trim();

            String nuevaDescripcion = etDescActDetalle
                    .getText().toString().trim();

            Object prioridadSeleccionada =
                    spinnerPrioridad.getSelectedItem();

            if (prioridadSeleccionada == null) {
                Toast.makeText(
                        this,
                        "Selecciona una prioridad",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String nuevaPrioridad =
                    prioridadSeleccionada.toString();

            if (nuevoNombre.isEmpty()) {
                Toast.makeText(
                        this,
                        "El nombre no puede estar vacío",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            boolean actualizado = dbHelper.actualizarDatosTarea(
                    idAct,
                    nuevoNombre,
                    nuevaDescripcion,
                    nuevaPrioridad
            );

            if (actualizado) {

                txtNomActDetalle.setText(nuevoNombre);

                txtDescDetalle.setText(
                        nuevaDescripcion.isEmpty()
                                ? "Sin descripción"
                                : nuevaDescripcion
                );

                txtPrioridadDetalle.setText(
                        "Prioridad: " + nuevaPrioridad
                );

                esModoEdicion = false;
                setModoEdicion(false);

                setResult(RESULT_OK);

                Toast.makeText(
                        this,
                        "Cambios guardados correctamente",
                        Toast.LENGTH_SHORT
                ).show();

            } else {
                Toast.makeText(
                        this,
                        "No se pudieron guardar los cambios",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private boolean cargarDatosTarea() {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT * FROM actividades WHERE id_act = ?",
                new String[]{idAct}
        )) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            String nombre = cursor.getString(
                    cursor.getColumnIndexOrThrow("nom_act")
            );

            String descripcion = cursor.getString(
                    cursor.getColumnIndexOrThrow("descrip_act")
            );

            String prioridad = cursor.getString(
                    cursor.getColumnIndexOrThrow("prioridad")
            );

            String fechaEntrega = cursor.getString(
                    cursor.getColumnIndexOrThrow("f_ent")
            );

            String pasosJson = cursor.getString(
                    cursor.getColumnIndexOrThrow("pasos")
            );

            int estadoCompletado = cursor.getInt(
                    cursor.getColumnIndexOrThrow("completado")
            );

            txtNomActDetalle.setText(nombre);

            txtDescDetalle.setText(
                    descripcion == null || descripcion.trim().isEmpty()
                            ? "Sin descripción"
                            : descripcion
            );

            txtPrioridadDetalle.setText(
                    "Prioridad: " + prioridad
            );

            txtFechaDetalle.setText(fechaEntrega);

            // Restaurar el estado guardado sin activar el listener.
            actualizandoEstado = true;
            chkTareaCompletada.setChecked(estadoCompletado == 1);
            actualizandoEstado = false;

            // Seleccionar la prioridad actual.
            for (int i = 0; i < spinnerPrioridad.getCount(); i++) {

                if (spinnerPrioridad
                        .getItemAtPosition(i)
                        .toString()
                        .equalsIgnoreCase(prioridad)) {

                    spinnerPrioridad.setSelection(i);
                    break;
                }
            }

            renderizarPasos(pasosJson);

            return true;

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error al cargar la tarea: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            return false;
        }
    }

    private void renderizarPasos(String pasosJson) {

        layoutPasosContainer.removeAllViews();

        if (pasosJson == null || pasosJson.trim().isEmpty()) {

            TextView tv = new TextView(this);
            tv.setText("No hay pasos registrados para esta actividad.");
            tv.setTextColor(0xFF495057);

            layoutPasosContainer.addView(tv);
            return;
        }

        try {

            JSONArray jsonArray = new JSONArray(pasosJson);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject pasoObj = jsonArray.getJSONObject(i);

                String textoPaso = pasoObj.optString(
                        "texto",
                        "Paso " + (i + 1)
                );

                boolean completado = pasoObj.optBoolean(
                        "completado",
                        false
                );

                CheckBox checkBox = new CheckBox(this);

                checkBox.setText(textoPaso);
                checkBox.setTextColor(0xFF212529);
                checkBox.setButtonTintList(
                        getColorStateList(android.R.color.black)
                );

                // Establecer el estado antes de agregar el listener.
                checkBox.setChecked(completado);

                final int indice = i;

                checkBox.setOnCheckedChangeListener(
                        (buttonView, isChecked) ->
                                actualizarEstadoPasoJson(
                                        indice,
                                        isChecked
                                )
                );

                layoutPasosContainer.addView(checkBox);
            }

        } catch (Exception e) {

            TextView tv = new TextView(this);
            tv.setText("No se pudieron cargar los pasos de la tarea.");
            tv.setTextColor(0xFF212529);

            layoutPasosContainer.addView(tv);
        }
    }

    private void actualizarEstadoPasoJson(
            int indice,
            boolean completado) {

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT pasos FROM actividades WHERE id_act = ?",
                new String[]{idAct}
        )) {

            if (!cursor.moveToFirst()) {
                return;
            }

            String pasosJson = cursor.getString(
                    cursor.getColumnIndexOrThrow("pasos")
            );

            if (pasosJson == null || pasosJson.trim().isEmpty()) {
                return;
            }

            JSONArray jsonArray = new JSONArray(pasosJson);

            if (indice < 0 || indice >= jsonArray.length()) {
                return;
            }

            JSONObject pasoObj = jsonArray.getJSONObject(indice);

            pasoObj.put("completado", completado);

            boolean actualizado = dbHelper.actualizarPasosTarea(
                    idAct,
                    jsonArray.toString()
            ) > 0;

            if (actualizado) {
                setResult(RESULT_OK);
            } else {
                Toast.makeText(
                        this,
                        "No se pudo guardar el estado del paso",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Error al guardar el paso",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setModoEdicion(boolean activo) {

        if (activo) {

            etNomActDetalle.setText(
                    txtNomActDetalle.getText().toString()
            );

            String descripcion =
                    txtDescDetalle.getText().toString();

            etDescActDetalle.setText(
                    descripcion.equals("Sin descripción")
                            ? ""
                            : descripcion
            );

            txtNomActDetalle.setVisibility(View.GONE);
            txtDescDetalle.setVisibility(View.GONE);
            txtPrioridadDetalle.setVisibility(View.GONE);

            etNomActDetalle.setVisibility(View.VISIBLE);
            etDescActDetalle.setVisibility(View.VISIBLE);
            spinnerPrioridad.setVisibility(View.VISIBLE);

            btnGuardarCambios.setVisibility(View.VISIBLE);
            btnEditarTarea.setText("Cancelar");

        } else {

            txtNomActDetalle.setVisibility(View.VISIBLE);
            txtDescDetalle.setVisibility(View.VISIBLE);
            txtPrioridadDetalle.setVisibility(View.VISIBLE);

            etNomActDetalle.setVisibility(View.GONE);
            etDescActDetalle.setVisibility(View.GONE);
            spinnerPrioridad.setVisibility(View.GONE);

            btnGuardarCambios.setVisibility(View.GONE);
            btnEditarTarea.setText("Editar");
        }
    }
}
