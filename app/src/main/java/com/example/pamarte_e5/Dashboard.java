
package com.example.pamarte_e5;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class Dashboard extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bnvIconos;
    private String idEstudiante;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        SharedPreferences prefs = getSharedPreferences(
                "SesionApp", Context.MODE_PRIVATE);

        idEstudiante = prefs.getString("ID_ESTUDIANTE", "");

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            idEstudiante = getIntent().getStringExtra("ID_ESTUDIANTE");
        }

        if (idEstudiante == null || idEstudiante.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "No se encontró el ID del estudiante. Inicia sesión nuevamente.",
                    Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        idEstudiante = idEstudiante.trim();

        viewPager = findViewById(R.id.viewPager);
        bnvIconos = findViewById(R.id.bnvIconos);

        ViewPagerAdapter adapter =
                new ViewPagerAdapter(this, idEstudiante);

        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        bnvIconos.getMenu()
                                .getItem(position)
                                .setChecked(true);
                    }
                });

        bnvIconos.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0);
                return true;

            } else if (itemId == R.id.nav_add_task) {
                viewPager.setCurrentItem(1);
                return true;

            } else if (itemId == R.id.nav_user) {
                viewPager.setCurrentItem(2);
                return true;
            }

            return false;
        });
    }
}
