package com.example.food_saver.utils;

import android.util.Log;

import com.example.food_saver.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
public class AiImageChecker {

    private static final String TAG = "AiImageChecker";

    /** Tried in order — first model is preferred, the rest are fallbacks when it's busy. */
    private static final String[] MODELS = {
            "gemini-3.6-flash",       // primary
            "gemini-3.5-flash-lite",  // fallback 1 — fast & lightweight
            "gemini-3.1-flash-lite"   // fallback 2 — stable lite model
    };

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    public static class Result {
        public final boolean isRealFood;
        public final boolean isAiGenerated;
        public final boolean isScreenshot;
        public final String reason;

        Result(boolean isRealFood, boolean isAiGenerated, boolean isScreenshot, String reason) {
            this.isRealFood = isRealFood;
            this.isAiGenerated = isAiGenerated;
            this.isScreenshot = isScreenshot;
            this.reason = reason;
        }
        public boolean isSuspicious() {
            return !isRealFood || isAiGenerated || isScreenshot;
        }
    }

    public interface CheckCallback {
        void onResult(Result result);
        void onCheckFailed(String message);
    }

    private static class ApiException extends IOException {
        final int code;
        ApiException(int code, String body) {
            super("Gemini API error (" + code + "): " + body);
            this.code = code;
        }
    }
    private static final int MAX_ATTEMPTS_PER_MODEL = 2;

    public static void checkImage(String base64Jpeg, CheckCallback callback) {
        new Thread(() -> {
            String apiKey = BuildConfig.GEMINI_API_KEY;
            Log.d(TAG, "API key configured: " + (apiKey != null && !apiKey.isEmpty())
                    + " (length " + (apiKey != null ? apiKey.length() : 0) + ")");
            if (apiKey == null || apiKey.isEmpty()) {
                callback.onCheckFailed("AI service is not configured in this build.");
                return;
            }

            String requestBody;
            try {
                requestBody = buildRequestBody(base64Jpeg);
            } catch (Exception e) {
                Log.e(TAG, "Could not build request", e);
                callback.onCheckFailed(friendlyMessage(e));
                return;
            }

            Exception lastError = null;

            modelLoop:
            for (String model : MODELS) {
                for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_MODEL; attempt++) {
                    try {
                        String responseText = postJson(model, apiKey, requestBody);
                        Log.d(TAG, "[" + model + "] Raw response: " + responseText);
                        Result result = parseResult(responseText);
                        Log.d(TAG, "[" + model + "] Parsed — realFood=" + result.isRealFood
                                + " aiGenerated=" + result.isAiGenerated
                                + " screenshot=" + result.isScreenshot
                                + " reason=" + result.reason);
                        callback.onResult(result);
                        return;
                    } catch (Exception e) {
                        lastError = e;
                        Log.e(TAG, "[" + model + "] Attempt " + attempt + " failed", e);

                        if (isNetworkError(e) && attempt == MAX_ATTEMPTS_PER_MODEL) {
                            break modelLoop;
                        }
                        if (attempt == MAX_ATTEMPTS_PER_MODEL || !isRetryable(e)) {
                            if (!shouldTryNextModel(e)) break modelLoop;
                            Log.w(TAG, "Switching from " + model + " to next fallback model");
                            break;
                        }
                        try {
                            Thread.sleep(attempt * 2000L); // 2s before retrying same model
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break modelLoop;
                        }
                    }
                }
            }
            callback.onCheckFailed(friendlyMessage(lastError));
        }).start();
    }
    private static boolean isRetryable(Exception e) {
        if (e instanceof ApiException) {
            int code = ((ApiException) e).code;
            return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
        }
        return e instanceof SocketTimeoutException
                || e instanceof ConnectException
                || e instanceof UnknownHostException
                || e instanceof org.json.JSONException
                || e instanceof IOException;
    }

    private static boolean shouldTryNextModel(Exception e) {
        if (e instanceof ApiException) {
            int code = ((ApiException) e).code;
            // 404 = model retired/renamed; 429/5xx = this model is busy.
            // 400/401/403 = bad key or bad request → same on every model, so stop.
            return code == 404 || code == 429 || code >= 500;
        }
        return e instanceof org.json.JSONException
                || (e instanceof IOException && !isNetworkError(e));
    }

    private static boolean isNetworkError(Exception e) {
        return e instanceof UnknownHostException
                || e instanceof ConnectException
                || e instanceof SocketTimeoutException;
    }

    private static String friendlyMessage(Exception e) {
        if (e instanceof ApiException) {
            int code = ((ApiException) e).code;
            if (code == 429) return "The AI service is receiving too many requests right now.";
            if (code == 500 || code == 502 || code == 503 || code == 504) return "The AI service is busy right now.";
            if (code == 400 || code == 401 || code == 403) return "The AI service rejected the request (code " + code + ").";
            if (code == 404) return "The AI model is no longer available (code 404).";
            return "AI service error (code " + code + ").";
        }
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return "No internet connection.";
        }
        if (e instanceof SocketTimeoutException) {
            return "The connection is too slow (timed out).";
        }
        return "Could not read the AI result (" + (e != null && e.getMessage() != null ? e.getMessage() : "unknown error") + ").";
    }

    private static String buildRequestBody(String base64Jpeg) throws Exception {
        String prompt = "You are checking a photo uploaded to a food-donation app, which requires a LIVE "
                + "camera photo of real food (no gallery uploads, no AI-generated images, no screenshots). "
                + "Look at the image and respond with ONLY this exact JSON, no other text, no markdown: "
                + "{\"is_real_food\": true or false, \"is_ai_generated\": true or false, "
                + "\"is_screenshot\": true or false, \"reason\": \"one short sentence\"}. "
                + "is_real_food should be true only if this genuinely looks like real food. "
                + "is_ai_generated should be true if the image looks synthetic/AI-generated "
                + "(unnatural textures, artifacts, overly perfect lighting, warped details). "
                + "is_screenshot should be true if this looks like a screenshot of a phone, website, "
                + "or app rather than a direct camera photo.";

        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "image/jpeg");
        inlineData.put("data", base64Jpeg);

        JSONObject imagePart = new JSONObject();
        imagePart.put("inline_data", inlineData);

        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(textPart);
        parts.put(imagePart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);
        return requestBody.toString();
    }

    private static String postJson(String model, String apiKey, String body) throws IOException {
        URL url = new URL(BASE_URL + model + ":generateContent");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-goog-api-key", apiKey);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String responseText = readStream(is);

            if (responseCode < 200 || responseCode >= 300) {
                throw new ApiException(responseCode, responseText);
            }
            return responseText;
        } finally {
            conn.disconnect();
        }
    }

    private static Result parseResult(String responseText) throws Exception {
        JSONObject responseJson = new JSONObject(responseText);
        JSONArray candidates = responseJson.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            throw new IOException("empty AI response");
        }
        JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
        JSONArray parts = content != null ? content.optJSONArray("parts") : null;
        if (parts == null || parts.length() == 0) {
            throw new IOException("empty AI response");
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (part.optBoolean("thought", false)) continue;
            text.append(part.optString("text", ""));
        }
        String modelText = text.toString();
        int startIdx = modelText.indexOf('{');
        int endIdx = modelText.lastIndexOf('}');
        if (startIdx < 0 || endIdx <= startIdx) {
            throw new IOException("no JSON in AI response");
        }

        JSONObject result = new JSONObject(modelText.substring(startIdx, endIdx + 1));
        boolean isRealFood = result.optBoolean("is_real_food", true);
        boolean isAiGenerated = result.optBoolean("is_ai_generated", false);
        boolean isScreenshot = result.optBoolean("is_screenshot", false);
        String reason = result.optString("reason", "");

        return new Result(isRealFood, isAiGenerated, isScreenshot, reason);
    }

    private static String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }
}