package com.example.lab7_20202396.fragments;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lab7_20202396.R;
import com.example.lab7_20202396.databinding.FragmentSummaryBinding;
import com.example.lab7_20202396.model.Expense;
import com.example.lab7_20202396.model.Income;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class SummaryFragment extends Fragment {

    private FragmentSummaryBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Calendar selectedMonth;
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        selectedMonth = Calendar.getInstance();

        // Configurar botón de selección de mes
        binding.buttonSelectMonth.setText(monthYearFormat.format(selectedMonth.getTime()));
        binding.buttonSelectMonth.setOnClickListener(v -> showMonthPicker());

        // Inicializar y configurar gráficos
        setupPieChart();
        setupBarChart();

        // Cargar datos para el mes actual
        loadMonthData();
    }

    private void showMonthPicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedMonth.set(Calendar.YEAR, year);
                    selectedMonth.set(Calendar.MONTH, month);
                    binding.buttonSelectMonth.setText(monthYearFormat.format(selectedMonth.getTime()));

                    // Recargar datos con el nuevo mes seleccionado
                    loadMonthData();
                },
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH),
                1
        );

        // Mostramos el diálogo con el DatePicker como selector de mes
        datePickerDialog.show();
    }

    private void setupPieChart() {
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.WHITE);
        binding.pieChart.setTransparentCircleRadius(61f);
        binding.pieChart.setHoleRadius(58f);
        binding.pieChart.setDrawCenterText(true);
        binding.pieChart.setCenterText("Distribución");
        binding.pieChart.setCenterTextSize(16f);

        Legend legend = binding.pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(10f);
    }

    private void setupBarChart() {
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.setPinchZoom(false);
        binding.barChart.setDrawBarShadow(false);
        binding.barChart.setDrawValueAboveBar(true);

        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                switch ((int) value) {
                    case 0: return "Ingresos";
                    case 1: return "Egresos";
                    case 2: return "Balance";
                    default: return "";
                }
            }
        });

        YAxis leftAxis = binding.barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setSpaceTop(35f);
        leftAxis.setAxisMinimum(0f);

        binding.barChart.getAxisRight().setEnabled(false);
        binding.barChart.getLegend().setEnabled(false);
    }

    private void loadMonthData() {
        if (auth.getCurrentUser() == null) {
            return;
        }

        showLoading(true);

        String userId = auth.getCurrentUser().getUid();

        // Calcular el inicio y fin del mes seleccionado
        Calendar startOfMonth = (Calendar) selectedMonth.clone();
        startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        startOfMonth.set(Calendar.MINUTE, 0);
        startOfMonth.set(Calendar.SECOND, 0);

        Calendar endOfMonth = (Calendar) selectedMonth.clone();
        endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH));
        endOfMonth.set(Calendar.HOUR_OF_DAY, 23);
        endOfMonth.set(Calendar.MINUTE, 59);
        endOfMonth.set(Calendar.SECOND, 59);

        Date startDate = startOfMonth.getTime();
        Date endDate = endOfMonth.getTime();

        final double[] totalIncome = {0};
        final double[] totalExpense = {0};
        final AtomicInteger pendingTasks = new AtomicInteger(2);

        // Obtener ingresos del mes
        db.collection("incomes")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(incomeDocuments -> {
                    for (Income income : incomeDocuments.toObjects(Income.class)) {
                        totalIncome[0] += income.getAmount();
                    }

                    if (pendingTasks.decrementAndGet() == 0) {
                        updateUIWithData(totalIncome[0], totalExpense[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Obtener egresos del mes
        db.collection("expenses")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener(expenseDocuments -> {
                    for (Expense expense : expenseDocuments.toObjects(Expense.class)) {
                        totalExpense[0] += expense.getAmount();
                    }

                    if (pendingTasks.decrementAndGet() == 0) {
                        updateUIWithData(totalIncome[0], totalExpense[0]);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIWithData(double totalIncome, double totalExpense) {
        double balance = totalIncome - totalExpense;

        // Actualizar texto de montos totales
        binding.textViewTotalIncome.setText(String.format(Locale.getDefault(), "S/ %.2f", totalIncome));
        binding.textViewTotalExpense.setText(String.format(Locale.getDefault(), "S/ %.2f", totalExpense));
        binding.textViewBalance.setText(String.format(Locale.getDefault(), "S/ %.2f", balance));

        // Establecer color para el balance (verde si es positivo, rojo si es negativo)
        binding.textViewBalance.setTextColor(balance >= 0 ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark));

        // Actualizar gráfico circular (PieChart)
        updatePieChart(totalIncome, totalExpense);

        // Actualizar gráfico de barras (BarChart)
        updateBarChart(totalIncome, totalExpense, balance);

        showLoading(false);
    }

    private void updatePieChart(double totalIncome, double totalExpense) {
        List<PieEntry> entries = new ArrayList<>();

        // Si no hay datos, mostrar un mensaje
        if (totalIncome == 0 && totalExpense == 0) {
            binding.pieChart.setData(null);
            binding.pieChart.invalidate();
            return;
        }

        // Añadir entradas si hay datos
        if (totalIncome > 0) {
            entries.add(new PieEntry((float) totalIncome, "Ingresos"));
        }

        if (totalExpense > 0) {
            entries.add(new PieEntry((float) totalExpense, "Egresos"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));

        binding.pieChart.setData(data);
        binding.pieChart.invalidate();
    }

    private void updateBarChart(double totalIncome, double totalExpense, double balance) {
        List<BarEntry> entries = new ArrayList<>();

        // Agregar entradas para ingresos, egresos y balance
        entries.add(new BarEntry(0, (float) totalIncome));
        entries.add(new BarEntry(1, (float) totalExpense));
        entries.add(new BarEntry(2, (float) Math.abs(balance)));

        BarDataSet dataSet = new BarDataSet(entries, "Datos financieros");

        // Configurar colores individuales para cada barra
        int[] colors = new int[] {
                getResources().getColor(android.R.color.holo_green_light),
                getResources().getColor(android.R.color.holo_red_light),
                balance >= 0 ?
                        getResources().getColor(android.R.color.holo_green_dark) :
                        getResources().getColor(android.R.color.holo_red_dark)
        };

        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(14f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.7f);

        binding.barChart.setData(data);
        binding.barChart.invalidate();
    }

    private void showLoading(boolean isLoading) {
        binding.progressBarSummary.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
