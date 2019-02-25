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

    /**
     * Save the buffered data into a JPG image file
     */
    public void publishScreenShot(byte[] buf, int width, int height) {
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
                10,//quality
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

    public void publishScreenShotv2(byte[] buf, int width, int height) {

        if (publisherImg != null) {
            android.util.Log.d(TAG, "SENDING IMAGE!!!!!!!!");
            ChannelBuffer cbuf = copiedBuffer(ByteOrder.LITTLE_ENDIAN, buf);

            sensor_msgs.CompressedImage image = publisherImg.newMessage();
            image.setData(cbuf);
            publisherImg.publish(image);
        }

    }
}