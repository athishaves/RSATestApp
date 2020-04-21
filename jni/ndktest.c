#include <jni.h>
#include <string.h>

jstring Java_com_athishWorks_rsatestapp_MainActivity_helloWorld (JNIEnv* env, jobject obj) {

    return (*env)->NewStringUTF(env, "Hello World");

}