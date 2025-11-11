package com.example.finance_code.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class AuthCheckActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt("theme_key", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);

        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        goToLogin();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
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
                .setDeviceCredentialAllowed(true)
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
            Toast.makeText(this, "Tu dispositivo no tiene seguridad configurada.", Toast.LENGTH_LONG).show();
            finishAffinity();
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(AuthCheckActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}