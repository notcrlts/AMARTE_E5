package com.example.pamarte_e5;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Registro extends AppCompatActivity {

    private EditText txteUsuario, txteNombre, txteCorreo, txtpPassword;
    private Button btnRegistrar;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        dbHelper = new DatabaseHelper(this);

        txteUsuario = findViewById(R.id.txteUsuario);
        txteNombre = findViewById(R.id.txteNombre);
        txteCorreo = findViewById(R.id.txteCorreo);
        txtpPassword = findViewById(R.id.txtpPassword);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        btnRegistrar.setOnClickListener(v -> {
            String usuario = txteUsuario.getText().toString().trim();
            String nombre = txteNombre.getText().toString().trim();
            String correo = txteCorreo.getText().toString().trim();
            String passw = txtpPassword.getText().toString().trim();

            // Incluimos nombre.isEmpty() para asegurarnos de tener el nombre completo
            if (usuario.isEmpty() || nombre.isEmpty() || correo.isEmpty() || passw.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean exito = dbHelper.insertarEstudiante(usuario, nombre, correo, passw, "Matutino");

            if (exito) {
                Toast.makeText(this, "¡Cuenta creada con éxito!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al registrar la cuenta", Toast.LENGTH_SHORT).show();
            }
        });
    }
}