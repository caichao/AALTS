#include <jni.h>
#include <string>
#include "fftsg_h_float.c"
/*
 * Class:     hust_cc_asynchronousacousticlocalization_utils_JniUtils
 * Method:    getCLanguageString
 * Signature: ()Ljava/lang/String;
 */
extern "C" {
JNIEXPORT jstring JNICALL
Java_hust_cc_asynchronousacousticlocalization_utils_JniUtils_sayHello(JNIEnv *env, jclass jobj) {
    std::string hello = "HelloWorld!!";
    return env->NewStringUTF(hello.c_str());
}


JNIEXPORT void JNICALL
Java_hust_cc_asynchronousacousticlocalization_utils_JniUtils_realForward(JNIEnv *env, jclass jobj,
                                                                 jfloatArray data, jint len,jfloatArray output) {
//    jboolean isCopy;
//    jfloat *dataP = static_cast<float*>(env->GetPrimitiveArrayCritical(data, &isCopy));
    jfloat *dataP = env->GetFloatArrayElements(data, 0);
    jsize n = env->GetArrayLength(data);
    int l = 1;
    while (l < len) {
        l = l * 2;
    }

    jfloat* sig = new jfloat[2*l];
    for(int i=0;i<n;i++){
        sig[2*i] = dataP[i];
    }
    cdft(2*l,-1,sig);
    env->SetFloatArrayRegion(output,0,2*l,sig);
    env->ReleaseFloatArrayElements(data, dataP, 0);
//    env->ReleasePrimitiveArrayCritical(data,dataP,0);
}


JNIEXPORT void JNICALL Java_hust_cc_asynchronousacousticlocalization_utils_JniUtils_xcorr(JNIEnv *env, jclass jobj,jfloatArray data1,jfloatArray data2,jfloatArray output){
    jfloat *dataP1 = env->GetFloatArrayElements(data1, 0);
    jfloat *dataP2 = env->GetFloatArrayElements(data2, 0);
    jsize n = env->GetArrayLength(data1);
    jfloat* res = new jfloat[n];
    jfloat* corr = new jfloat[n/2];
    res[0] = dataP1[0]*dataP2[0];
    res[1] = dataP1[1]*dataP2[1];
    for(int i=1;i<n/2;i++){
        float a = dataP1[2*i];
        float b = dataP1[2*i+1];
        float c = dataP2[2*i];
        float d = dataP2[2*i+1];
        res[2*i] = a*c+b*d;
        res[2*i+1] = b*c-a*d;
    }
    cdft(n,1,res);
    for(int i=0;i<n;i++){
        float a = res[2*i];
        float b = res[2*i+1];
        corr[i] = sqrt(a*a+b*b)*2/n;
    }
    env->SetFloatArrayRegion(output,0,n/2,corr);
    env->ReleaseFloatArrayElements(data1, dataP1, 0);
    env->ReleaseFloatArrayElements(data2, dataP2, 0);




}

//JNIEXPORT void JNICALL
//JavaCritical_hust_cc_asynchronousacousticlocalization_utils_JniUtils_realForward(jint length,jfloat* data) {
//    jfloat* sig = new jfloat[2*length];
//    for(int i=0;i<length;i++){
//        sig[2*i] = data[i];
//    }
//    cdft(2*length,-1,sig);
//    for(int i=0;i<length;i++){
//        data[i] = sig[i];
//    }
//}


}