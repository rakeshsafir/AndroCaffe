
#include "androcaffe.h"
#include "classify.h"

#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <opencv2/imgproc/imgproc.hpp>


jint StdStrFromJniStr(JNIEnv* env, jstring jniStr, std::string &stdStr) {
    jint retCode = 0;

    const char * res = NULL;

    jboolean isCopy = true;
    res = env->GetStringUTFChars(jniStr, &isCopy);
    if(true != isCopy && NULL != res) {
        retCode = -1;
    }
    stdStr = std::string(res);

    env->ReleaseStringUTFChars(jniStr, res);

    return retCode;
}

Classifier *gClassify = NULL;
extern "C"
jint
Java_com_gputech_androcaffe_MainActivity_jniCaffeInit(JNIEnv* env,
                                                      jclass clazz,
                                                      jstring model_file,
                                                      jstring trained_file,
                                                      jstring mean_file,
                                                      jstring label_file) {
    jint retCode = -1;
    std::string model_str;
    std::string trained_str;
    std::string mean_str;
    std::string label_str;

    do {
        if((retCode = StdStrFromJniStr(env, model_file, model_str)) != 0) {
            break;
        }
        if((retCode = StdStrFromJniStr(env, trained_file, trained_str)) != 0) {
            break;
        }
        if((retCode = StdStrFromJniStr(env, mean_file, mean_str)) != 0) {
            break;
        }
        if((retCode = StdStrFromJniStr(env, label_file, label_str)) != 0) {
            break;
        }

        gClassify = new Classifier(model_str, trained_str, mean_str, label_str);
        if(NULL == gClassify) {
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
                                                       jstring imgPath,
                                                       jintArray info) {

    jint retCode = -1;
    LOGI("jniDoClassify +--->\n");

    jint *i = NULL;
    const char *imgPathStr = NULL;

    jboolean isCopy = false;


    do {
        isCopy = false;
        i = (env)->GetIntArrayElements(info, &isCopy);
        if(true != isCopy || NULL == i) {
            break;
        }

        isCopy = false;
        imgPathStr = env->GetStringUTFChars(imgPath, &isCopy);
        if(true != isCopy || NULL == imgPathStr) {
            break;
        }

        cv::Mat img = cv::imread(imgPathStr, CV_LOAD_IMAGE_COLOR);
        std::vector<Prediction> predictions = gClassify->Classify(img);

        /* Print the top N predictions. */
        for (size_t i = 0; i < predictions.size(); ++i) {
            Prediction p = predictions[i];
            LOGI("%f - %s", p.second, static_cast<std::string>(p.first).c_str());
        }

        retCode = 0;
    }while(0);

    if(NULL != imgPathStr) {
        env->ReleaseStringUTFChars(imgPath, imgPathStr);
        imgPathStr = NULL;
    }

    if(NULL != i) {
        env->ReleaseIntArrayElements(info, i, 0);
        i = NULL;
    }



    LOGI("<----jniDoClassify\n");

    return retCode;
}

extern "C"
void
Java_com_gputech_androcaffe_MainActivity_jniCaffeDeInit(JNIEnv* env) {
    if(NULL != gClassify) {
        delete gClassify;
        gClassify = NULL;
    }
}

