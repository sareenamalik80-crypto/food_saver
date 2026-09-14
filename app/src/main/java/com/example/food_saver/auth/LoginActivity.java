package com.example.food_saver.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.R;
import com.example.food_saver.databinding.ActivityLoginBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.donor.DonorHomeActivity;
import com.example.food_saver.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthRepository authRepository;
    private GoogleSignInClient googleSignInClient;
    private boolean passwordVisible = false;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    setLoading(true);
                    authRepository.continueWithGoogleCredential(credential, new AuthRepository.GoogleAuthCallback() {
                        @Override
                        public void onExistingUser(User user) {
                            setLoading(false);
                            routeUser(user);
                        }

                        @Override
                        public void onNewUser(String uid, String displayName, String email) {
                            setLoading(false);
                            Intent intent = new Intent(LoginActivity.this, CompleteGoogleProfileActivity.class);
                            intent.putExtra(CompleteGoogleProfileActivity.EXTRA_UID, uid);
                            intent.putExtra(CompleteGoogleProfileActivity.EXTRA_NAME, displayName);
                            intent.putExtra(CompleteGoogleProfileActivity.EXTRA_EMAIL, email);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in cancelled or failed.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.root);

        authRepository = new AuthRepository();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener(v -> attemptLogin());
        binding.btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        binding.btnGoogleSignIn.setOnClickListener(v ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));

        binding.tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        });
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selection = binding.etPassword.getSelectionEnd();
        binding.etPassword.setInputType(passwordVisible
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        binding.etPassword.setSelection(selection);
    }

    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setHint("Email");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setText(binding.etEmail.getText().toString().trim());
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, 0);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your account email — we'll send a link to reset your password.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Enter your email first.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    authRepository.sendPasswordReset(email, new AuthRepository.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(LoginActivity.this,
                                    "Password reset link sent — check your email.", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Auto-route if a session is already active (skip login screen).
        if (authRepository.getCurrentUser() != null) {
            setLoading(true);
            authRepository.fetchUserProfile(authRepository.getCurrentUser().getUid(),
                    new AuthRepository.AuthCallback() {
                        @Override
                        public void onSuccess(User user) {
                            setLoading(false);
                            routeUser(user);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            setLoading(false);
                            // Profile missing/corrupt — force a clean re-login.
                            authRepository.logout();
                        }
                    });
        }
    }

    private void attemptLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                routeUser(user);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Central routing point: role decides which dashboard, status decides
     * whether a donor/NGO account is even allowed in yet.
     * NOTE: Admin and NGO dashboards are placeholders until those modules
     * are built — replace the commented lines as they land.
     */
    void routeUser(User user) {
        if (User.ROLE_ADMIN.equals(user.getRole())) {
            // startActivity(new Intent(this, AdminDashboardActivity.class));
            Toast.makeText(this, "Route to Admin Dashboard", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (User.STATUS_PENDING.equals(user.getStatus())) {
            startActivity(new Intent(this, PendingVerificationActivity.class));
            finish();
            return;
        }

        if (User.STATUS_REJECTED.equals(user.getStatus())) {
            Toast.makeText(this, "Your account was not approved. Contact support.", Toast.LENGTH_LONG).show();
            authRepository.logout();
            return;
        }

        // status == verified
        if (User.ROLE_DONOR.equals(user.getRole())) {
            startActivity(new Intent(this, DonorHomeActivity.class));
        } else if (User.ROLE_NGO.equals(user.getRole())) {
            // startActivity(new Intent(this, NgoHomeActivity.class));
            Toast.makeText(this, "Route to NGO Home", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
