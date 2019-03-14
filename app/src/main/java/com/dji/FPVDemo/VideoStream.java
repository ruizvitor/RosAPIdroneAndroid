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

            //resize buf

//            byte[] cp = new byte[(width * height)];
//            System.arraycopy(buf, 0, cp, 0, (width * height));
//
//            byte[] bytes = new byte[(width * height) / 4];
//
//            for (int i = 0; i < height; i+=2) {
//                for (int j = 0; j < width; j+=2) {
//                    bytes[((i/2)*width)+(j/2)] = cp[(i*width)+j];
//                }
//            }

//            ChannelBuffer cbuf = copiedBuffer(ByteOrder.LITTLE_ENDIAN, buf);
//
////            sensor_msgs.Image image = publisherImg.newMessage();
//            sensor_msgs.CompressedImage image = publisherImg.newMessage();
//
////            image.setWidth(width);
////            image.setHeight(height);
////            image.setEncoding("mono8");
////            image.setStep(width);
//
////            image.setHeader();
//
//            image.getHeader().setStamp(mynode.getCurrentTime());
//            image.getHeader().setFrameId("camera");
//            image.setFormat("jpeg");
//            image.setData(cbuf);
//
//            publisherImg.publish(image);


            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    int ylen = width * height;
                    //nv21test
//                    byte[] bytes = new byte[1080*960];
                    byte[] bytes = new byte[(height+(height/2))*width];
                    System.arraycopy(buf, 0, bytes, 0, ylen);

                    //END COMPUTATION
                    YuvImage yuvImage = new YuvImage(bytes,
                            ImageFormat.NV21,
                            width,
                            height,
                            null);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    yuvImage.compressToJpeg(new Rect(0,
//                                    0,
//                                    width,
//                                    height),
//                            10,//quality
//                            baos);

                    yuvImage.compressToJpeg(new Rect(0,
                                    0,
                                    width,
                                    height),
                            25,//quality
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