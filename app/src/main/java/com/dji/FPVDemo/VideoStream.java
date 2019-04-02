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
    FlightHelper flightHelper = null;

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

        flightHelper = new FlightHelper();

        subscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {

                log.info("I heard: \"" + message.getData() + "\"");
                if (message.getData().equals("hover")) {
                    flightHelper.hoverProcedure();
                }
            }
        });

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