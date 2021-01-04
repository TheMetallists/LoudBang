#ifndef WSPR_ENC_H
#define WSPR_ENC_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * WSPR message encoder
 *      Make sure each parameter has its proper length.
 *      As an MCU program, waste treatments are not so much.
 *
 * Parameters
 *    call      Pointer to a callsign. A compound callsign makes the message to
 *              be encoded in type 2.
 *    grid      Pointer to a grid locator. A 6-character grid locator makes the
 *              message to be encoded in type 3.
 *     dBm      Pointer to a TX power level with ranging "0" to "60".
 * symbols      Output pointer to an array that holding 162 channel symbols.
 *
 * Returns      Message type used in the encoding, ranging is 1 - 3.
 */
uint8_t wspr_enc(const char *call, const char *grid, const char *dBm,
                 uint8_t *symbols);

uint8_t LB_WSPR_Encode2symbolz(uint8_t *symbols, const char *call, const char *grid, const char *dBm);

#ifdef __cplusplus
}
#endif

#endif // WSPR_ENC_H
