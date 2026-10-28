
package com.example.pamarte_e5;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Usuario extends Fragment {

    private ShapeableImageView imgvFotoPerfil;
    private TextView txtUsernameHeader;
    private TextView txtCorreoHeader;

    private EditText etNombrePerfil;
    private EditText etUsuarioPerfil;
    private EditText etCorreoPerfil;

    private Button btnCambiarFoto;
    private Button btnEditarPerfil;
    private Button btnAdministrarClases;
    private Button btnCerrarSesion;

    private EditText etMateriaHorario;
    private EditText etSalonHorario;
    private Spinner spDiaHorario;

    private Button btnHoraInicio;
    private Button btnHoraFin;
    private Button btnAgregarClase;

    private LinearLayout contenedorClases;

    private DatabaseHelper dbHelper;
    private JSONObject horario;

    private String idEstudiante;
    private String horaInicio = "";
    private String horaFin = "";

    private boolean perfilEnEdicion = false;

    private final String[] dias = {
            "Lunes", "Martes", "Miercoles", "Jueves",
            "Viernes", "Sabado", "Domingo"
    };

    private final String[] diasCortos = {
            "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"
    };

    private final ActivityResultLauncher<String> selectorFoto =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null && getContext() != null) {
                            guardarFotoPerfil(uri);
                        }
                    }
            );

    private final ActivityResultLauncher<String> permisoNotificaciones =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    concedido -> {
                        if (getContext() == null) return;

                        crearCanalNotificaciones(requireContext());

                        if (concedido) {
                            programarRecordatoriosActuales();
                        } else {
                            Toast.makeText(
                                    requireContext(),
                                    "Activa las notificaciones en los ajustes de la aplicación.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View vista = inflater.inflate(
                R.layout.fragment_usuario,
                container,
                false
        );

        imgvFotoPerfil = vista.findViewById(R.id.imgvFotoPerfil);
        txtUsernameHeader = vista.findViewById(R.id.txtUsernameHeader);
        txtCorreoHeader = vista.findViewById(R.id.txtCorreoPerfil);

        etNombrePerfil = vista.findViewById(R.id.etNombrePerfil);
        etUsuarioPerfil = vista.findViewById(R.id.etUsuarioPerfil);
        etCorreoPerfil = vista.findViewById(R.id.etCorreoPerfil);

        btnCambiarFoto = vista.findViewById(R.id.btnCambiarFoto);
        btnEditarPerfil = vista.findViewById(R.id.btnEditarPerfil);

        btnAdministrarClases = vista.findViewById(R.id.btnAdministrarClases);

        btnCerrarSesion = vista.findViewById(R.id.btnCerrarSesion);

        etMateriaHorario = vista.findViewById(R.id.etMateriaHorario);
        etSalonHorario = vista.findViewById(R.id.etSalonHorario);
        spDiaHorario = vista.findViewById(R.id.spDiaHorario);

        btnHoraInicio = vista.findViewById(R.id.btnHoraInicio);
        btnHoraFin = vista.findViewById(R.id.btnHoraFin);
        btnAgregarClase = vista.findViewById(R.id.btnAgregarClase);

        contenedorClases = vista.findViewById(R.id.contenedorClases);

        dbHelper = new DatabaseHelper(requireContext());

        SharedPreferences preferencias =
                requireContext().getSharedPreferences(
                        "SesionApp",
                        Context.MODE_PRIVATE
                );

        idEstudiante = preferencias.getString("ID_ESTUDIANTE", null);

        configurarDias();
        cargarDatosPerfil();
        cargarHorario();
        mostrarClases();

        btnCambiarFoto.setOnClickListener(v -> abrirGaleria());
        btnEditarPerfil.setOnClickListener(v -> cambiarModoEdicion());

        btnHoraInicio.setOnClickListener(v -> seleccionarHora(true));
        btnHoraFin.setOnClickListener(v -> seleccionarHora(false));
        btnAgregarClase.setOnClickListener(v -> agregarClase());

        btnAdministrarClases.setOnClickListener(
                v -> mostrarAdministracionClases()
        );

        btnCerrarSesion.setOnClickListener(
                v -> confirmarCerrarSesion()
        );

        prepararNotificaciones();
        return vista;
    }

    private void cargarDatosPerfil() {
        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No se encontró la sesión del estudiante.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Cursor cursor = dbHelper.obtenerDatosEstudiante(idEstudiante);

        if (cursor == null) return;

        try {
            if (cursor.moveToFirst()) {
                String nombre = obtenerTexto(cursor, "nombre");
                String usuario = obtenerTexto(cursor, "usuario");
                String correo = obtenerTexto(cursor, "correo");
                String foto = obtenerTexto(cursor, "foto_perfil");

                etNombrePerfil.setText(nombre);
                etUsuarioPerfil.setText(usuario);
                etCorreoPerfil.setText(correo);

                String encabezado =
                        nombre.trim().isEmpty() ? usuario : nombre;

                txtUsernameHeader.setText(
                        encabezado.isEmpty() ? "Estudiante" : encabezado
                );

                txtCorreoHeader.setText(correo);

                if (!foto.trim().isEmpty()) {
                    try {
                        imgvFotoPerfil.setImageURI(Uri.parse(foto));
                    } catch (Exception e) {
                        imgvFotoPerfil.setImageResource(
                                R.drawable.ic_launcher_background
                        );
                    }
                }
            }
        } finally {
            cursor.close();
        }
    }

    private String obtenerTexto(Cursor cursor, String columna) {
        int indice = cursor.getColumnIndex(columna);

        if (indice == -1 || cursor.isNull(indice)) {
            return "";
        }

        String valor = cursor.getString(indice);
        return valor == null ? "" : valor;
    }

    private void abrirGaleria() {
        selectorFoto.launch("image/*");
    }

    private void guardarFotoPerfil(Uri uri) {
        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No se encontró la sesión del estudiante.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String nombreArchivo = "foto_perfil_"
                + idEstudiante.replaceAll("[^a-zA-Z0-9_-]", "")
                + ".img";

        File archivo = new File(
                requireContext().getFilesDir(),
                nombreArchivo
        );

        try (
                InputStream entrada =
                        requireContext().getContentResolver()
                                .openInputStream(uri);
                FileOutputStream salida = new FileOutputStream(archivo)
        ) {
            if (entrada == null) {
                throw new Exception("No se pudo leer la imagen.");
            }

            byte[] buffer = new byte[8192];
            int cantidad;

            while ((cantidad = entrada.read(buffer)) != -1) {
                salida.write(buffer, 0, cantidad);
            }

            salida.flush();

            Uri uriGuardada = Uri.fromFile(archivo);

            int resultado = dbHelper.actualizarFotoPerfil(
                    idEstudiante,
                    uriGuardada.toString()
            );

            if (resultado > 0) {
                imgvFotoPerfil.setImageURI(uriGuardada);

                Toast.makeText(
                        requireContext(),
                        "Foto de perfil actualizada.",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Toast.makeText(
                        requireContext(),
                        "No se pudo guardar la foto.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {
            Toast.makeText(
                    requireContext(),
                    "Error al guardar la fotografía: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void cambiarModoEdicion() {
        if (!perfilEnEdicion) {
            perfilEnEdicion = true;

            etNombrePerfil.setEnabled(true);
            etUsuarioPerfil.setEnabled(true);
            etCorreoPerfil.setEnabled(true);

            btnEditarPerfil.setText("Guardar cambios");
        } else {
            guardarDatosPerfil();
        }
    }

    private void guardarDatosPerfil() {
        String nombre = etNombrePerfil.getText().toString().trim();
        String usuario = etUsuarioPerfil.getText().toString().trim();
        String correo = etCorreoPerfil.getText().toString().trim();

        if (nombre.isEmpty() || usuario.isEmpty() || correo.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Completa todos los campos del perfil.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        ContentValues valores = new ContentValues();
        valores.put("nombre", nombre);
        valores.put("usuario", usuario);
        valores.put("correo", correo);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int resultado = db.update(
                "estudiante",
                valores,
                "id_estudiante = ?",
                new String[]{idEstudiante}
        );

        if (resultado > 0) {
            txtUsernameHeader.setText(nombre);
            txtCorreoHeader.setText(correo);

            perfilEnEdicion = false;

            etNombrePerfil.setEnabled(false);
            etUsuarioPerfil.setEnabled(false);
            etCorreoPerfil.setEnabled(false);

            btnEditarPerfil.setText("Editar perfil");

            Toast.makeText(
                    requireContext(),
                    "Perfil actualizado.",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    requireContext(),
                    "No se pudieron guardar los cambios.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // ==================================================
    // HORARIO: CARGAR Y AGREGAR CLASES
    // ==================================================

    private void configurarDias() {
        ArrayAdapter<String> adaptador = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                dias
        );

        adaptador.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spDiaHorario.setAdapter(adaptador);
    }

    private void inicializarHorario() {
        horario = new JSONObject();

        for (String dia : dias) {
            try {
                horario.put(dia, new JSONArray());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void cargarHorario() {
        inicializarHorario();

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            return;
        }

        Cursor cursor = dbHelper.obtenerDatosEstudiante(idEstudiante);

        if (cursor == null) return;

        try {
            if (cursor.moveToFirst()) {
                String guardado = obtenerTexto(cursor, "horario");

                if (!guardado.trim().isEmpty()) {
                    try {
                        horario = new JSONObject(guardado);
                    } catch (JSONException e) {
                        inicializarHorario();

                        Toast.makeText(
                                requireContext(),
                                "El horario guardado no tiene un formato válido.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            }
        } finally {
            cursor.close();
        }

        for (String dia : dias) {
            if (horario.optJSONArray(dia) == null) {
                try {
                    horario.put(dia, new JSONArray());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void seleccionarHora(boolean esInicio) {
        Calendar calendario = Calendar.getInstance();

        int horaActual = calendario.get(Calendar.HOUR_OF_DAY);
        int minutoActual = calendario.get(Calendar.MINUTE);

        new TimePickerDialog(
                requireContext(),
                (vista, hora, minuto) -> {
                    String seleccionada = String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            hora,
                            minuto
                    );

                    if (esInicio) {
                        horaInicio = seleccionada;
                        btnHoraInicio.setText(seleccionada);
                    } else {
                        horaFin = seleccionada;
                        btnHoraFin.setText(seleccionada);
                    }
                },
                horaActual,
                minutoActual,
                true
        ).show();
    }

    private void agregarClase() {
        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "No se encontró la sesión del estudiante.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String materia = etMateriaHorario.getText().toString().trim();
        String salon = etSalonHorario.getText().toString().trim();

        if (spDiaHorario.getSelectedItem() == null) return;

        String dia = spDiaHorario.getSelectedItem().toString();

        if (materia.isEmpty()) {
            etMateriaHorario.setError("Escribe la materia.");
            return;
        }

        if (salon.isEmpty()) {
            etSalonHorario.setError("Escribe el salón.");
            return;
        }

        if (!horaEnRango(horaInicio) || !horaEnRango(horaFin)
                || horaInicio.compareTo(horaFin) >= 0) {
            Toast.makeText(
                    requireContext(),
                    "Selecciona un horario válido.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        JSONArray clases = horario.optJSONArray(dia);

        if (clases == null) {
            clases = new JSONArray();

            try {
                horario.put(dia, clases);
            } catch (JSONException e) {
                return;
            }
        }

        JSONObject clase = new JSONObject();

        try {
            clase.put("materia", materia);
            clase.put("salon", salon);
            clase.put("hora_inicio", horaInicio);
            clase.put("hora_fin", horaFin);

            clases.put(clase);

        } catch (JSONException e) {
            Toast.makeText(
                    requireContext(),
                    "No se pudo agregar la clase.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        guardarHorario(true);
    }

    private void guardarHorario(boolean limpiarCampos) {
        if (idEstudiante == null || horario == null) return;

        int resultado = dbHelper.actualizarHorario(
                idEstudiante,
                horario.toString()
        );

        if (resultado > 0) {
            if (limpiarCampos) {
                etMateriaHorario.setText("");
                etSalonHorario.setText("");

                horaInicio = "";
                horaFin = "";

                btnHoraInicio.setText("Hora de inicio");
                btnHoraFin.setText("Hora de fin");
            }

            mostrarClases();
            programarRecordatoriosActuales();

            Toast.makeText(
                    requireContext(),
                    "Horario guardado correctamente.",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            Toast.makeText(
                    requireContext(),
                    "No se pudo guardar el horario.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // ==================================================
    // TABLA SEMANAL
    // ==================================================

    private void mostrarClases() {
        if (contenedorClases == null) return;

        contenedorClases.removeAllViews();

        if (horario == null) {
            inicializarHorario();
        }

        Set<String> conjuntoIntervalos = new HashSet<>();

        for (String dia : dias) {
            JSONArray clases = horario.optJSONArray(dia);

            if (clases == null) continue;

            for (int i = 0; i < clases.length(); i++) {
                JSONObject clase = clases.optJSONObject(i);

                if (clase == null) continue;

                String inicio = clase.optString("hora_inicio", "");
                String fin = clase.optString("hora_fin", "");

                if (horaEnRango(inicio) && horaEnRango(fin)) {
                    conjuntoIntervalos.add(inicio + "|" + fin);
                }
            }
        }

        ArrayList<String> intervalos =
                new ArrayList<>(conjuntoIntervalos);

        Collections.sort(intervalos, (a, b) -> {
            String[] horaA = a.split("\\|");
            String[] horaB = b.split("\\|");

            int comparacion = horaA[0].compareTo(horaB[0]);

            if (comparacion == 0) {
                comparacion = horaA[1].compareTo(horaB[1]);
            }

            return comparacion;
        });

        if (intervalos.isEmpty()) {
            TextView vacio = new TextView(requireContext());

            vacio.setText("Aún no has agregado clases.");
            vacio.setTextSize(14);
            vacio.setGravity(Gravity.CENTER);
            vacio.setPadding(16, 24, 16, 24);
            vacio.setTextColor(Color.DKGRAY);

            contenedorClases.addView(vacio);
            return;
        }

        TableLayout tabla = new TableLayout(requireContext());
        tabla.setStretchAllColumns(true);

        TableRow encabezado = new TableRow(requireContext());
        encabezado.addView(crearCeldaHorario("Hora", true));

        for (String dia : diasCortos) {
            encabezado.addView(crearCeldaHorario(dia, true));
        }

        tabla.addView(encabezado);

        for (String intervalo : intervalos) {
            String[] horas = intervalo.split("\\|");

            String inicio = horas[0];
            String fin = horas[1];

            TableRow fila = new TableRow(requireContext());

            fila.addView(
                    crearCeldaHorario(inicio + "\n-\n" + fin, true)
            );

            for (String dia : dias) {
                JSONArray clases = horario.optJSONArray(dia);

                StringBuilder texto = new StringBuilder();
                boolean tieneClases = false;

                if (clases != null) {
                    for (int i = 0; i < clases.length(); i++) {
                        JSONObject clase = clases.optJSONObject(i);

                        if (clase == null) continue;

                        if (inicio.equals(
                                clase.optString("hora_inicio", ""))
                                && fin.equals(
                                clase.optString("hora_fin", ""))) {

                            if (texto.length() > 0) {
                                texto.append("\n\n");
                            }

                            texto.append(
                                    clase.optString("materia", "Materia")
                            );

                            String salon = clase.optString("salon", "");

                            if (!salon.trim().isEmpty()) {
                                texto.append("\nSalón: ").append(salon);
                            }

                            tieneClases = true;
                        }
                    }
                }

                TextView celda = crearCeldaHorario(
                        tieneClases ? texto.toString() : "—",
                        false
                );

                if (tieneClases) {
                    final String diaSeleccionado = dia;
                    final String intervaloSeleccionado = intervalo;

                    celda.setOnClickListener(v ->
                            mostrarClasesDeCelda(
                                    diaSeleccionado,
                                    intervaloSeleccionado
                            )
                    );
                }

                fila.addView(celda);
            }

            tabla.addView(fila);
        }

        HorizontalScrollView desplazamiento =
                new HorizontalScrollView(requireContext());

        desplazamiento.setFillViewport(false);
        desplazamiento.setHorizontalScrollBarEnabled(true);
        desplazamiento.addView(tabla);

        desplazamiento.setOnTouchListener((v, evento) -> {
            if (evento.getAction() == MotionEvent.ACTION_DOWN
                    || evento.getAction() == MotionEvent.ACTION_MOVE) {
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }

            if (evento.getAction() == MotionEvent.ACTION_UP
                    || evento.getAction() == MotionEvent.ACTION_CANCEL) {
                if (v.getParent() != null) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
            }

            return false;
        });

        contenedorClases.addView(
                desplazamiento,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );
    }

    private TextView crearCeldaHorario(
            String texto,
            boolean encabezado) {

        TextView celda = new TextView(requireContext());

        celda.setText(texto);
        celda.setTextSize(12);
        celda.setGravity(Gravity.CENTER);
        celda.setPadding(10, 14, 10, 14);
        celda.setMinWidth(85);

        if (encabezado) {
            celda.setTextColor(Color.WHITE);
            celda.setBackgroundColor(Color.rgb(45, 50, 58));
        } else {
            celda.setTextColor(Color.rgb(35, 35, 35));
            celda.setBackgroundColor(Color.rgb(245, 246, 248));
        }

        TableRow.LayoutParams parametros = new TableRow.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        parametros.setMargins(1, 1, 1, 1);
        celda.setLayoutParams(parametros);

        return celda;
    }

    private boolean horaEnRango(String hora) {
        if (hora == null || !hora.matches("\\d{2}:\\d{2}")) {
            return false;
        }

        try {
            String[] partes = hora.split(":");

            int h = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]);

            return h >= 0 && h <= 23 && m >= 0 && m <= 59;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================================================
    // ADMINISTRAR, CONSULTAR, MODIFICAR Y ELIMINAR
    // ==================================================

    private void mostrarAdministracionClases() {
        ArrayList<String> nombres = new ArrayList<>();
        ArrayList<String> diasClases = new ArrayList<>();
        ArrayList<Integer> indicesClases = new ArrayList<>();

        for (String dia : dias) {
            JSONArray clases = horario.optJSONArray(dia);

            if (clases == null) continue;

            for (int i = 0; i < clases.length(); i++) {
                JSONObject clase = clases.optJSONObject(i);

                if (clase == null) continue;

                String materia = clase.optString("materia", "Materia");
                String salon = clase.optString("salon", "Sin salón");
                String inicio = clase.optString("hora_inicio", "");
                String fin = clase.optString("hora_fin", "");

                nombres.add(
                        dia + " | " + inicio + "-" + fin
                                + "\n" + materia + " | Salón: " + salon
                );

                diasClases.add(dia);
                indicesClases.add(i);
            }
        }

        if (nombres.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Administrar clases")
                    .setMessage("Todavía no has agregado materias.")
                    .setPositiveButton("Aceptar", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Selecciona una clase")
                .setItems(
                        nombres.toArray(new String[0]),
                        (dialogo, posicion) ->
                                mostrarDetallesClase(
                                        diasClases.get(posicion),
                                        indicesClases.get(posicion)
                                )
                )
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void mostrarClasesDeCelda(String dia, String intervalo) {
        JSONArray clases = horario.optJSONArray(dia);

        if (clases == null) return;

        String[] horas = intervalo.split("\\|");

        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<String> nombres = new ArrayList<>();

        for (int i = 0; i < clases.length(); i++) {
            JSONObject clase = clases.optJSONObject(i);

            if (clase == null) continue;

            if (horas[0].equals(clase.optString("hora_inicio", ""))
                    && horas[1].equals(clase.optString("hora_fin", ""))) {

                indices.add(i);

                String materia = clase.optString("materia", "Materia");
                String salon = clase.optString("salon", "");

                nombres.add(
                        materia + (salon.isEmpty()
                                ? ""
                                : " — Salón " + salon)
                );
            }
        }

        if (indices.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle(dia + " · " + horas[0] + "–" + horas[1])
                .setItems(
                        nombres.toArray(new String[0]),
                        (dialogo, seleccion) ->
                                mostrarDetallesClase(
                                        dia,
                                        indices.get(seleccion)
                                )
                )
                .setNegativeButton("Cerrar", null)
                .show();
    }

    private void mostrarDetallesClase(String dia, int indice) {
        JSONArray clases = horario.optJSONArray(dia);

        if (clases == null) return;

        JSONObject clase = clases.optJSONObject(indice);

        if (clase == null) return;

        String materia = clase.optString("materia", "");
        String salon = clase.optString("salon", "");
        String inicio = clase.optString("hora_inicio", "");
        String fin = clase.optString("hora_fin", "");

        String detalles =
                "Materia: " + materia
                        + "\nSalón: "
                        + (salon.isEmpty() ? "Sin especificar" : salon)
                        + "\nDía: " + dia
                        + "\nHora de inicio: " + inicio
                        + "\nHora de fin: " + fin;

        new AlertDialog.Builder(requireContext())
                .setTitle("Detalles de la clase")
                .setMessage(detalles)
                .setPositiveButton(
                        "Modificar",
                        (dialogo, opcion) ->
                                mostrarEditorClase(dia, indice)
                )
                .setNegativeButton(
                        "Eliminar",
                        (dialogo, opcion) ->
                                confirmarEliminarClase(dia, indice)
                )
                .setNeutralButton("Cerrar", null)
                .show();
    }

    private void mostrarEditorClase(String dia, int indice) {
        JSONArray clases = horario.optJSONArray(dia);

        if (clases == null) return;

        JSONObject clase = clases.optJSONObject(indice);

        if (clase == null) return;

        LinearLayout contenido = new LinearLayout(requireContext());
        contenido.setOrientation(LinearLayout.VERTICAL);
        contenido.setPadding(32, 8, 32, 0);

        EditText campoMateria = crearCampo(
                "Materia",
                clase.optString("materia", ""),
                InputType.TYPE_CLASS_TEXT
        );

        EditText campoSalon = crearCampo(
                "Salón",
                clase.optString("salon", ""),
                InputType.TYPE_CLASS_TEXT
        );

        EditText campoInicio = crearCampo(
                "Hora de inicio (HH:mm)",
                clase.optString("hora_inicio", ""),
                InputType.TYPE_CLASS_DATETIME
        );

        EditText campoFin = crearCampo(
                "Hora de fin (HH:mm)",
                clase.optString("hora_fin", ""),
                InputType.TYPE_CLASS_DATETIME
        );

        contenido.addView(campoMateria);
        contenido.addView(campoSalon);
        contenido.addView(campoInicio);
        contenido.addView(campoFin);

        AlertDialog dialogo = new AlertDialog.Builder(requireContext())
                .setTitle("Modificar clase")
                .setView(contenido)
                .setPositiveButton("Guardar", null)
                .setNegativeButton("Cancelar", null)
                .create();

        dialogo.setOnShowListener(v ->
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(boton -> {

                            String materia =
                                    campoMateria.getText().toString().trim();

                            String salon =
                                    campoSalon.getText().toString().trim();

                            String inicio =
                                    campoInicio.getText().toString().trim();

                            String fin =
                                    campoFin.getText().toString().trim();

                            if (materia.isEmpty() || salon.isEmpty()) {
                                Toast.makeText(
                                        requireContext(),
                                        "Completa la materia y el salón.",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            if (!horaEnRango(inicio)
                                    || !horaEnRango(fin)
                                    || inicio.compareTo(fin) >= 0) {
                                Toast.makeText(
                                        requireContext(),
                                        "Comprueba las horas en formato HH:mm.",
                                        Toast.LENGTH_SHORT
                                ).show();
                                return;
                            }

                            try {
                                clase.put("materia", materia);
                                clase.put("salon", salon);
                                clase.put("hora_inicio", inicio);
                                clase.put("hora_fin", fin);

                                guardarHorario(false);
                                dialogo.dismiss();

                            } catch (JSONException e) {
                                Toast.makeText(
                                        requireContext(),
                                        "No se pudieron modificar los datos.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        })
        );

        dialogo.show();
    }

    private EditText crearCampo(
            String pista,
            String valor,
            int tipoEntrada) {

        EditText campo = new EditText(requireContext());

        campo.setHint(pista);
        campo.setText(valor);
        campo.setSingleLine(true);
        campo.setInputType(tipoEntrada);

        return campo;
    }

    private void confirmarEliminarClase(String dia, int indice) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar clase")
                .setMessage(
                        "¿Seguro que quieres eliminar esta clase del horario?"
                )
                .setPositiveButton(
                        "Eliminar",
                        (dialogo, opcion) -> eliminarClase(dia, indice)
                )
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarClase(String dia, int indice) {
        JSONArray clases = horario.optJSONArray(dia);

        if (clases == null || indice < 0 || indice >= clases.length()) {
            return;
        }

        clases.remove(indice);
        guardarHorario(false);
    }

    // ==================================================
    // CERRAR SESIÓN
    // ==================================================

    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres salir de tu cuenta?")
                .setPositiveButton(
                        "Cerrar sesión",
                        (dialogo, opcion) -> cerrarSesion()
                )
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        SharedPreferences preferencias =
                requireContext().getSharedPreferences(
                        "SesionApp",
                        Context.MODE_PRIVATE
                );

        preferencias.edit().clear().apply();

        Intent intent = new Intent(
                requireActivity(),
                MainActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }

    // ==================================================
    // NOTIFICACIONES
    // ==================================================

    private void prepararNotificaciones() {
        crearCanalNotificaciones(requireContext());

        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            permisoNotificaciones.launch(
                    Manifest.permission.POST_NOTIFICATIONS
            );

            return;
        }

        programarRecordatoriosActuales();
    }

    private void programarRecordatoriosActuales() {
        if (getContext() == null
                || dbHelper == null
                || idEstudiante == null
                || idEstudiante.trim().isEmpty()) {
            return;
        }

        Recordatorios.programarRecordatoriosClases(
                requireContext(),
                idEstudiante,
                horario == null ? new JSONObject() : horario
        );

        Recordatorios.programarRecordatorioTareas(
                requireContext(),
                idEstudiante
        );
    }

    public static void crearCanalNotificaciones(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    "recordatorios",
                    "Recordatorios de estudio",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            canal.setDescription(
                    "Avisos de clases y tareas pendientes."
            );

            NotificationManager administrador =
                    contexto.getSystemService(NotificationManager.class);

            if (administrador != null) {
                administrador.createNotificationChannel(canal);
            }
        }
    }

    // ==================================================
    // LIMPIEZA
    // ==================================================

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }

        imgvFotoPerfil = null;
        txtUsernameHeader = null;
        txtCorreoHeader = null;

        etNombrePerfil = null;
        etUsuarioPerfil = null;
        etCorreoPerfil = null;

        btnCambiarFoto = null;
        btnEditarPerfil = null;
        btnAdministrarClases = null;
        btnCerrarSesion = null;

        etMateriaHorario = null;
        etSalonHorario = null;
        spDiaHorario = null;

        btnHoraInicio = null;
        btnHoraFin = null;
        btnAgregarClase = null;

        contenedorClases = null;
    }

}
