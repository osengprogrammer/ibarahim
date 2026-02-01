#include <jni.h>
#include <cmath>
#include <algorithm>

extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_crashcourse_utils_NativeMath_cosineDistance(
    JNIEnv* env, jobject /* this */, jfloatArray a, jfloatArray b) {
    
    jsize len = env->GetArrayLength(a);
    if (len != env->GetArrayLength(b)) return 1.0f;

    jfloat* ptrA = env->GetFloatArrayElements(a, nullptr);
    jfloat* ptrB = env->GetFloatArrayElements(b, nullptr);

    float dot = 0.0f;
    float normA = 0.0f;
    float normB = 0.0f;

    for (int i = 0; i < len; i++) {
        dot += ptrA[i] * ptrB[i];
        normA += ptrA[i] * ptrA[i];
        normB += ptrB[i] * ptrB[i];
    }

    env->ReleaseFloatArrayElements(a, ptrA, JNI_ABORT);
    env->ReleaseFloatArrayElements(b, ptrB, JNI_ABORT);

    float denom = std::sqrt(normA) * std::sqrt(normB);
    if (denom == 0.0f) return 1.0f;

    float similarity = dot / denom;
    float sim = std::max(-1.0f, std::min(1.0f, similarity));
    
    return 1.0f - sim;
}