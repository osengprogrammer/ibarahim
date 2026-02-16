#include <jni.h>
#include <string>

/**
 * 🛡️ Security Logic V.16.5 - XOR Shield
 */
const char MASK[] = {0x01, 0x05, 0x07, 0x02, 0x04}; 

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_crashcourse_util_NativeKeyStore_getIsoKey(JNIEnv* env, jobject /* thiz */) {
    
    unsigned char obfuscated_key[] = {
        0x40, 0x5f, 0x52, 0x50, 0x45, 0x5e, 0x52, 0x40, 0x44, 0x57, 0x56, 0x41, 0x5e, 0x33, 0x35, 0x35, 0x34
    };
    
    int key_len = sizeof(obfuscated_key);
    std::string decrypted_key = "";

    for (int i = 0; i < key_len; i++) {
        decrypted_key += (char)(obfuscated_key[i] ^ MASK[i % sizeof(MASK)]);
    }

    return env->NewStringUTF(decrypted_key.c_str());
}