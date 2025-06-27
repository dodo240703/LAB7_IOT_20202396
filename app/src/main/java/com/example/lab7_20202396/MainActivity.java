package com.example.lab7_20202396;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.lab7_20202396.databinding.ActivityMainBinding;
import com.example.lab7_20202396.fragments.ExpenseFragment;
import com.example.lab7_20202396.fragments.IncomeFragment;
import com.example.lab7_20202396.fragments.SummaryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        // Verificar si el usuario está autenticado
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Configurar ViewPager2
        binding.viewPager.setAdapter(new ViewPagerAdapter(this));
        binding.viewPager.setUserInputEnabled(true);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        binding.bottomNavigationView.setSelectedItemId(R.id.navigation_income);
                        break;
                    case 1:
                        binding.bottomNavigationView.setSelectedItemId(R.id.navigation_expense);
                        break;
                    case 2:
                        binding.bottomNavigationView.setSelectedItemId(R.id.navigation_summary);
                        break;
                }
            }
        });

        // Configurar BottomNavigationView
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_income) {
                binding.viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.navigation_expense) {
                binding.viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.navigation_summary) {
                binding.viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.navigation_logout) {
                logout();
                return true;
            }
            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void logout() {
        firebaseAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    // Adaptador para ViewPager2
    private static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new IncomeFragment();
                case 1:
                    return new ExpenseFragment();
                case 2:
                    return new SummaryFragment();
                default:
                    return new IncomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3; // Número de páginas (Income, Expense, Summary)
        }
    }
}