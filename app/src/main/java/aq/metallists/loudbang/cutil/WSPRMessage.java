package aq.metallists.loudbang.cutil;

public class WSPRMessage {
    public String out;
    float snr;
    float dt;
    float drift;
    double freq;
    String message;
    String call;
    int callhash;
    String loc;
    int power;

    public WSPRMessage(float snr, double freq, float dt, float drift, String message) {
        this.snr = snr;
        this.freq = freq;
        this.message = message;
        this.dt = dt;
        this.drift = drift;

        out = "".concat(Float.toString(snr)).concat(" -> ").concat(Double.toString(freq)).concat(" -> ").concat(message);
    }

    public float getSNR() {
        return snr;
    }

    public double getFREQ() {
        return freq;
    }

    public String getMSG() {
        return message;
    }

    public float getDT() {
        return this.dt;
    }

    public float getDRIFT() {
        return this.drift;
    }
}
