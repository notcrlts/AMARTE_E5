
package com.example.pamarte_e5;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText txteEmail, txtpPassword;
    private CheckBox chkMantenerSesion;
    private Button btnIniciarSesion, btnIrRegistro;
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom);

                    return insets;
                });

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("SesionApp", MODE_PRIVATE);

        boolean mantenerSesion = prefs.getBoolean(
                "MANTENER_SESION", false);

        String idGuardado = prefs.getString(
                "ID_ESTUDIANTE", "");

        if (mantenerSesion) {
            if (idGuardado != null
                    && !idGuardado.trim().isEmpty()
                    && dbHelper.existeEstudiante(idGuardado.trim())) {

                irAlDashboard(idGuardado.trim());
                return;

            } else {
                // La sesión guardada ya no corresponde
                // a un estudiante existente.
                prefs.edit()
                        .remove("ID_ESTUDIANTE")
                        .putBoolean("MANTENER_SESION", false)
                        .apply();

                Toast.makeText(
                        this,
                        "Tu sesión anterior ya no es válida. Inicia sesión nuevamente.",
                        Toast.LENGTH_LONG).show();
            }
        }

        txteEmail = findViewById(R.id.txteEmail);
        txtpPassword = findViewById(R.id.txtpPassword);
        chkMantenerSesion = findViewById(R.id.chkMantenerSesion);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnIrRegistro = findViewById(R.id.btnIrRegistro);

        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());
        btnIrRegistro.setOnClickListener(v -> registrar());
    }

    private void iniciarSesion() {
        String email = txteEmail.getText().toString().trim();
        String password = txtpPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "Por favor completa todos los campos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String idEstudiante = dbHelper.verificarUsuario(
                email, password);

        if (idEstudiante != null
                && !idEstudiante.trim().isEmpty()
                && dbHelper.existeEstudiante(idEstudiante.trim())) {

            idEstudiante = idEstudiante.trim();

            boolean recordar = chkMantenerSesion.isChecked();

            prefs.edit()
                    .putString("ID_ESTUDIANTE", idEstudiante)
                    .putBoolean("MANTENER_SESION", recordar)
                    .apply();

            Toast.makeText(
                    this,
                    "¡Inicio de sesión exitoso!",
                    Toast.LENGTH_SHORT).show();

            irAlDashboard(idEstudiante);

        } else {
            Toast.makeText(
                    this,
                    "Correo o contraseña incorrectos, o cuenta inexistente.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void irAlDashboard(String idEstudiante) {
        Intent intent = new Intent(this, Dashboard.class);
        intent.putExtra("ID_ESTUDIANTE", idEstudiante);
        startActivity(intent);
        finish();
    }

    private void registrar() {
        Intent intent = new Intent(this, Registro.class);
        startActivity(intent);
    }
}
