package com.dji.FPVDemo;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;


import org.apache.commons.logging.Log;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;


import java.io.ByteArrayOutputStream;
import java.nio.ByteOrder;

import std_msgs.String;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

public class VideoStream extends AbstractNodeMain {

    private static final java.lang.String TAG = VideoStream.class.getName();

    ConnectedNode mynode;
    Publisher<String> publisher;
    //    Publisher<sensor_msgs.Image> publisherImg;
    Publisher<sensor_msgs.CompressedImage> publisherImg;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/videoStream");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        mynode = connectedNode;

        final Log log = connectedNode.getLog();
        Subscriber<String> subscriber = connectedNode.newSubscriber("chatter", std_msgs.String._TYPE);

        publisher = connectedNode.newPublisher("chatterResponse", std_msgs.String._TYPE);
//        publisherImg = connectedNode.newPublisher("chatterImg", sensor_msgs.Image._TYPE);
        publisherImg = connectedNode.newPublisher("chatterImg", sensor_msgs.CompressedImage._TYPE);


        subscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {

                log.info("I heard: \"" + message.getData() + "\"");
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

                    //START COMPUTATION
                    int ylen = width * height;
//                    byte[] y = new byte[ylen];
                    byte[] u = new byte[ylen / 4];
                    byte[] v = new byte[ylen / 4];
                    byte[] nu = new byte[ylen / 4];
                    byte[] nv = new byte[ylen / 4];

//                    System.arraycopy(buf, 0, y, 0, y.length);
                    for (int i = 0; i < u.length; i++) {
                        v[i] = buf[ylen + 2 * i];
                        u[i] = buf[ylen + 2 * i + 1];
                    }
                    int uvWidth = width / 2;
                    int uvHeight = height / 2;
                    for (int j = 0; j < uvWidth / 2; j++) {
                        for (int i = 0; i < uvHeight / 2; i++) {
                            byte uSample1 = u[i * uvWidth + j];
                            byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                            byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                            byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                            nu[2 * (i * uvWidth + j)] = uSample1;
                            nu[2 * (i * uvWidth + j) + 1] = uSample1;
                            nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                            nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                            nv[2 * (i * uvWidth + j)] = vSample1;
                            nv[2 * (i * uvWidth + j) + 1] = vSample1;
                            nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                            nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
                        }
                    }
                    //nv21test
                    byte[] bytes = new byte[buf.length];
//                    System.arraycopy(y, 0, bytes, 0, y.length);
                    System.arraycopy(buf, 0, bytes, 0, ylen);
                    for (int i = 0; i < u.length; i++) {
                        bytes[ylen + (i * 2)] = nv[i];
                        bytes[ylen + (i * 2) + 1] = nu[i];
                    }

                    //END COMPUTATION
                    YuvImage yuvImage = new YuvImage(bytes,
                            ImageFormat.NV21,
                            width,
                            height,
                            null);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0,
                                    0,
                                    width,
                                    height),
                            75,//quality
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