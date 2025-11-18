package com.example.finance_code.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.finance_code.ui.home.HomeActivity;
import com.example.finance_code.ui.transaction.addTransaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;

public class AuthCheckActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private FirebaseAuth mAuth;

    private static final String EXTRA_WIDGET_TRANSACTION_TYPE = "com.example.finance_code.WIDGET_TRANSACTION_TYPE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            Intent currentIntent = getIntent();
                            if (currentIntent != null && currentIntent.hasExtra(EXTRA_WIDGET_TRANSACTION_TYPE)) {
                                int type = currentIntent.getIntExtra(EXTRA_WIDGET_TRANSACTION_TYPE, -1);
                                goToAddTransaction(type);
                            } else {
                                goToHome();
                            }
                        } else {
                            goToLogin();
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Si el usuario cancela o hay error, cerramos la app
                        Toast.makeText(getApplicationContext(),
                                "Autenticación cancelada", Toast.LENGTH_SHORT).show();
                        finishAffinity();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Requerida")
                .setSubtitle("Confirma tu identidad para continuar")
                .setDescription("Usa tu huella digital, patrón o PIN para desbloquear la app.")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkBiometricsAndAuthenticate();
    }

    private void checkBiometricsAndAuthenticate() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int canAuth = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                Intent currentIntent = getIntent();
                if (currentIntent != null && currentIntent.hasExtra(EXTRA_WIDGET_TRANSACTION_TYPE)) {
                    int type = currentIntent.getIntExtra(EXTRA_WIDGET_TRANSACTION_TYPE, -1);
                    goToAddTransaction(type);
                } else {
                    goToHome();
                }
            } else {
                goToLogin();
            }
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(AuthCheckActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToHome() {
        Intent intent = new Intent(AuthCheckActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToAddTransaction(int type) {
        Intent intent = new Intent(AuthCheckActivity.this, addTransaction.class);
        intent.putExtra(EXTRA_WIDGET_TRANSACTION_TYPE, type);
        startActivity(intent);
        finish();
    }
}