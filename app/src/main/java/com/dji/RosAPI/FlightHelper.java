package com.dji.RosAPI;

import android.os.Handler;

import com.dji.RosAPI.internal.ModuleVerificationUtil;
import com.dji.RosAPI.internal.controller.DJISampleApplication;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class FlightHelper {

    private static final java.lang.String TAG = FlightHelper.class.getName();
    FlightController flightController = null;

    interface CmdCallback {
        void onCompleted(boolean success);
    }

    public class RunnableLoop implements Runnable {
        public int index = 0;

        public RunnableLoop(int _index) {
            this.index = _index;
        }

        @Override
        public void run() {
        }
    }

    public FlightHelper(){
        initFlightController();
    }

    void initFlightController() {

        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
            android.util.Log.d(TAG, "initFlightController, ready to fly");
        }

    }

    void initVirtualControl(final CmdCallback cmdCallback) {

        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                    android.util.Log.d(TAG, "setVirtualStickModeEnabled true successful");
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);//set to m/s
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);//set to m/s
                    flightController.setVerticalControlMode(VerticalControlMode.POSITION);//set m to ground
                    cmdCallback.onCompleted(true);
                } else {
                    android.util.Log.e(TAG, error.getDescription());
                    cmdCallback.onCompleted(false);
                }

            }
        });

    }

    void startTakeOff(final CmdCallback cmdCallback) {
        flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    android.util.Log.d(TAG, "takeOff Succeeded");
                    cmdCallback.onCompleted(true);
                } else {
                    android.util.Log.e(TAG, error.getDescription());
                    cmdCallback.onCompleted(false);
                }
            }
        });
    }

    void startLanding(final CmdCallback cmdCallback) {
        flightController.startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    android.util.Log.d(TAG, "landing Succeeded");
                    cmdCallback.onCompleted(true);
                } else {
                    android.util.Log.e(TAG, error.getDescription());
                    cmdCallback.onCompleted(false);
                }
            }
        });
    }


    void sendCommand(final CmdCallback cmdCallback, final float roll, final float pitch, final float yaw, final float verticalThrottle) {

        if (flightController.isVirtualStickControlModeAvailable()) {
            FlightControlData controlData = new FlightControlData(roll, pitch, yaw, verticalThrottle);//pitch, roll, yaw, verticalThrottle
            flightController.sendVirtualStickFlightControlData(controlData, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        android.util.Log.d(TAG, "command sent");
                        cmdCallback.onCompleted(true);
                    } else {
                        android.util.Log.e(TAG, error.getDescription());
                        cmdCallback.onCompleted(false);
                    }
                }
            });
        }

    }

    void hoverProcedure() {

        if (flightController == null) {
            initFlightController();
        }

        final CmdCallback startLandingCallback = new CmdCallback() {//define next step after startLandingCallback
            @Override
            public void onCompleted(boolean success) {
                if (!success) {
                    android.util.Log.e(TAG, "an error has occurred in startLanding");
                }
            }
        };

        final CmdCallback sendCommandCallback = new CmdCallback() {//define next step after sendCommand
            @Override
            public void onCompleted(boolean success) {
                if (!success) {
                    android.util.Log.e(TAG, "an error has occurred in sendCommand");
                    android.util.Log.e(TAG, "starting emergency landing!!!");
                    startLanding(startLandingCallback);
                }
            }
        };

        final CmdCallback startTakeOffCallback = new CmdCallback() {//define next step after startTakeOff
            @Override
            public void onCompleted(boolean success) {
                if (success) {
                    //loop send command every x milliseconds

                    final Handler handler = new Handler();

                    handler.postDelayed(new RunnableLoop(40) {//10s of hovering 40*250 == 10000 milliseconds
                        @Override
                        public void run() {
                            android.util.Log.d(TAG, "this.index=" + this.index);
                            if (this.index == 0) {
                                startLanding(startLandingCallback);
                            } else {
                                sendCommand(sendCommandCallback, 0.0f, 0.0f, 0.0f, 0.5f);
                                this.index--;
                                handler.postDelayed(this, 250);// virtual stick must receive a command every (between 5 Hz to 25 Hz )
                            }
                        }
                    }, 250);


                } else {
                    android.util.Log.e(TAG, "an error has occurred in startTakeOff");
                }
            }
        };

        final CmdCallback initVirtualControlCallback = new CmdCallback() {//define next step after initVirtualControl
            @Override
            public void onCompleted(boolean success) {
                if (success) {
                    startTakeOff(startTakeOffCallback);
                } else {
                    android.util.Log.e(TAG, "an error has occurred in initVirtualControl");
                }
            }
        };

        initVirtualControl(initVirtualControlCallback);


    }


}
