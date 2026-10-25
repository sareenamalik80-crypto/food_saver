package com.example.food_saver.utils;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.ViewParent;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern ORG_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9 .&'-]{2,49}$");
    private static final Pattern PERSON_NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{2,49}$");
    private static final Pattern PHONE_PK = Pattern.compile("^(03\\d{9}|\\+923\\d{9})$");
    private static final Pattern FOOD_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 ,.&()'/-]{1,49}$");
    private static final Pattern QUANTITY = Pattern.compile("^(\\d+(\\.\\d+)?)\\s*[A-Za-z][A-Za-z ]{0,19}$");
    private static final Pattern VEHICLE = Pattern.compile("^[A-Za-z]{2,4}[- ]?(\\d{2}[- ])?\\d{1,4}$");
    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");

    private static final java.util.Map<EditText, Boolean> WATCHED = new java.util.WeakHashMap<>();

    public static final int DESCRIPTION_MAX = 300;
    public static final int COMMENT_MAX = 300;
    public static final int MESSAGE_MAX = 500;

    private ValidationUtils() {}

    public static String orgName(String label, String value) {
        if (TextUtils.isEmpty(value)) return label + " is required";
        if (value.length() < 3) return label + " must be at least 3 characters";
        if (value.length() > 50) return label + " can't be longer than 50 characters";
        if (!ORG_NAME.matcher(value).matches()) return label + " must start with a letter and can only contain letters, numbers, spaces and . & ' -";
        return null;
    }

    public static String personName(String label, String value) {
        if (TextUtils.isEmpty(value)) return label + " is required";
        if (value.length() < 3) return label + " must be at least 3 characters";
        if (value.length() > 50) return label + " can't be longer than 50 characters";
        if (!PERSON_NAME.matcher(value).matches()) return label + " can only contain letters and spaces";
        return null;
    }

    public static String email(String value) {
        if (TextUtils.isEmpty(value)) return "Email is required";
        if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) return "Enter a valid email address (e.g. name@gmail.com)";
        return null;
    }

    public static String newPassword(String value) {
        if (TextUtils.isEmpty(value)) return "Password is required";
        if (value.length() < 8) return "Password must be at least 8 characters";
        if (value.length() > 32) return "Password can't be longer than 32 characters";
        if (value.contains(" ")) return "Password can't contain spaces";
        if (!value.matches(".*[A-Z].*")) return "Password must contain at least one uppercase letter (A-Z)";
        if (!value.matches(".*[a-z].*")) return "Password must contain at least one lowercase letter (a-z)";
        if (!value.matches(".*\\d.*")) return "Password must contain at least one number (0-9)";
        if (!value.matches(".*[^A-Za-z0-9].*")) return "Password must contain at least one special character (e.g. @ # $ !)";
        return null;
    }

    public static String loginPassword(String value) {
        if (TextUtils.isEmpty(value)) return "Password is required";
        if (value.length() < 6) return "Password must be at least 6 characters";
        return null;
    }

    public static String phone(String value) {
        if (TextUtils.isEmpty(value)) return "Phone number is required";
        String clean = value.replaceAll("[\\s-]", "");
        if (!PHONE_PK.matcher(clean).matches()) return "Enter a valid mobile number (e.g. 03001234567)";
        return null;
    }

    public static String address(String label, String value) {
        if (TextUtils.isEmpty(value)) return label + " is required";
        if (value.length() < 5) return label + " is too short — please enter a complete address";
        if (value.length() > 200) return label + " can't be longer than 200 characters";
        if (!HAS_LETTER.matcher(value).matches()) return "Enter a valid " + label.toLowerCase();
        return null;
    }

    public static String foodName(String value) {
        if (TextUtils.isEmpty(value)) return "Food name is required";
        if (value.length() < 2) return "Food name must be at least 2 characters";
        if (value.length() > 50) return "Food name can't be longer than 50 characters";
        if (!HAS_LETTER.matcher(value).matches() || !FOOD_NAME.matcher(value).matches()) return "Enter a valid food name (e.g. Chicken Biryani)";
        return null;
    }

    public static String quantity(String value) {
        if (TextUtils.isEmpty(value)) return "Quantity is required";
        java.util.regex.Matcher m = QUANTITY.matcher(value);
        if (!m.matches()) return "Enter quantity with a unit (e.g. 10 kg or 20 plates)";
        double amount = Double.parseDouble(m.group(1));
        if (amount <= 0) return "Quantity must be greater than 0";
        if (amount > 10000) return "Quantity looks too large — please check it";
        return null;
    }

    public static String optionalText(String label, String value, int max) {
        if (value != null && value.length() > max) return label + " can't be longer than " + max + " characters";
        return null;
    }

    public static String vehicleNumber(String value) {
        if (TextUtils.isEmpty(value)) return "Vehicle number is required";
        if (!VEHICLE.matcher(value).matches()) return "Enter a valid vehicle number (e.g. RIM-1234)";
        return null;
    }

    public static String required(String label, String value) {
        if (TextUtils.isEmpty(value)) return label + " is required";
        return null;
    }

    public static final class Form {
        private boolean valid = true;

        public Form check(EditText field, String error) {
            if (error == null) {
                clear(field);
            } else {
                show(field, error);
                if (valid) {
                    field.requestFocus();
                    valid = false;
                }
            }
            return this;
        }

        public boolean isValid() {
            return valid;
        }
    }

    public static void show(EditText field, String error) {
        TextInputLayout layout = findLayout(field);
        if (layout != null) {
            layout.setError(error);
            if (!WATCHED.containsKey(field)) {
                WATCHED.put(field, Boolean.TRUE);
                field.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(Editable s) { layout.setError(null); }
                });
            }
        } else {
            field.setError(error);
        }
    }

    public static void clear(EditText field) {
        TextInputLayout layout = findLayout(field);
        if (layout != null) {
            layout.setError(null);
        } else {
            field.setError(null);
        }
    }

    private static TextInputLayout findLayout(EditText field) {
        ViewParent parent = field.getParent();
        while (parent != null) {
            if (parent instanceof TextInputLayout) return (TextInputLayout) parent;
            parent = parent.getParent();
        }
        return null;
    }
}
