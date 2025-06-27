package com.example.lab7_20202396.service;

import com.example.lab7_20202396.model.Expense;
import com.example.lab7_20202396.model.Income;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Clase de servicio para gestionar operaciones con Firebase Realtime Database
 */
public class FinanceService {

    private static final String EXPENSES_PATH = "expenses";
    private static final String INCOMES_PATH = "incomes";

    private final DatabaseReference databaseReference;
    private final FirebaseAuth firebaseAuth;

    // Constructor
    public FinanceService() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // Obtener usuario actual
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Verificar si hay usuario autenticado
    public boolean isUserLoggedIn() {
        return getCurrentUser() != null;
    }

    // --- GASTOS ---

    // Guardar un nuevo gasto
    public Task<Void> saveExpense(Expense expense) {
        if (!isUserLoggedIn()) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        // Generar ID único para este gasto
        String expenseId = databaseReference.child(EXPENSES_PATH).push().getKey();
        expense.setId(expenseId);

        // Guardar en la base de datos
        return databaseReference.child(EXPENSES_PATH).child(expenseId).setValue(expense);
    }

    // Actualizar un gasto existente
    public Task<Void> updateExpense(Expense expense) {
        if (!isUserLoggedIn() || expense.getId() == null) {
            throw new IllegalStateException("Usuario no autenticado o ID de gasto no válido");
        }

        return databaseReference.child(EXPENSES_PATH).child(expense.getId()).updateChildren(expense.toMap());
    }

    // Eliminar un gasto
    public Task<Void> deleteExpense(String expenseId) {
        if (!isUserLoggedIn()) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        return databaseReference.child(EXPENSES_PATH).child(expenseId).removeValue();
    }

    // Obtener todos los gastos del usuario actual
    public void getAllExpenses(final DataCallback<List<Expense>> callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Usuario no autenticado"));
            return;
        }

        Query query = databaseReference.child(EXPENSES_PATH).orderByChild("userId").equalTo(user.getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Expense> expenses = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Expense expense = snapshot.getValue(Expense.class);
                    if (expense != null) {
                        expense.setId(snapshot.getKey());
                        expenses.add(expense);
                    }
                }

                callback.onSuccess(expenses);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    // Obtener gastos del usuario actual por mes
    public void getExpensesByMonth(int year, int month, final DataCallback<List<Expense>> callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Usuario no autenticado"));
            return;
        }

        // Calcular timestamp para el primer y último día del mes
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1, 0, 0, 0);
        final long startDate = calendar.getTimeInMillis();

        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        final long endDate = calendar.getTimeInMillis();

        Query query = databaseReference.child(EXPENSES_PATH)
                .orderByChild("userId")
                .equalTo(user.getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Expense> expenses = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Expense expense = snapshot.getValue(Expense.class);
                    if (expense != null &&
                        expense.getDate().getTime() >= startDate &&
                        expense.getDate().getTime() <= endDate) {
                        expense.setId(snapshot.getKey());
                        expenses.add(expense);
                    }
                }

                callback.onSuccess(expenses);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    // --- INGRESOS ---

    // Guardar un nuevo ingreso
    public Task<Void> saveIncome(Income income) {
        if (!isUserLoggedIn()) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        // Generar ID único para este ingreso
        String incomeId = databaseReference.child(INCOMES_PATH).push().getKey();
        income.setId(incomeId);

        // Guardar en la base de datos
        return databaseReference.child(INCOMES_PATH).child(incomeId).setValue(income);
    }

    // Actualizar un ingreso existente
    public Task<Void> updateIncome(Income income) {
        if (!isUserLoggedIn() || income.getId() == null) {
            throw new IllegalStateException("Usuario no autenticado o ID de ingreso no válido");
        }

        return databaseReference.child(INCOMES_PATH).child(income.getId()).updateChildren(income.toMap());
    }

    // Eliminar un ingreso
    public Task<Void> deleteIncome(String incomeId) {
        if (!isUserLoggedIn()) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        return databaseReference.child(INCOMES_PATH).child(incomeId).removeValue();
    }

    // Obtener todos los ingresos del usuario actual
    public void getAllIncomes(final DataCallback<List<Income>> callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Usuario no autenticado"));
            return;
        }

        Query query = databaseReference.child(INCOMES_PATH).orderByChild("userId").equalTo(user.getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Income> incomes = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Income income = snapshot.getValue(Income.class);
                    if (income != null) {
                        income.setId(snapshot.getKey());
                        incomes.add(income);
                    }
                }

                callback.onSuccess(incomes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    // Obtener ingresos del usuario actual por mes
    public void getIncomesByMonth(int year, int month, final DataCallback<List<Income>> callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(new IllegalStateException("Usuario no autenticado"));
            return;
        }

        // Calcular timestamp para el primer y último día del mes
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, 1, 0, 0, 0);
        final long startDate = calendar.getTimeInMillis();

        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        final long endDate = calendar.getTimeInMillis();

        Query query = databaseReference.child(INCOMES_PATH)
                .orderByChild("userId")
                .equalTo(user.getUid());

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Income> incomes = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Income income = snapshot.getValue(Income.class);
                    if (income != null &&
                        income.getDate().getTime() >= startDate &&
                        income.getDate().getTime() <= endDate) {
                        income.setId(snapshot.getKey());
                        incomes.add(income);
                    }
                }

                callback.onSuccess(incomes);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                callback.onError(databaseError.toException());
            }
        });
    }

    // --- RESUMEN FINANCIERO ---

    // Obtener resumen financiero del mes
    public void getFinancialSummary(int year, int month, final DataCallback<FinancialSummary> callback) {
        final FinancialSummary summary = new FinancialSummary();

        // Obtener ingresos del mes
        getIncomesByMonth(year, month, new DataCallback<List<Income>>() {
            @Override
            public void onSuccess(List<Income> incomes) {
                double totalIncome = 0;
                for (Income income : incomes) {
                    totalIncome += income.getAmount();
                }
                summary.setTotalIncome(totalIncome);

                // Luego obtener gastos del mes
                getExpensesByMonth(year, month, new DataCallback<List<Expense>>() {
                    @Override
                    public void onSuccess(List<Expense> expenses) {
                        double totalExpense = 0;
                        for (Expense expense : expenses) {
                            totalExpense += expense.getAmount();
                        }
                        summary.setTotalExpense(totalExpense);
                        summary.setBalance(summary.getTotalIncome() - summary.getTotalExpense());

                        callback.onSuccess(summary);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    // --- CLASES AUXILIARES ---

    // Clase para manejar respuesta asíncrona de Firebase
    public interface DataCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // Clase para el resumen financiero
    public static class FinancialSummary {
        private double totalIncome;
        private double totalExpense;
        private double balance;

        public FinancialSummary() {
            this.totalIncome = 0;
            this.totalExpense = 0;
            this.balance = 0;
        }

        public double getTotalIncome() {
            return totalIncome;
        }

        public void setTotalIncome(double totalIncome) {
            this.totalIncome = totalIncome;
        }

        public double getTotalExpense() {
            return totalExpense;
        }

        public void setTotalExpense(double totalExpense) {
            this.totalExpense = totalExpense;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }
    }
}
