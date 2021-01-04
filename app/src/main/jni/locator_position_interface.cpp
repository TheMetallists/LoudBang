#include "jni_link.h"
#include <iostream>
#include "lbenc2/wenc.h"
#include <android/log.h>
#include <stdio.h>
#include <math.h>

class LocatorPosnInterface {
public:
    double lata, lona, latb, lonb;
    char *loca, *locb;

    LocatorPosnInterface(const char *ina, const char *inb) {
        this->lata = -90.0;
        this->lona = -180.0;
        this->latb = -90.0;
        this->lonb = -180;
        this->loca = (char *) malloc(11);
        memset(this->loca, 0, 11);
        this->locb = (char *) malloc(11);
        memset(this->locb, 0, 11);


        strcpy(this->loca, ina);
        strcpy(this->locb, inb);


        while ((strlen(this->loca) / 2) < 5) {
            if (((strlen(this->loca) / 2) % 2) == 1) {
                this->loca[strlen(this->loca)] = '5';
                this->loca[strlen(this->loca)] = '5';
            } else {
                this->loca[strlen(this->loca)] = 'L';
                this->loca[strlen(this->loca)] = 'L';
            }
        }

        while ((strlen(this->locb) / 2) < 5) {
            if (((strlen(this->locb) / 2) % 2) == 1) {
                this->locb[strlen(this->locb)] = '5';
                this->locb[strlen(this->locb)] = '5';
            } else {
                this->locb[strlen(this->locb)] = 'L';
                this->locb[strlen(this->locb)] = 'L';
            }
        }


        this->extractLocatorPartA(0, 1);
        this->extractLocatorPartA(1, 10);
        this->extractLocatorPartA(2, 10 * 24);
        this->extractLocatorPartA(3, 10 * 24 * 10);
        this->extractLocatorPartA(4, 10 * 24 * 10 * 24);


        this->extractLocatorPartB(0, 1);
        this->extractLocatorPartB(1, 10);
        this->extractLocatorPartB(2, 10 * 24);
        this->extractLocatorPartB(3, 10 * 24 * 10);
        this->extractLocatorPartB(4, 10 * 24 * 10 * 24);


    }

    double getDistance() {
        double theta, dist;

        if ((this->lata == this->latb) && (this->lona == this->lonb)) {
            return 0;
        } else {
            theta = this->lona - this->lonb;
            dist = sin(deg2rad(this->lata)) * sin(deg2rad(this->latb)) +
                   cos(deg2rad(this->lata)) * cos(deg2rad(this->latb)) * cos(deg2rad(theta));
            dist = acos(dist);
            dist = this->rad2deg(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1.609344;
            return dist;
        }
    }

    ~LocatorPosnInterface() {
        free(this->loca);
        free(this->locb);
    }

private:
    void extractLocatorPartA(int counter, double divisor) {
        char grid_lon = this->loca[counter * 2];
        char grid_lat = this->loca[counter * 2 + 1];

        int llat, llon;

        if ('0' <= grid_lat && grid_lat <= '9') {
            llat = grid_lat - '0';
        } else if ('a' <= grid_lat && grid_lat <= 'z') {
            llat = grid_lat - 'a';
        } else if ('A' <= grid_lat && grid_lat <= 'Z') {
            llat = grid_lat - 'A';
        }

        if ('0' <= grid_lon && grid_lon <= '9') {
            llon = grid_lon - '0';
        } else if ('a' <= grid_lon && grid_lon <= 'z') {
            llon = grid_lon - 'a';
        } else if ('A' <= grid_lon && grid_lon <= 'Z') {
            llon = grid_lon - 'A';
        }

        this->lata += llat * 10.0 / divisor;
        this->lona += llon * 20.0 / divisor;

    }

    void extractLocatorPartB(int counter, double divisor) {
        char grid_lon = this->locb[counter * 2];
        char grid_lat = this->locb[counter * 2 + 1];

        int llat, llon;

        if ('0' <= grid_lat && grid_lat <= '9') {
            llat = grid_lat - '0';
        } else if ('a' <= grid_lat && grid_lat <= 'z') {
            llat = grid_lat - 'a';
        } else if ('A' <= grid_lat && grid_lat <= 'Z') {
            llat = grid_lat - 'A';
        }

        if ('0' <= grid_lon && grid_lon <= '9') {
            llon = grid_lon - '0';
        } else if ('a' <= grid_lon && grid_lon <= 'z') {
            llon = grid_lon - 'a';
        } else if ('A' <= grid_lon && grid_lon <= 'Z') {
            llon = grid_lon - 'A';
        }

        this->latb += llat * 10.0 / divisor;
        this->lonb += llon * 20.0 / divisor;

    }


    double deg2rad(double deg) {
        return (deg * M_PI / 180);
    }


    double rad2deg(double rad) {
        return (rad * 180 / M_PI);
    }

};


extern "C"
JNIEXPORT jdouble JNICALL
Java_aq_metallists_loudbang_cutil_CJarInterface_WSPRGetDistanceBetweenLocators(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jstring a,
                                                                               jstring b) {
    const char *j_a = env->GetStringUTFChars(a, 0);
    const char *j_b = env->GetStringUTFChars(b, 0);

    if (strlen(j_a) == 0 || strlen(j_b) == 0) {
        env->ReleaseStringUTFChars(a, j_a);
        env->ReleaseStringUTFChars(b, j_b);
        return (jdouble) -1;
    }

    LocatorPosnInterface *lpi = new LocatorPosnInterface(j_a, j_b);

    env->ReleaseStringUTFChars(a, j_a);
    env->ReleaseStringUTFChars(b, j_b);

    jdouble ret = (jdouble) lpi->getDistance();
    delete lpi;

    return ret;
}
