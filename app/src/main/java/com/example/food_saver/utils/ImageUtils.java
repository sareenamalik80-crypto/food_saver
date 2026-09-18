package com.example.food_saver.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Storage requires the paid Blaze plan, so images are instead
 * compressed and stored as Base64 strings directly inside Firestore
 * documents. Firestore caps a document at 1 MiB total, so images are
 * downsampled and JPEG-compressed to stay comfortably under that.
 *
 * IMPORTANT: call compressImageToBase64 on a background thread — decoding
 * and compressing a photo can take a noticeable moment and will freeze the
 * UI if run on the main thread.
 */
public class ImageUtils {

    /**
     * @param maxDimension the longest side (width or height) the image will
     *                     be downsampled to, e.g. 800-1024 for food photos
     *                     or verification documents.
     * @param quality      JPEG compression quality, 0-100. 50-70 gives a
     *                     good size/quality tradeoff for this use case.
     */
    public static String compressImageToBase64(Context context, Uri imageUri,
                                                 int maxDimension, int quality) throws IOException {
        Bitmap bitmap = decodeSampledBitmap(context, imageUri, maxDimension);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] bytes = outputStream.toByteArray();
        bitmap.recycle();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    /** Decodes a stored Base64 string back to raw bytes — feed this straight into Glide. */
    public static byte[] decodeBase64ToBytes(String base64) {
        if (base64 == null || base64.isEmpty()) return new byte[0];
        return Base64.decode(base64, Base64.NO_WRAP);
    }

    private static Bitmap decodeSampledBitmap(Context context, Uri uri, int maxDimension) throws IOException {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, boundsOptions);
        }

        int inSampleSize = calculateInSampleSize(boundsOptions, maxDimension, maxDimension);

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = inSampleSize;

        Bitmap bitmap;
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, decodeOptions);
        }

        if (bitmap == null) {
            throw new IOException("Could not decode image.");
        }
        return bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
