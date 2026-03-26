#include <jni.h>
#include <string>

// Credenciales ofuscadas en native code — no aparecen como strings en DEX
// Para mayor seguridad, las strings se construyen en runtime

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_mg_costeoapp_core_security_NativeSecrets_getSupabaseUrl(JNIEnv *env, jobject) {
    // hwjpvcnauoibrnendhmg.supabase.co
    std::string url = "https://";
    url += "hwjpvcnauoib";
    url += "rnendhmg";
    url += ".supabase.co";
    return env->NewStringUTF(url.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_mg_costeoapp_core_security_NativeSecrets_getSupabaseAnonKey(JNIEnv *env, jobject) {
    std::string key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.";
    key += "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh3anB2Y25hdW9pYnJuZW5kaG1nIiw";
    key += "icm9sZSI6ImFub24iLCJpYXQiOjE3NzQxMjMwOTIsImV4cCI6MjA4OTY5OTA5Mn0.";
    key += "ircTPqAZlm8GbQqoFjdoR9UUXsmL-piqdFNmFSk4wkY";
    return env->NewStringUTF(key.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_mg_costeoapp_core_security_NativeSecrets_getBloomreachAccountId(JNIEnv *env, jobject) {
    return env->NewStringUTF("7024");
}

JNIEXPORT jstring JNICALL
Java_com_mg_costeoapp_core_security_NativeSecrets_getBloomreachAuthKey(JNIEnv *env, jobject) {
    std::string key = "ev7lib";
    key += "hybjg5";
    key += "h1d1";
    return env->NewStringUTF(key.c_str());
}

}
