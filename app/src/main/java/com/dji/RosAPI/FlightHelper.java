package com.dji.RosAPI;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.dji.RosAPI.internal.ModuleVerificationUtil;
import com.dji.RosAPI.internal.controller.DJISampleApplication;

import dji.common.error.DJIError;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.ControlMode;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class FlightHelper {

    private static final java.lang.String TAG = FlightHelper.class.getName();
    private static final java.lang.String DEBUGFLYTAG = "DebugFly";
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


    public FlightHelper() {
        initFlightController();
    }


    void initFlightController() {

        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
            flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
            android.util.Log.d(TAG, "initFlightController, ready to fly");

            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull FlightControllerState flightControllerState) {
                    Attitude attitude = flightControllerState.getAttitude();

                    android.util.Log.d(DEBUGFLYTAG, "roll=" + attitude.roll);
                    android.util.Log.d(DEBUGFLYTAG, "pitc=" + attitude.pitch);
                    android.util.Log.d(DEBUGFLYTAG, "yaw==" + attitude.yaw);
                    android.util.Log.d(DEBUGFLYTAG, "velX=" + flightControllerState.getVelocityX());
                    android.util.Log.d(DEBUGFLYTAG, "velY=" + flightControllerState.getVelocityY());
                    android.util.Log.d(DEBUGFLYTAG, "velZ=" + flightControllerState.getVelocityZ());

                }
            });
        }

    }

    void setNovice(final CmdCallback cmdCallback) {

        flightController.setNoviceModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                    android.util.Log.d(TAG, "setNovice true successful");
                    cmdCallback.onCompleted(true);
                } else {
                    android.util.Log.e(TAG, error.getDescription());
                    cmdCallback.onCompleted(false);
                }

            }
        });

    }

    void initVirtualControl(final CmdCallback cmdCallback) {

        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                    android.util.Log.d(TAG, "setVirtualStickModeEnabled true successful");

                    flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);//set flight relative to body frame

                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);//set to m/s
                    flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);//set to degrees/s

//                    flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);//set to degrees

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

    void cancelTakeOff(final CmdCallback cmdCallback) {
        flightController.cancelTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    android.util.Log.d(TAG, "cancelTakeOff Succeeded");
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


    void hoverProcedureAdvanced() {

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

        final CmdCallback cancelTakeOffCallback = new CmdCallback() {//define next step after cancelTakeOff
            @Override
            public void onCompleted(boolean success) {
                if (success) {

                    final Handler handler = new Handler();

                    handler.postDelayed(new RunnableLoop(200) {//10s of hovering 200*50 == 10000 milliseconds
                        @Override
                        public void run() {
                            android.util.Log.d(TAG, "this.index=" + this.index);
                            if (this.index == 0) {
                                startLanding(startLandingCallback);
                            } else {
                                sendCommand(sendCommandCallback, 0.00f, 0.00f, 0.00f, 0.25f);
//                                sendCommand(sendCommandCallback, -0.03f, -0.11f, 0.00f, 2.0f);//+x right of me, -y towards me
                                this.index--;
                                handler.postDelayed(this, 50);// virtual stick must receive a command every (between 5 Hz to 25 Hz )
                            }
                        }
                    }, 50);

                } else {
                    android.util.Log.e(TAG, "an error has occurred in cancelTakeOff");
                }
            }
        };

        final CmdCallback startTakeOffCallback = new CmdCallback() {//define next step after startTakeOff
            @Override
            public void onCompleted(boolean success) {
                if (success) {

                    //a trick is used here to avoid wasting time ascending
                    (new Handler()).postDelayed(new Runnable() {//wait takeoff
                        @Override
                        public void run() {
                            cancelTakeOff(cancelTakeOffCallback);
                        }
                    }, 3000);//wait takeoff

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