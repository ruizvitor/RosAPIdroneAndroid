package com.dji.FPVDemo;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.os.Handler;


import com.dji.FPVDemo.internal.ModuleVerificationUtil;
import com.dji.FPVDemo.internal.controller.DJISampleApplication;

import org.apache.commons.logging.Log;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;


import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;

import dji.common.error.DJIError;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.flightcontroller.imu.IMUState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.sdk.flightcontroller.Compass;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import std_msgs.String;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

public class VideoStream extends AbstractNodeMain {

    private static final java.lang.String TAG = VideoStream.class.getName();

    ConnectedNode mynode;
    Publisher<String> publisher;
    Publisher<sensor_msgs.CompressedImage> publisherImg;
    Compass compass = null;
    FlightController flightController;
    FlightControllerState flightControllerState;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/videoStream");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mynode = connectedNode;

        final Log log = connectedNode.getLog();
        Subscriber<String> subscriber = connectedNode.newSubscriber("djiSDK/listener", std_msgs.String._TYPE);

        publisher = connectedNode.newPublisher("djiSDK/flightLog", std_msgs.String._TYPE);
        publisherImg = connectedNode.newPublisher("camera/compressed", sensor_msgs.CompressedImage._TYPE);

        initVirtualControl(false);


//        connectedNode.executeCancellableLoop(new CancellableLoop() {
//            private int sequenceNumber;
//
//            @Override
//            protected void setup() {
//                sequenceNumber = 0;
//            }
//
//            @Override
//            protected void loop() throws InterruptedException {
////                std_msgs.String str = publisher.newMessage();
////                str.setData("Hello world! " + sequenceNumber);
////                publisher.publish(str);
////                sequenceNumber++;
//
////                flightControllerState.setVelocityX(0.0f);
////                flightControllerState.setVelocityY(0.0f);
//
//
//
//                flightControllerState = flightController.getState();
//
//                Attitude att = flightControllerState.getAttitude();
//
//                LocationCoordinate3D loc = flightControllerState.getAircraftLocation();
//                LocationCoordinate2D homeloc = flightControllerState.getHomeLocation();
//
////                android.util.Log.d(TAG,"loc.lat="+loc.getLatitude());
////                android.util.Log.d(TAG,"loc.lon="+loc.getLongitude());
////                android.util.Log.d(TAG,"loc.alt="+loc.getAltitude());
////
////
////                android.util.Log.d(TAG,"homeloc.lat="+homeloc.getLatitude());
////                android.util.Log.d(TAG,"homeloc.lon="+homeloc.getLongitude());
////
////                android.util.Log.d(TAG, "att.pitch:" + att.pitch);
////                android.util.Log.d(TAG, "att.yaw:" + att.yaw);
////                android.util.Log.d(TAG, "att.roll:" + att.roll);
////
////                android.util.Log.d(TAG, "getTakeoffLocationAltitude:" + flightControllerState.getTakeoffLocationAltitude());
////
////                android.util.Log.d(TAG, "velx:" + flightControllerState.getVelocityX());
////                android.util.Log.d(TAG, "vely:" + flightControllerState.getVelocityY());
////                android.util.Log.d(TAG, "velz:" + flightControllerState.getVelocityZ());
////
////                android.util.Log.d(TAG, "north:" + getCompassHeading());
//
//                java.lang.String logmsg = "\n";
//                logmsg = logmsg+"loc.lat="+loc.getLatitude()+"\n";
//                logmsg = logmsg+"loc.lon="+loc.getLongitude()+"\n";
//                logmsg = logmsg+"loc.alt="+loc.getAltitude()+"\n";
//                logmsg = logmsg+"homeloc.lat="+homeloc.getLatitude()+"\n";
//                logmsg = logmsg+"homeloc.lon="+homeloc.getLongitude()+"\n";
//                logmsg = logmsg+"att.pitch:" + att.pitch+"\n";
//                logmsg = logmsg+"att.yaw:" + att.yaw+"\n";
//                logmsg = logmsg+"att.roll:" + att.roll+"\n";
//                logmsg = logmsg+"getTakeoffLocationAltitude:" + flightControllerState.getTakeoffLocationAltitude()+"\n";
//                logmsg = logmsg+"velx:" + flightControllerState.getVelocityX()+"\n";
//                logmsg = logmsg+"vely:" + flightControllerState.getVelocityY()+"\n";
//                logmsg = logmsg+"velz:" + flightControllerState.getVelocityZ()+"\n";
//                logmsg = logmsg+"north:" + getCompassHeading()+"\n";
//
//
//                pubMessage(logmsg);
//                Thread.sleep(100);
//            }
//        });


        subscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {

                log.info("I heard: \"" + message.getData() + "\"");
                if (message.getData().equals("test")) {
                    pubMessage("Starting Flight");
                    testFlight();
                }
                if (message.getData().equals("hover")) {
                    initVirtualControl(true);
//                    pubMessage("Starting hover");
                }
            }
        });

    }

    private Runnable mStopMotor = new Runnable() {
        @Override
        public void run() {
            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        android.util.Log.d(TAG, "Landing Succeeded");
                        pubMessage("Starting Landing");

                    } else {
                        android.util.Log.e(TAG, error.getDescription());
                    }

                }
            });
        }
    };


    private Runnable mMotorCommand = new Runnable() {
        @Override
        public void run() {
            sendCommand(0.0f, 0.0f, 0.0f, 0.5f, 30);
        }
    };

    void initVirtualControl(boolean state) {
        if (state == true) {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
                compass = flightController.getCompass();

                android.util.Log.d(TAG, "about to fly");

                flightControllerState = flightController.getState();

                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {

                        if (error == null) {
                            android.util.Log.d(TAG, "setVirtualStickModeEnabled true successful");
                            pubMessage("setVirtualStickModeEnabled true successful");
                            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);//set to m/s
                            flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);//set to m/s
                            flightController.setVerticalControlMode(VerticalControlMode.POSITION);//set m to ground

                            hoverProcedure();

                        } else {
                            android.util.Log.e(TAG, error.getDescription());
                        }

                    }
                });
            }
        } else {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
                compass = flightController.getCompass();

                android.util.Log.d(TAG, "about to fly");

                flightControllerState = flightController.getState();
            }
        }

    }

    void hoverProcedure() {

        flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                    android.util.Log.d(TAG, "takeOff Succeeded");

                    (new Handler()).postDelayed(mMotorCommand, 5000);//

                } else {
                    android.util.Log.e(TAG, error.getDescription());
                }

            }
        });


    }

    void sendCommand(final float roll, final float pitch, final float yaw, final float verticalThrottle, final int iter) {
        android.util.Log.d(TAG, "iter:" + iter);
        if (iter == 0) {
            //start landing
            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        android.util.Log.d(TAG, "Landing Succeeded");
                        pubMessage("Starting Landing");

                    } else {
                        android.util.Log.e(TAG, error.getDescription());
                    }

                }
            });

        } else {
            if (flightController.isVirtualStickControlModeAvailable()) {

                FlightControlData controlData = new FlightControlData(roll, pitch, yaw, verticalThrottle);//pitch, roll, yaw, verticalThrottle
                flightController.sendVirtualStickFlightControlData(controlData, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {

                        if (error == null) {
                            android.util.Log.d(TAG, "command sent");
//                            (new Handler()).postDelayed(mMotorCommand, 100);//

                            (new Handler()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendCommand(roll, pitch, yaw, verticalThrottle, iter - 1);
                                }
                            }, 250);

                        } else {
                            android.util.Log.e(TAG, error.getDescription());
                        }

                    }
                });
            }
        }
    }


//    void setIMUCallback() {
//
//        flightController.setIMUStateCallback(new IMUState.Callback() {
//
//            @Override
//            public void onUpdate(IMUState state) {
//
//                android.util.Log.d(TAG, "IMU ID:"+ state.getIndex());
//                android.util.Log.d(TAG, "Is connected:"+ state.isConnected());
//                android.util.Log.d(TAG, "AccelerometerState:"+ state.getAccelerometerState());
//                android.util.Log.d(TAG, "AccelerometerValue:"+ state.getAccelerometerValue());
//                android.util.Log.d(TAG, "getGyroscopeState:"+ state.getGyroscopeState());
//                android.util.Log.d(TAG, "getGyroscopeValue:"+ state.getGyroscopeValue());
//
//            }
//
//        });
//
//    }


    //    void initCompass() {
//        if (ModuleVerificationUtil.isFlightControllerAvailable()) {
//            flightController = ((Aircraft) DJISampleApplication.getProductInstance()).getFlightController();
//
//            if (ModuleVerificationUtil.isCompassAvailable()) {
//                compass = flightController.getCompass();
//            }
//        }
//    }
//
    void testFlight() {
        flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {

                if (error == null) {
                    android.util.Log.d(TAG, "takeOff Succeeded");

                    (new Handler()).postDelayed(mStopMotor, 5000);//


                } else {
                    android.util.Log.e(TAG, error.getDescription());
                }

            }
        });
    }

    float getCompassHeading() {
        if (compass != null)
            return compass.getHeading();
        else
            return 0;
    }

    public void pubMessage(java.lang.String msg) {
        std_msgs.String str = publisher.newMessage();
        str.setData(msg);
        publisher.publish(str);
    }


    public void publishImage(final byte[] buf, final int width, final int height) {
        if (publisherImg != null) {


            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    int ylen = width * height;

                    YuvImage yuvImage = new YuvImage(buf,
                            ImageFormat.NV21,
                            width,
                            height,
                            null);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    yuvImage.compressToJpeg(new Rect(0,
                                    0,
                                    width,
                                    height),
                            50,//quality
                            baos);

                    sensor_msgs.CompressedImage image = publisherImg.newMessage();

                    image.setFormat("jpeg");
                    image.getHeader().setStamp(mynode.getCurrentTime());
                    image.getHeader().setFrameId("camera");

                    ChannelBufferOutputStream stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
                    stream.buffer().writeBytes(baos.toByteArray());
                    image.setData(stream.buffer().copy());
                    stream.buffer().clear();

                    publisherImg.publish(image);
                }
            });

        }
    }
}