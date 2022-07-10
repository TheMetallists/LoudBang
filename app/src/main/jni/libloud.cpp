#include "jni_link.h"
#include <iostream>
#include "lbenc2/wenc.h"
#include <android/log.h>
#include <stdio.h>
#include <math.h>

int mains() {
    return 220;
}

#define APPNAME "Messodj"
#define WSPR_SYMBOL_COUNT 162

extern "C" JNIEXPORT jbyteArray

JNICALL
Java_aq_metallists_loudbang_cutil_CJarInterface_WSPREncodeToPCM
        (JNIEnv *env, jclass cls, jstring j_calls, jstring j_loca, jint j_powr, jint j_offset,
         jboolean lsb_mod) {
    //JTEncode jit;
    uint8_t symbols[WSPR_SYMBOL_COUNT];

    const char *callsign = env->GetStringUTFChars(j_calls, 0);
    const char *loca = env->GetStringUTFChars(j_loca, 0);

    //jit.wspr_encode(callsign, loca, (uint8_t)j_powr, symbols);
    char powr[3];
    snprintf(powr, 3, "%02d", (int) j_powr);

    __android_log_print(ANDROID_LOG_WARN, APPNAME, "WENCODE: %s %s %s", callsign, loca, powr);


    int mt = LB_WSPR_Encode2symbolz(symbols, callsign, loca, powr);
    __android_log_print(ANDROID_LOG_WARN, APPNAME, "Messodj typo: %d", mt);

    env->ReleaseStringUTFChars(j_calls, callsign);
    env->ReleaseStringUTFChars(j_loca, loca);


    double TAU = 2 * M_PI;
    //short sound[WSPR_SYMBOL_COUNT * WSPR_SYMBOL_LENGTH];
    short *sound = (short *) malloc(sizeof(short) * WSPR_SYMBOL_COUNT * WSPR_SYMBOL_LENGTH);
    memset(sound, 0, sizeof(short) * WSPR_SYMBOL_COUNT * WSPR_SYMBOL_LENGTH);

    __android_log_print(ANDROID_LOG_WARN, APPNAME, "Target array length: %d",
                        WSPR_SYMBOL_COUNT * WSPR_SYMBOL_LENGTH);

    short volume = 16383;
    for (int i = 0; i < WSPR_SYMBOL_COUNT; i++) {
        if (lsb_mod) {
            symbols[i] = (uint8_t) 3 - symbols[i];
        }
        double frequency = 1500 + ((int) j_offset) + symbols[i] * 1.4548;
        double theta = frequency * TAU / (double) 12000;
        // 'volume' is UInt16 with range 0 thru Uint16.MaxValue ( = 65 535)
        // we need 'amp' to have the range of 0 thru Int16.MaxValue ( = 32 767)
        double amp = volume >> 2; // so we simply set amp = volume / 2
        for (int step = 0; step < WSPR_SYMBOL_LENGTH; step++) {
            if (((i * WSPR_SYMBOL_LENGTH) + step) % 10000 == 0)
                __android_log_print(ANDROID_LOG_VERBOSE, APPNAME, "W @ %d",
                                    (i * WSPR_SYMBOL_LENGTH) + step);

            sound[(i * WSPR_SYMBOL_LENGTH) + step] = (short) (amp * sin(theta * (double) step));
        }
    }


    jbyteArray ret = env->NewByteArray(WSPR_SYMBOL_COUNT * WSPR_SYMBOL_LENGTH * sizeof(short));
    env->SetByteArrayRegion(ret, 0, WSPR_SYMBOL_COUNT * WSPR_SYMBOL_LENGTH * sizeof(short),
                            (jbyte *) sound);
    free(sound);// is it work?
    return ret;
}


extern "C"
JNIEXPORT jint JNICALL
Java_aq_metallists_loudbang_cutil_CJarInterface_radioCheck(JNIEnv *env, jclass clazz,
                                                           jint testvar) {
    return (jint) (testvar * 42);
}


unsigned char *as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength(array);
    unsigned char *buf = new unsigned char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return buf;
}

extern "C" jobjectArray jani_do_process(JNIEnv *env, jclass clazz,
                                        unsigned char *soundarr, int len, double jdialfreq,
                                        jboolean lsb_mode);

extern "C"
JNIEXPORT jobjectArray

JNICALL
Java_aq_metallists_loudbang_cutil_CJarInterface_WSPRDecodeFromPcm(JNIEnv *env, jclass clazz,
                                                                  jbyteArray sound,
                                                                  jdouble dialfreq, jboolean lsb) {
    unsigned char *soundarr = as_unsigned_char_array(env, sound);

    return jani_do_process(env, clazz, soundarr, (int) env->GetArrayLength(sound), dialfreq, lsb);
}


#include "wsprd/nhash.h"

#define WSPRD_NHASH_CONSTANT 146

extern "C"
JNIEXPORT jint

JNICALL
Java_aq_metallists_loudbang_cutil_CJarInterface_WSPRNhash(JNIEnv *env, jclass clazz, jstring call) {
    const char *callsign = env->GetStringUTFChars(call, 0);
    int ret = nhash(callsign, strlen(callsign), WSPRD_NHASH_CONSTANT);
    env->ReleaseStringUTFChars(call, callsign);
    return (jint)
            ret;
}


extern "C"
JNIEXPORT jstring JNICALL
Java_aq_metallists_loudbang_cutil_CJarInterface_WSPRLatLonToGSQ(JNIEnv *env, jclass clazz,
                                                                jdouble lon, jdouble lat) {
    if (isnan(lat) || isnan(lon)) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Latitude or longitude is NaN!");
        return NULL;
    }

    if (abs(lat) >= 90) {
        env->ThrowNew(env->FindClass("java/lang/Exception"),
                      "Latitude is >= +-90 deg. Grid sq. doesn't work on poles.");
        return NULL;
    }


    if (lon < -180)
        lon += 360;

    if (lon > 180)
        lon -= 360;

    double ycalc[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    double yn[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    double ydiv_arr[] = {10, 1, 0.04166666, 1 / 240, 1 / 240 / 24};
    double ydiv = 0, yres = 0, ylp = 0;

    ycalc[0] = (lat + 180) / 2;
    ycalc[1] = lon + 90;

    for (int yi = 0; yi < 2; yi++) {
        for (int yk = 0; yk < 5; yk++) {
            ydiv = ydiv_arr[yk];
            yres = ycalc[yi] / ydiv;

            ycalc[yi] = yres;

            if (yres > 0)
                ylp = floor(yres);
            else
                ylp = ceil(yres);

            ycalc[yi] = (ycalc[yi] - ylp) * ydiv;
            yn[2 * yk + yi] = ylp;
        }
    }


    char result[7];
    result[0] = (char) (yn[0] + 'A');
    result[1] = (char) (yn[1] + 'A');
    result[2] = (char) (yn[2] + '0');
    result[3] = (char) (yn[3] + '0');
    result[4] = (char) (yn[4] + 'a');
    result[5] = (char) (yn[5] + 'a');
    result[6] = 0;

    return env->NewStringUTF(result);
}

