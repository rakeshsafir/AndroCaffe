#ifndef ANDROCAFFE_ANDROCAFFE_H
#define ANDROCAFFE_ANDROCAFFE_H

#include <time.h>
#include <math.h>

#include <android/log.h>

#define CPU_ONLY

#define app_name "AndroCaffe"

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, app_name, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, app_name, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, app_name, __VA_ARGS__))


#endif //ANDROCAFFE_ANDROCAFFE_H
