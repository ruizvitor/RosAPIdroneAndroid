package com.dji.FPVDemo;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;


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
    Publisher<sensor_msgs.Image> publisherImg;

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
        publisherImg = connectedNode.newPublisher("chatterImg", sensor_msgs.Image._TYPE);


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


    public void publishImage(byte[] buf, int width, int height) {
        if (publisherImg != null) {
            ChannelBuffer cbuf = copiedBuffer(ByteOrder.LITTLE_ENDIAN, buf);

            sensor_msgs.Image image = publisherImg.newMessage();
//        image.setWidth(width);
//        image.setHeight(height);
//        image.setWidth(960);
//        image.setHeight(1080);

//            image.setWidth(960 * 1080);
//            image.setHeight(1);

            image.setWidth(960);
            image.setHeight(1080);
            image.setStep(960);

            image.setEncoding("mono8");
            image.setData(cbuf);
            publisherImg.publish(image);
        }
    }
}