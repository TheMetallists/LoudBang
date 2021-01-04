/*
 * This program tests a prototype of the WSPR encoder "wspr_enc" on PC. Check
 * wspr_enc.h and wspr_enc.c for more details.
 *
 * The WSPR coding simulation tool "WSPRcode" is required to verify the symbols
 * produced by the encoder. It can be built from the WSJT-X source code, or can
 * be downloaded from a link bellow.
 *
 * Visit the WSPRcode source at:
 *      https://sourceforge.net/p/wsjt/wsjtx/ci/master/tree/lib/wsprcode/
 *
 * Download a WSPRcode Windows executable from: (works on Windows 2000 and newer)
 *      https://www.physics.princeton.edu/pulsar/K1JT/WSPRcode.exe
 *
 * References:
 * K1JT: WSPR 2.0 User’s Guide
 *      https://www.physics.princeton.edu/pulsar/K1JT/WSPR_2.0_User.pdf
 *
 * Author: BD1ES
 * Date modified: 21 FEB 2020
 */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>
#include <getopt.h>

#include "wenc.h"


static void print_help(const char *pname) {
    printf("WSPR encoder function \"wspr_enc\" test tool.\n");
    printf("Usage: %s [options] <callsign> <grid> <dBm>\n", pname);
    printf("Examples: %s BD1XYZ OM89 10         (message type 1)\n", pname);
    printf("          %s BD1XYZ/M xxxx 20       (message type 2)\n", pname);
    printf("          %s BD1XYZ OM89dw 30       (message type 3)\n", pname);
    printf("Options:\n");
    printf("    -h --help\n");
    printf("       Display this help screen.\n");
    printf("    --gen_tables\n");
    printf("       Generate constant lookup tables.\n");
    printf("Parameters:\n");
    printf("    callsign: BD1XYZ BD1XYZ/9 EA8/BD1XYZ ...\n");
    printf("        where, compound calls produce type 2 messages.\n");
    printf("    grid: 4- or 6-character grid locator\n");
    printf("        where, 6-character locators produce type 3 messages.\n");
    printf("    dBm: TX power in dBm, ranging 0 - 60.\n");
    printf("\n");
    printf("This program uses WSPRcode to generate the references. ");
    printf("Make sure it's installed in the system and can be invoked.\n");
}

/* run this program using the console pauser or add your own getch, system("pause") or input loop */

uint8_t LB_WSPR_Encode2symbolz(uint8_t *symbols, const char *call, const char *grid, const char *dBm) {

    uint8_t msgtype = wspr_enc(call, grid, dBm, symbols);

    return msgtype;
}

