
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
                                                      jclass clazz) {
    jint retCode = -1;
    std::string model_str = "/data/data/com.gputech.androcaffe/app_execdir/model.prototxt";
    std::string trained_str = "/data/data/com.gputech.androcaffe/app_execdir/model.caffemodel";
    std::string mean_str = "/data/data/com.gputech.androcaffe/app_execdir/model.binaryproto";
    std::string label_str = "/data/data/com.gputech.androcaffe/app_execdir/model.txt";

    do {
        gClassify = new Classifier(model_str, trained_str, mean_str, label_str);
        if(NULL == gClassify) {
            LOGE("Failed to create Classifier Object...\n");
            break;
        }
        LOGI("Classifier Object Created...\n");
        retCode = 0;

    }while(0);

    return retCode;
}


extern "C"
jint
Java_com_gputech_androcaffe_MainActivity_jniDoClassify(JNIEnv* env,
                                                       jclass clazz,
                                                       jstring imgPath) {

    jint retCode = -1;
    LOGI("jniDoClassify +--->\n");

    const char * imgPathStr = NULL;
    jboolean isCopy = true;

    imgPathStr = env->GetStringUTFChars(imgPath, &isCopy);

    do {

        if(true != isCopy && NULL != imgPathStr) {
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

    env->ReleaseStringUTFChars(imgPath, imgPathStr);

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

