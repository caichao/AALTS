#include <jni.h>
#include <string>
/*
 * Class:     hust_cc_asynchronousacousticlocalization_utils_JniUtils
 * Method:    getCLanguageString
 * Signature: ()Ljava/lang/String;
 */
extern "C"
    JNIEXPORT jstring JNICALL Java_hust_cc_asynchronousacousticlocalization_utils_JniUtils_sayHello(JNIEnv *env, jclass jobj){
        std::string hello = "HelloWorld!!";
        return env->NewStringUTF(hello.c_str());
    }