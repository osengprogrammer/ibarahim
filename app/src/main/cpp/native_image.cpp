#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <cstring>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NativeImage", __VA_ARGS__)

/**
 * =====================================================
 * HELPER: Clamp values to valid range
 * =====================================================
 */
inline float clamp(float v, float min, float max) {
    return std::max(min, std::min(max, v));
}

inline int clampInt(int v, int min, int max) {
    return std::max(min, std::min(max, v));
}

/**
 * =====================================================
 * UI / DEBUG ONLY
 * YUV420 -> ARGB Bitmap conversion
 * =====================================================
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_crashcourse_ml_nativeutils_NativeImageProcessor_yuv420ToArgb(
        JNIEnv *env,
        jobject,
        jobject yBuffer,
        jobject uBuffer,
        jobject vBuffer,
        jint width,
        jint height,
        jint yRowStride,
        jint uvRowStride,
        jint yPixelStride,
        jint uvPixelStride,
        jintArray outArgb
) {
    auto *yData = static_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto *uData = static_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    auto *vData = static_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));

    if (!yData || !uData || !vData) return;

    jint *out = env->GetIntArrayElements(outArgb, nullptr);
    int outIndex = 0;

    for (int y = 0; y < height; y++) {
        int yRow = y * yRowStride;
        int uvRow = (y >> 1) * uvRowStride;

        for (int x = 0; x < width; x++) {
            int yIndex = yRow + x * yPixelStride;
            
            // Y Channel
            int Y = (yData[yIndex] & 0xFF); 
            // Standard YUV usually doesn't subtract 16 for full range, but if your camera is limited range:
            // Y = std::max(Y - 16, 0); 
            // We will stick to standard conversion which handles range in the formula.

            // UV Channel
            int uvCol = (x >> 1) * uvPixelStride;
            int uvIndex = uvRow + uvCol;

            // Safe fetch using strides
            int U = (int)(uData[uvIndex] & 0xFF) - 128;
            int V = (int)(vData[uvIndex] & 0xFF) - 128;

            // Integer optimization for YUV -> RGB
            int r = (int)(Y + 1.370705f * V);
            int g = (int)(Y - 0.337633f * U - 0.698001f * V);
            int b = (int)(Y + 1.732446f * U);

            r = clampInt(r, 0, 255);
            g = clampInt(g, 0, 255);
            b = clampInt(b, 0, 255);

            out[outIndex++] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
    }
    env->ReleaseIntArrayElements(outArgb, out, 0);
}

/**
 * =====================================================
 * AUTHORITATIVE FACE PREPROCESSING (INFERENCE)
 * - Bilinear Interpolation (Smoother images = fewer ghosts)
 * - Correct Rotation Mapping (Fixes the "Edge Noise")
 * - Normalization [-1, 1]
 * =====================================================
 */
extern "C"
JNIEXPORT void JNICALL
Java_com_example_crashcourse_ml_nativeutils_NativeImageProcessor_preprocessFace(
        JNIEnv *env,
        jobject,
        jobject yBuffer,
        jobject uBuffer,
        jobject vBuffer,
        jint width,
        jint height,
        jint yRowStride,
        jint uvRowStride,
        jint yPixelStride,
        jint uvPixelStride,
        jint cropLeft,
        jint cropTop,
        jint cropWidth,
        jint cropHeight,
        jint rotation,
        jint outputSize,
        jobject outBuffer
) {
    auto *Y_ptr = static_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto *U_ptr = static_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    auto *V_ptr = static_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));
    auto *out   = static_cast<float *>(env->GetDirectBufferAddress(outBuffer));

    if (!Y_ptr || !U_ptr || !V_ptr || !out) {
        LOGE("Null buffer in preprocessFace");
        return;
    }

    // Loop over the TARGET output image (e.g., 112x112)
    for (int oy = 0; oy < outputSize; oy++) {
        for (int ox = 0; ox < outputSize; ox++) {
            
            // 1. Calculate relative position in the Crop Rect (0.0 to 1.0)
            // Adding 0.5f samples from the center of the pixel
            float fx = (ox + 0.5f) / (float)outputSize;
            float fy = (oy + 0.5f) / (float)outputSize;

            // 2. Map to the "Upright" coordinate system within the crop
            float sx = cropLeft + fx * cropWidth;
            float sy = cropTop  + fy * cropHeight;

            // 3. Rotate these coordinates to match the raw Sensor data
            // We are mapping: Upright(sx,sy) -> RawSensor(rx,ry)
            float rx, ry;
            switch (rotation) {
                case 90:
                    rx = sy;
                    ry = (float)width - sx - 1.0f;
                    break;
                case 180:
                    rx = (float)width - sx - 1.0f;
                    ry = (float)height - sy - 1.0f;
                    break;
                case 270:
                    rx = (float)height - sy - 1.0f;
                    ry = sx;
                    break;
                case 0:
                default:
                    rx = sx;
                    ry = sy;
                    break;
            }

            // 4. Bilinear Interpolation for Y (Luma)
            // This reduces "jagged" noise which AI often mistakes for features
            int x0 = (int)rx;
            int y0 = (int)ry;
            
            // Clamp to ensure we don't read outside valid memory
            x0 = clampInt(x0, 0, width - 2);
            y0 = clampInt(y0, 0, height - 2);

            float dx = rx - x0;
            float dy = ry - y0;

            // Read 4 neighbors
            int row1 = y0 * yRowStride;
            int row2 = (y0 + 1) * yRowStride;
            
            // Notice: We multiply x by yPixelStride just in case (though usually 1 for Y)
            float val00 = Y_ptr[row1 + x0 * yPixelStride] & 0xFF;
            float val10 = Y_ptr[row1 + (x0+1) * yPixelStride] & 0xFF;
            float val01 = Y_ptr[row2 + x0 * yPixelStride] & 0xFF;
            float val11 = Y_ptr[row2 + (x0+1) * yPixelStride] & 0xFF;

            // Weighted average
            float y_interp = (val00 * (1 - dx) * (1 - dy)) +
                             (val10 * dx * (1 - dy)) +
                             (val01 * (1 - dx) * dy) +
                             (val11 * dx * dy);

            // 5. Chroma (UV) - Nearest Neighbor is sufficient for color
            int crX = (int)rx >> 1;
            int crY = (int)ry >> 1;
            
            // Clamp chroma coordinates
            crX = clampInt(crX, 0, (width >> 1) - 1);
            crY = clampInt(crY, 0, (height >> 1) - 1);

            int uvOffset = crY * uvRowStride + crX * uvPixelStride;
            
            float u_val = (float)((U_ptr[uvOffset] & 0xFF) - 128);
            float v_val = (float)((V_ptr[uvOffset] & 0xFF) - 128);

            // 6. YUV -> RGB
            // Using slightly more accurate floats for ML models
            float r = y_interp + 1.402f * v_val;
            float g = y_interp - 0.344136f * u_val - 0.714136f * v_val;
            float b = y_interp + 1.772f * u_val;

            // 7. Normalize [-1, 1] and Write to Output
            // (r - 127.5) / 128.0
            int outIdx = (oy * outputSize + ox) * 3;
            
            out[outIdx + 0] = (clamp(r, 0.f, 255.f) - 127.5f) / 128.0f;
            out[outIdx + 1] = (clamp(g, 0.f, 255.f) - 127.5f) / 128.0f;
            out[outIdx + 2] = (clamp(b, 0.f, 255.f) - 127.5f) / 128.0f;
        }
    }
}