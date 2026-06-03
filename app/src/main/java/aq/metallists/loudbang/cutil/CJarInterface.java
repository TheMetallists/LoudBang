package aq.metallists.loudbang.cutil;

public class CJarInterface {
    static {
        System.loadLibrary("fftw3f");
        System.loadLibrary("QuietScream");
    }

    //public static short DEFAULT_VOLUME = 16383;
    public static short DEFAULT_VOLUME = 32767;

    public static native byte[] GenTestBeep(short volume, int offset);

    public static native byte[] WSPREncodeToPCM(String callsign, String locator,short volume, int power, int offset, boolean lsb);

    public static native WSPRMessage[] WSPRDecodeFromPcm(byte[] sound, double dialfreq, boolean lsb);

    public static native int WSPRNhash(String call);

    public static native double WSPRGetDistanceBetweenLocators(String a, String b);

    public static native String WSPRLatLonToGSQ(double lat, double lon);

    public static native int radioCheck(int testvar);

    public static native int getVolume(byte[] sound);
}
