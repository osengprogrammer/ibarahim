#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <cstring>
#include <string>

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NativeImage", __VA_ARGS__)

/**
 * =====================================================
 * SHARED HELPERS
 * =====================================================
 */
inline float clamp(float v, float min, float max) {
    return std::max(min, std::min(max, v));
}

inline int clampInt(int v, int min, int max) {
    return std::max(min, std::min(max, v));
}

extern "C" {

/**
 * =====================================================
 * 🛡️ LOGIC FOR: com.example.crashcourse.util.NativeKeyStore
 * =====================================================
 */
JNIEXPORT jstring JNICALL
Java_com_example_crashcourse_util_NativeKeyStore_getIsoKey(
    JNIEnv* env, jobject /* this */) {
    
    std::string key = "";
    key += 'A'; key += 'Z'; key += 'U'; key += 'R'; key += 'A';
    key += '_';
    key += 'S'; key += 'E'; key += 'C'; key += 'U'; key += 'R'; key += 'E';
    key += '_';
    key += '2'; key += '0'; key += '2'; key += '6'; 
    
    return env->NewStringUTF(key.c_str());
}

/**
 * =====================================================
 * ✅ LOGIC FOR: com.example.crashcourse.ml.nativeutils.NativeMath
 * (Alamat diperbarui agar sinkron dengan NativeMath.kt baru)
 * =====================================================
 */
JNIEXPORT jfloat JNICALL
Java_com_example_crashcourse_ml_nativeutils_NativeMath_cosineDistance(
    JNIEnv* env, jobject /* this */, jfloatArray a, jfloatArray b) {
    
    jsize len = env->GetArrayLength(a);
    if (len != env->GetArrayLength(b)) return 1.0f;

    jfloat* ptrA = env->GetFloatArrayElements(a, nullptr);
    jfloat* ptrB = env->GetFloatArrayElements(b, nullptr);

    float dot = 0.0f, normA = 0.0f, normB = 0.0f;

    for (int i = 0; i < len; i++) {
        dot += ptrA[i] * ptrB[i];
        normA += ptrA[i] * ptrA[i];
        normB += ptrB[i] * ptrB[i];
    }

    env->ReleaseFloatArrayElements(a, ptrA, JNI_ABORT);
    env->ReleaseFloatArrayElements(b, ptrB, JNI_ABORT);

    float denom = std::sqrt(normA) * std::sqrt(normB);
    if (denom < 1e-6f) return 1.0f;

    float similarity = dot / denom;
    float sim = std::max(-1.0f, std::min(1.0f, similarity));
    
    return 1.0f - sim;
}

JNIEXPORT void JNICALL
Java_com_example_crashcourse_ml_nativeutils_NativeMath_preprocessImage(
    JNIEnv* env, jobject /* this */, jobject byteBuffer, jint size) {
    
    float* pixels = (float*)env->GetDirectBufferAddress(byteBuffer);
    if (pixels == nullptr) return;

    for (int i = 0; i < size; i++) {
        pixels[i] = (pixels[i] - 127.5f) / 128.0f;
    }
}

/**
 * =====================================================
 * ✅ LOGIC FOR: com.example.crashcourse.ml.nativeutils.NativeImageProcessor
 * =====================================================
 */
JNIEXPORT void JNICALL
Java_com_example_crashcourse_ml_nativeutils_NativeImageProcessor_preprocessFace(
        JNIEnv *env, jobject, jobject yBuffer, jobject uBuffer, jobject vBuffer,
        jint width, jint height, jint yRowStride, jint uvRowStride,
        jint yPixelStride, jint uvPixelStride, jint cropLeft, jint cropTop,
        jint cropWidth, jint cropHeight, jint rotation, jint outputSize,
        jobject outBuffer
) {
    auto *Y_ptr = static_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    auto *U_ptr = static_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    auto *V_ptr = static_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));
    auto *out   = static_cast<float *>(env->GetDirectBufferAddress(outBuffer));

    if (!Y_ptr || !U_ptr || !V_ptr || !out) return;

    for (int oy = 0; oy < outputSize; oy++) {
        for (int ox = 0; ox < outputSize; ox++) {
            float fx = (ox + 0.5f) / (float)outputSize;
            float fy = (oy + 0.5f) / (float)outputSize;

            float sx = (float)cropLeft + fx * (float)cropWidth;
            float sy = (float)cropTop  + fy * (float)cropHeight;

            float rx, ry;
            switch (rotation) {
                case 90:  rx = sy; ry = (float)width - sx - 1.0f; break;
                case 180: rx = (float)width - sx - 1.0f; ry = (float)height - sy - 1.0f; break;
                case 270: rx = (float)height - sy - 1.0f; ry = sx; break;
                default:  rx = sx; ry = sy; break;
            }

            int x0 = clampInt((int)rx, 0, width - 2);
            int y0 = clampInt((int)ry, 0, height - 2);
            float dx = rx - (float)x0;
            float dy = ry - (float)y0;

            int r1 = y0 * yRowStride;
            int r2 = (y0 + 1) * yRowStride;
            
            float v00 = (float)(Y_ptr[r1 + x0 * yPixelStride] & 0xFF);
            float v10 = (float)(Y_ptr[r1 + (x0+1) * yPixelStride] & 0xFF);
            float v01 = (float)(Y_ptr[r2 + x0 * yPixelStride] & 0xFF);
            float v11 = (float)(Y_ptr[r2 + (x0+1) * yPixelStride] & 0xFF);

            float y_interp = (v00 * (1.0f-dx) * (1.0f-dy)) + (v10 * dx * (1.0f-dy)) +
                             (v01 * (1.0f-dx) * dy) + (v11 * dx * dy);

            int crX = clampInt((int)rx >> 1, 0, (width >> 1) - 1);
            int crY = clampInt((int)ry >> 1, 0, (height >> 1) - 1);
            int uvOff = crY * uvRowStride + crX * uvPixelStride;
            
            float u_val = (float)((U_ptr[uvOff] & 0xFF) - 128);
            float v_val = (float)((V_ptr[uvOff] & 0xFF) - 128);

            float r = y_interp + 1.402f * v_val;
            float g = y_interp - 0.344136f * u_val - 0.714136f * v_val;
            float b = y_interp + 1.772f * u_val;

            int outIdx = (oy * outputSize + ox) * 3;
            out[outIdx + 0] = (clamp(r, 0.f, 255.f) - 127.5f) / 128.0f;
            out[outIdx + 1] = (clamp(g, 0.f, 255.f) - 127.5f) / 128.0f;
            out[outIdx + 2] = (clamp(b, 0.f, 255.f) - 127.5f) / 128.0f;
        }
    }
}

} // extern "C"