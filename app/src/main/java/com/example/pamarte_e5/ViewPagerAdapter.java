
package com.example.pamarte_e5;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    private final String idEstudiante;

    public ViewPagerAdapter(
            @NonNull FragmentActivity fragmentActivity,
            String idEstudiante) {
        super(fragmentActivity);
        this.idEstudiante = idEstudiante;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new Dash();

            case 1:
                return Tareas.newInstance(idEstudiante);

            case 2:
                return new Usuario();

            default:
                return new Dash();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
