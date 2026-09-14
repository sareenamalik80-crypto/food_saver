package com.example.food_saver.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.Locale;

/**
 * Wraps FusedLocationProviderClient + Geocoder so the Post Food / Edit Food
 * screens can fill in "Pickup Location" from the donor's actual GPS
 * position instead of manual typing.
 *
 * Caller must have already checked/requested ACCESS_FINE_LOCATION before
 * calling fetchCurrentLocation — this class doesn't handle permissions.
 */
public class LocationHelper {

    public interface LocationCallback {
        /** address may be null if reverse-geocoding failed — lat/lng are still usable. */
        void onLocationFound(double latitude, double longitude, String address);
        void onError(String errorMessage);
    }

    @SuppressLint("MissingPermission") // caller is required to have checked permission first
    public static void fetchCurrentLocation(Context context, LocationCallback callback) {
        CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(request, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onError("Could not get current location. Make sure GPS is turned on.");
                        return;
                    }
                    String address = reverseGeocode(context, location);
                    callback.onLocationFound(location.getLatitude(), location.getLongitude(), address);
                })
                .addOnFailureListener(e -> callback.onError("Location fetch failed: " + e.getMessage()));
    }

    /** Best-effort address lookup — returns null (not a crash) if it fails. */
    private static String reverseGeocode(Context context, Location location) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            @SuppressWarnings("deprecation") // simplest synchronous API; fine for this use case
            List<Address> results = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (results != null && !results.isEmpty()) {
                return results.get(0).getAddressLine(0);
            }
        } catch (Exception ignored) {
            // Network/geocoder unavailable — caller falls back to showing raw coordinates.
        }
        return null;
    }
}
