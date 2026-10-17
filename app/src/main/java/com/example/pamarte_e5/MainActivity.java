package com.example.pamarte_e5;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText txteEmail, txtpPassword;
    private Button btnIniciarSesion;
    private String email, password;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        txteEmail = findViewById(R.id.txteEmail);
        txtpPassword = findViewById(R.id.txtpPassword);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);


    }

    private void iniciarSesion() {
        email = txteEmail.getText().toString();
        password = txtpPassword.getText().toString();

    }
}