package com.dji.RosAPI;

import com.dji.RosAPI.internal.controller.DJISampleApplication;

import org.ros.node.topic.Publisher;

import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.util.CommonCallbacks;
import dji.sdk.products.Aircraft;
import std_msgs.String;

public class GimbalHelper {
    private static final java.lang.String TAG = GimbalHelper.class.getName();
    dji.sdk.gimbal.Gimbal gimbal = null;
    Publisher<String> publisherFlightLog = null;
    Publisher<String> publisherCmdFlightDebug = null;
    public GimbalHelper.CmdCallback cmdCallbackDefault = null;

    interface CmdCallback {
        void onCompleted(boolean success);
    }

    public GimbalHelper() {
        initGimbal();
    }

    public GimbalHelper(Publisher<String> pub, Publisher<String> pubDebug) {
        publisherFlightLog = pub;
        publisherCmdFlightDebug = pubDebug;
        initGimbal();
    }

    void pubMessage(Publisher<String> pub, java.lang.String msg) {
        if (pub != null) {
            std_msgs.String str = pub.newMessage();
            str.setData(msg);
            pub.publish(str);
        }
    }

    void initGimbal() {
        gimbal = ((Aircraft) DJISampleApplication.getProductInstance()).getGimbal();

        cmdCallbackDefault = new GimbalHelper.CmdCallback() {
            @Override
            public void onCompleted(boolean success) {
                if (!success) {
                    android.util.Log.e(TAG, "an error has occurred on cmdCallbackDefault");
                    pubMessage(publisherCmdFlightDebug, "an error has occurred on cmdCallbackDefault");
                }
            }
        };
    }

    void rotate(final GimbalHelper.CmdCallback cmdCallback, float pitchValue, double timeValue) {

//        RELATIVE_ANGLE	The angle value, when the gimbal is rotating, relative to the current angle.
//        ABSOLUTE_ANGLE	The angle value, when the gimbal is rotating, relative to 0 degrees (aircraft heading).
//        SPEED	            Rotate the gimbal's pitch, roll, and yaw in SPEED Mode. The direction can either be set to clockwise or counter-clockwise.
//                          For Phantom 3 Professional, Phantom 3 Advanced and Phantom 3 Standard, roll and yaw rotations are not available.
//                          For Inspire 1, Inspire Pro and M100, pitch, roll and yaw rotations are available.
//                          For Osmo, roll rotation is not available. The yaw angleVelocity of DJIGimbalSpeedRotation range is (-120, 120).


//        Sets the completion time in seconds to complete an action to control the gimbal.
//        If the the rotation mode is ABSOLUTE_ANGLE then the time determines the duration of time the gimbal should rotate to its new position.
//        For example, if a value of 2.0 is used, then the gimbal will rotate to its target position in 2.0 seconds. Range is [0.1,25.5] seconds.

        Rotation.Builder builder = new Rotation.Builder().mode(dji.common.gimbal.RotationMode.ABSOLUTE_ANGLE).pitch(pitchValue).yaw(Rotation.NO_ROTATION).roll(Rotation.NO_ROTATION).time(timeValue);
        Rotation rotation = builder.build();

        gimbal.rotate(rotation, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    android.util.Log.d(TAG, "rotate Succeeded");
                    pubMessage(publisherCmdFlightDebug, "rotate Succeeded");
                    cmdCallback.onCompleted(true);
                } else {
                    android.util.Log.e(TAG, error.getDescription());
                    pubMessage(publisherCmdFlightDebug, error.getDescription());
                    cmdCallback.onCompleted(false);
                }
            }
        });
    }
}
