package com.example.food_saver.utils;

import android.util.Log;

import com.example.food_saver.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Best-effort AI check on a food photo before it's posted. Sends the image
 * to Gemini's vision API and asks it to flag three things: whether it looks
 * like a real food photo, whether it looks AI-generated/synthetic, and
 * whether it looks like a screenshot rather than a live camera photo.
 *
 * IMPORTANT: this is a heuristic check, not a guarantee — no AI detector is
 * 100% reliable, especially against a compressed JPEG. Treat a "suspicious"
 * result as a prompt for the donor to double-check, not as proof of fraud.
 *
 * Requires a Gemini API key in local.properties as GEMINI_API_KEY=... (see
 * build.gradle.kts — never hardcode the key in source, since that would
 * ship it inside the APK and leak it if this project is pushed to GitHub).
 */
public class AiImageChecker {

    private static final String MODEL = "gemini-3.6-flash";
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent";

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

        /** True if any check tripped — caller decides how to warn the donor. */
        public boolean isSuspicious() {
            return !isRealFood || isAiGenerated || isScreenshot;
        }
    }

    public interface CheckCallback {
        /** Called on a background thread — hop back to the UI thread (runOnUiThread) before touching views. */
        void onResult(Result result);

        /** Network/API/parsing failure — the check itself couldn't run. Callers should fail open (allow posting) rather than block a donor over a connectivity hiccup. */
        void onCheckFailed(String message);
    }

    public static void checkImage(String base64Jpeg, CheckCallback callback) {
        new Thread(() -> {
            try {
                String apiKey = BuildConfig.GEMINI_API_KEY;
                Log.d("AiImageChecker", "API key configured: " + (apiKey != null && !apiKey.isEmpty())
                        + " (length " + (apiKey != null ? apiKey.length() : 0) + ")");
                if (apiKey == null || apiKey.isEmpty()) {
                    callback.onCheckFailed("Gemini API key not configured (see local.properties).");
                    return;
                }

                String requestBody = buildRequestBody(base64Jpeg);
                String responseText = postJson(apiKey, requestBody);
                Log.d("AiImageChecker", "Raw response: " + responseText);
                Result result = parseResult(responseText);
                Log.d("AiImageChecker", "Parsed — realFood=" + result.isRealFood
                        + " aiGenerated=" + result.isAiGenerated
                        + " screenshot=" + result.isScreenshot
                        + " reason=" + result.reason);
                callback.onResult(result);

            } catch (Exception e) {
                Log.e("AiImageChecker", "Check failed", e);
                callback.onCheckFailed(e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        }).start();
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
        return requestBody.toString();
    }

    private static String postJson(String apiKey, String body) throws IOException {
        URL url = new URL(ENDPOINT + "?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String responseText = readStream(is);

            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Gemini API error (" + responseCode + "): " + responseText);
            }
            return responseText;
        } finally {
            conn.disconnect();
        }
    }

    private static Result parseResult(String responseText) throws Exception {
        JSONObject responseJson = new JSONObject(responseText);
        String modelText = responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Strip markdown code fences in case the model adds them despite instructions.
        String cleaned = modelText.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
        }

        JSONObject result = new JSONObject(cleaned);
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
