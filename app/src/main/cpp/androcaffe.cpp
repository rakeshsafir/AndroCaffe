#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include "androcaffe.h"
#include "classify.h"

extern "C"
jint
Java_com_gputech_androcaffe_MainActivity_jniCaffeInit(JNIEnv* env) {

    jint retCode = -1;
    const char *fPath;
    do {

        fPath = "/data/data/com.gputech.androcaffe/app_execdir/deploy.prototxt";
        if((retCode = loadPrototxt(fPath)) != 0) {
            LOGE("loadPrototxt(%s) failed...\n", fPath);
            break;
        }

        fPath = "/data/data/com.gputech.androcaffe/app_execdir/snapshot_iter_10000.caffemodel";
        if((retCode = loadCaffeModel(fPath)) != 0) {
            LOGE("loadPrototxt(%s) failed...\n", fPath);
            break;
        }

        retCode = 0;
    }while(0);

    return retCode;
}


extern "C"
jint
Java_com_gputech_androcaffe_MainActivity_jniDoClassify(JNIEnv* env,
                                                       jclass clazz,
                                                       jobject bitmapIn,
                                                       jintArray info) {
    void*	bi;
    void*   bo;
    jint*   i;

    jint retCode = -1;
    LOGI("jniDoClassify +--->\n");

    do {
        if((i = env->GetIntArrayElements(info, NULL)) == NULL) {
            LOGE("env->GetIntArrayElements failed...\n");
            break;
        }
        if( ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_lockPixels(env, bitmapIn, &bi)) {
            LOGE("AndroidBitmap_lockPixels(inputImage) failed...\n");
            break;
        }

        if((retCode = classify((unsigned char *)bi, (int *)i)) != 0 ) {
            LOGE("classify() failed...\n");
            break;
        }

        if(ANDROID_BITMAP_RESULT_SUCCESS != AndroidBitmap_unlockPixels(env, bitmapIn)) {
            LOGE("AndroidBitmap_unlockPixels(inputImage) failed...\n");
            break;
        }

        env->ReleaseIntArrayElements(info, i, 0);
        retCode = 0;
    }while(0);

    LOGI("<----jniDoClassify\n");

    return retCode;
}
