#include <jni.h>


JNIEXPORT jstring JNICALL
Java_com_pepsa_netposcontactlesssdkjava_presentation_ui_activities_MainActivity_getApiKey(
        JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "Just a random string");
}