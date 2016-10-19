cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/include)

add_library( androcaffe

             SHARED

             src/main/cpp/classify.cpp
             src/main/cpp/androcaffe.cpp )

add_library( Caffe SHARED IMPORTED )
set_target_properties(Caffe PROPERTIES IMPORTED_LOCATION /home/linuxdev/Development/Android/AndroidStudioApplications/AndroCaffe/app/src/main/jniLibs/${ANDROID_ABI}/libcaffe.so)

add_library( OpenCVImgProc STATIC IMPORTED )
set_target_properties(OpenCVImgProc PROPERTIES IMPORTED_LOCATION /home/linuxdev/Development/Android/AndroidStudioApplications/AndroCaffe/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_imgproc.a)

find_library( log-lib log )
find_library( jnigraphics-lib jnigraphics )

target_link_libraries( androcaffe Caffe OpenCVImgProc ${log-lib} ${jnigraphics-lib} )
