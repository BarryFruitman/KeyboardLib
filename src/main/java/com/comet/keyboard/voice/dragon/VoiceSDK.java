package com.comet.keyboard.voice.dragon;

import android.content.Context;

import com.comet.keyboard.R;
import com.nuance.nmdp.speechkit.Prompt;
import com.nuance.nmdp.speechkit.SpeechKit;

public class VoiceSDK
{
    /**
     * The login parameters should be specified in the following manner:
     * 
     * public static final String SpeechKitServer = "ndev.server.name";
     * 
     * public static final int SpeechKitPort = 1000;
     * 
     * public static final String SpeechKitAppId = "ExampleSpeechKitSampleID";
     * 
     * public static final byte[] SpeechKitApplicationKey =
     * {
     *     (byte)0x38, (byte)0x32, (byte)0x0e, (byte)0x46, (byte)0x4e, (byte)0x46, (byte)0x12, (byte)0x5c, (byte)0x50, (byte)0x1d,
     *     (byte)0x4a, (byte)0x39, (byte)0x4f, (byte)0x12, (byte)0x48, (byte)0x53, (byte)0x3e, (byte)0x5b, (byte)0x31, (byte)0x22,
     *     (byte)0x5d, (byte)0x4b, (byte)0x22, (byte)0x09, (byte)0x13, (byte)0x46, (byte)0x61, (byte)0x19, (byte)0x1f, (byte)0x2d,
     *     (byte)0x13, (byte)0x47, (byte)0x3d, (byte)0x58, (byte)0x30, (byte)0x29, (byte)0x56, (byte)0x04, (byte)0x20, (byte)0x33,
     *     (byte)0x27, (byte)0x0f, (byte)0x57, (byte)0x45, (byte)0x61, (byte)0x5f, (byte)0x25, (byte)0x0d, (byte)0x48, (byte)0x21,
     *     (byte)0x2a, (byte)0x62, (byte)0x46, (byte)0x64, (byte)0x54, (byte)0x4a, (byte)0x10, (byte)0x36, (byte)0x4f, (byte)0x64
     * };
     * 
     * Please note that all the specified values are non-functional
     * and are provided solely as an illustrative example.
     * 
     */

    /* Please contact Nuance to receive the necessary connection and login parameters */
    public static final String SpeechKitServer = "sandbox.nmdp.nuancemobility.net";

    public static final int SpeechKitPort = 443;

    public static final boolean SpeechKitSsl = false;

    public static final String SpeechKitAppId = "NMDPTRIAL_cometinc20120809135110";

    public static final byte[] SpeechKitApplicationKey = {
    	(byte)0xe4, (byte)0x8c, (byte)0x37, (byte)0xcf, (byte)0x5a, (byte)0xd5,	(byte)0x79, (byte)0x4e, (byte)0xb0, (byte)0x1e, 
    	(byte)0x85, (byte)0x2a, (byte)0x42, (byte)0x90, (byte)0x49, (byte)0x72, (byte)0xe2, (byte)0x46, (byte)0x09, (byte)0x56, 
    	(byte)0x46, (byte)0x9c, (byte)0xd2, (byte)0x55, (byte)0x25, (byte)0x3f, (byte)0x1c, (byte)0x59, (byte)0x5e, (byte)0xab, 
    	(byte)0x61, (byte)0x42, (byte)0x37, (byte)0x98, (byte)0x12, (byte)0x92, (byte)0x6d, (byte)0x8b, (byte)0xe0, (byte)0x1e, 
    	(byte)0xa9, (byte)0x65, (byte)0x48, (byte)0xec, (byte)0xf5, (byte)0x91, (byte)0x5e, (byte)0xd8, (byte)0xd8, (byte)0x67, 
    	(byte)0x2e, (byte)0x1e, (byte)0x03, (byte)0x00, (byte)0x73, (byte)0x29, (byte)0x3f, (byte)0x8f, (byte)0x82, (byte)0x9e, 
    	(byte)0x3b, (byte)0xe3, (byte)0xe0, (byte)0x72};


    static SpeechKit mSpeechKit = null;
    public static SpeechKit getSpeechKit(Context context) {
    	if(mSpeechKit == null) {
    		mSpeechKit = SpeechKit.initialize(context.getApplicationContext(), VoiceSDK.SpeechKitAppId, VoiceSDK.SpeechKitServer, VoiceSDK.SpeechKitPort, VoiceSDK.SpeechKitSsl, VoiceSDK.SpeechKitApplicationKey);
    		mSpeechKit.connect();
    		// TODO: Keep an eye out for audio prompts not working on the Droid 2 or other 2.2 devices.
    		Prompt beep = mSpeechKit.defineAudioPrompt(R.raw.voice_prompt);
    		mSpeechKit.setDefaultRecognizerPrompts(beep, Prompt.vibration(100), null, null);
    	}

    	return mSpeechKit;
    }
    
    
    
    
    
}
