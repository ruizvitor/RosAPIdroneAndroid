package com.dji.RosAPI;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;


import org.apache.commons.logging.Log;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;


import java.io.ByteArrayOutputStream;

import std_msgs.String;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

public class PublisherSubscriber extends AbstractNodeMain {

    private static final java.lang.String TAG = PublisherSubscriber.class.getName();

    ConnectedNode mynode;
//    Publisher<String> publisher;
    Publisher<String> publisherCmdFlightDebug;
    Publisher<String> publisherFlightLog;
    Publisher<sensor_msgs.CompressedImage> publisherImg;
    FlightHelper flightHelper = null;


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/publisherSubscriber");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mynode = connectedNode;

        final Log log = connectedNode.getLog();
        Subscriber<String> subscriber = connectedNode.newSubscriber("djiSDK/listener", std_msgs.String._TYPE);

        publisherFlightLog = connectedNode.newPublisher("djiSDK/flightLog", std_msgs.String._TYPE);
        publisherCmdFlightDebug = connectedNode.newPublisher("djiSDK/cmdFlightDebug", std_msgs.String._TYPE);
//        publisher = connectedNode.newPublisher("djiSDK/somethingElse", std_msgs.String._TYPE);
        publisherImg = connectedNode.newPublisher("camera/compressed", sensor_msgs.CompressedImage._TYPE);

        flightHelper = new FlightHelper(publisherFlightLog, publisherCmdFlightDebug);

        subscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {
                //IMPLEMENT Subscriber logic here

                java.lang.String[] args = message.getData().split(" ");

                if (args[0].equals("hover")) {//high level example of chain of commands
                    flightHelper.hoverProcedureAdvanced();
                }
                if (args[0].equals("initVirtualControl")) {
                    flightHelper.initVirtualControl(flightHelper.cmdCallbackDefault);//implement your own cmdCallback for more advanced op
                }
                if (args[0].equals("startTakeOff")) {
                    flightHelper.startTakeOff(flightHelper.cmdCallbackDefault);//implement your own cmdCallback for more advanced op
                }
                if (args[0].equals("cancelTakeOff")) {
                    flightHelper.cancelTakeOff(flightHelper.cmdCallbackDefault);//implement your own cmdCallback for more advanced op
                }
                if (args[0].equals("sendCommand")) {
                    if (args.length == 5) {
                        flightHelper.sendCommand(flightHelper.cmdCallbackDefault, Float.parseFloat(args[1]), Float.parseFloat(args[2]), Float.parseFloat(args[3]), Float.parseFloat(args[4]));//implement your own cmdCallback for more advanced op
                    } else {
                        pubMessageGeneric(publisherCmdFlightDebug, "sendCommand does not have enough arguments");
                    }
                }
                if (args[0].equals("startLanding")) {
                    flightHelper.startLanding(flightHelper.cmdCallbackDefault);//implement your own cmdCallback for more advanced op
                }
                //END OF IMPLEMENT Subscriber logic
            }
        });

    }

    public void pubMessageGeneric(Publisher<String> pub, java.lang.String msg) {
        if (pub != null) {
            std_msgs.String str = pub.newMessage();
            str.setData(msg);
            pub.publish(str);
        }
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
                            90,//quality
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