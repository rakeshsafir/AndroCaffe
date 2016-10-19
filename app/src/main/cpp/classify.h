#ifndef ANDROCAFFE_CLASSIFY_H
#define ANDROCAFFE_CLASSIFY_H

int loadPrototxt(const char *fPath);
int loadCaffeModel(const char *fPath);
int classify(unsigned char *inImg, int *info);

#endif //ANDROCAFFE_CLASSIFY_H
