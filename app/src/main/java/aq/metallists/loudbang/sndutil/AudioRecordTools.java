package aq.metallists.loudbang.sndutil;

import static aq.metallists.loudbang.sndutil.CommonAudioTools.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import aq.metallists.loudbang.LBService;
import aq.metallists.loudbang.R;

public class AudioRecordTools {
    public static final int TYPE_ECHO_REFERENCE_OVERRIDE = 28;

    public static String getAudioInputNameByType(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return "TYPE_UNKNOWN";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "TYPE_BUILTIN_EARPIECE";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "TYPE_BUILTIN_SPEAKER";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "TYPE_WIRED_HEADSET";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "TYPE_WIRED_HEADPHONES";
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "TYPE_LINE_ANALOG";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "TYPE_LINE_DIGITAL";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "TYPE_BLUETOOTH_SCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "TYPE_BLUETOOTH_A2DP";
            case AudioDeviceInfo.TYPE_HDMI:
                return "TYPE_HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return "TYPE_HDMI_ARC";
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "TYPE_USB_DEVICE";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return "TYPE_USB_ACCESSORY";
            case AudioDeviceInfo.TYPE_DOCK:
                return "TYPE_DOCK";
            case AudioDeviceInfo.TYPE_FM:
                return "TYPE_FM";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "TYPE_BUILTIN_MIC";
            case AudioDeviceInfo.TYPE_FM_TUNER:
                return "TYPE_FM_TUNER";
            case AudioDeviceInfo.TYPE_TV_TUNER:
                return "TYPE_TV_TUNER";
            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "TYPE_TELEPHONY";
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return "TYPE_AUX_LINE";
            case AudioDeviceInfo.TYPE_IP:
                return "TYPE_IP";
            case AudioDeviceInfo.TYPE_BUS:
                return "TYPE_BUS";
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "TYPE_USB_HEADSET";
            case AudioDeviceInfo.TYPE_HEARING_AID:
                return "TYPE_HEARING_AID";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE:
                return "TYPE_BUILTIN_SPEAKER_SAFE";
            case AudioDeviceInfo.TYPE_REMOTE_SUBMIX:
                return "TYPE_REMOTE_SUBMIX";
            case AudioDeviceInfo.TYPE_BLE_HEADSET:
                return "TYPE_BLE_HEADSET";
            case AudioDeviceInfo.TYPE_BLE_SPEAKER:
                return "TYPE_BLE_SPEAKER";
            case TYPE_ECHO_REFERENCE_OVERRIDE:
                return "TYPE_ECHO_REFERENCE";
            case AudioDeviceInfo.TYPE_HDMI_EARC:
                return "TYPE_HDMI_EARC";
            case AudioDeviceInfo.TYPE_BLE_BROADCAST:
                return "TYPE_BLE_BROADCAST";
            case AudioDeviceInfo.TYPE_DOCK_ANALOG:
                return "TYPE_DOCK_ANALOG";
            default:
                return String.format(Locale.GERMAN, "Unknown (%d)", type);
        }
    }

    @SuppressLint("NewApi")
    public static AudioRecord getAudioRecord(Context ctx, Object mprSrc) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw new Error("You have no permission to record audio.");
        }

        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        AudioRecord ar = null;
        AudioDeviceInfo tgtDev = null;
        boolean isBluetooth = false;


        if (mprSrc == null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
            int sourceId = -1;
            try {
                sourceId = sp.getInt("rx_deviceid", -1);
            } catch (Exception x) {
                Toast.makeText(ctx, R.string.sndinput_device_invalid, Toast.LENGTH_LONG).show();
                sp.edit().putInt("rx_deviceid", -1).apply();
            }

            boolean blueMode = sp.getBoolean("snd_bluemode", false);

            if (sourceId > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioDeviceInfo[] inputDevices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);

                    boolean hasRequestedDevice = false;
                    for (AudioDeviceInfo dev : inputDevices) {
                        if (dev.getId() == sourceId) {
                            if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                                Log.i("ART", "Starting BluetothSCO!");
                                /*
                                 * TODO:
                                 *  the stream type must be STREAM_VOICE_CALL
                                 * the format must be mono
                                 * the sampling must be 16kHz or 8kHz
                                 * */

                                isBluetooth = true;
                            }
                            tgtDev = dev;
                            hasRequestedDevice = true;
                        }
                    }

                    if (!hasRequestedDevice) {
                        //TODO: run on UI thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(ctx, R.string.sndinput_device_vanished, Toast.LENGTH_LONG).show();
                        });
                    }

                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ctx, R.string.sndinput_low_android_version2, Toast.LENGTH_LONG).show();
                    });
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioDeviceInfo[] inputDevices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);

                    for (AudioDeviceInfo dev : inputDevices) {
                        if (dev.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                            Log.i("ART", "Starting BluetothSCO!");
                            /*
                             * TODO:
                             *  the stream type must be STREAM_VOICE_CALL
                             * the format must be mono
                             * the sampling must be 16kHz or 8kHz
                             * */

                            isBluetooth = true;
                            tgtDev = dev;
                        }
                    }
                }
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            AudioRecord.Builder arb = new AudioRecord.Builder()
                    .setBufferSizeInBytes(BUFFER_SIZE)
                    .setAudioFormat(
                            new AudioFormat.Builder()
                                    .setEncoding(ENCODING)
                                    .setChannelMask(CHANNEL_MASK)
                                    .setSampleRate(SAMPLE_RATE)
                                    .build()
                    );

            if (isBluetooth) {
                Log.i("ART", "VOICE_COMMUNICATION");
                arb.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
            }
            if (mprSrc != null) {
                MediaProjection mpr = (MediaProjection) mprSrc;
                AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mpr)
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        /*
                        TODO: add settings entry
                        .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                        .addMatchingUsage(AudioAttributes.USAGE_GAME)
                        .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)*/
                        .build();
                arb.setAudioPlaybackCaptureConfig(config);
            }

            ar = arb.build();
        } else {
            // use boring constructor
            ar = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
        }

        if (isBluetooth) {
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
            am.setBluetoothScoOn(true);
            am.startBluetoothSco();
        }
        if (tgtDev != null) {
            Log.i("ART", "SPD");
            ar.setPreferredDevice(tgtDev);
        }

        return ar;
    }

    public static void validateAudioDeviceId(SharedPreferences sp, Context ctx) {
        int sourceId = -1;
        try {
            sourceId = sp.getInt("rx_deviceid", -1);
        } catch (Exception x) {
            Toast.makeText(ctx, R.string.sndinput_device_invalid, Toast.LENGTH_LONG).show();
            sp.edit().putInt("rx_deviceid", -1).apply();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] inputDevices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);

            boolean hasRequestedDevice = false;
            for (AudioDeviceInfo dev : inputDevices) {
                if (dev.getId() == sourceId) {
                    hasRequestedDevice = true;
                }
            }

            if (!hasRequestedDevice) {
                Toast.makeText(ctx, R.string.sndinput_device_vanished, Toast.LENGTH_LONG).show();
                sp.edit().putInt("rx_deviceid", -1).apply();
            }

        }

    }
}
