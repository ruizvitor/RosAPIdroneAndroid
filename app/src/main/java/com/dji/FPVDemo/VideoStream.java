package com.dji.FPVDemo;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;


import com.dji.FPVDemo.media.DJIVideoStreamDecoder;
import com.dji.videostreamdecodingsample.media.NativeHelper;

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
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import std_msgs.MultiArrayDimension;
import std_msgs.MultiArrayLayout;
import std_msgs.String;

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

public class VideoStream extends AbstractNodeMain {

    Context context = null;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    Camera mCamera = null;
    DJIVideoStreamDecoder djiDecoder = null;
    public static VideoStream instance = null;

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

        initMy();
    }

    public void pubMessage(java.lang.String msg) {
        std_msgs.String str = publisher.newMessage();
        str.setData(msg);
        publisher.publish(str);
    }


    public void publishImage(byte[] buf, int size) {
//        android.util.Log.d(TAG, "SENDING IMAGE!!!!!!!!");
//        ChannelBuffer cbuf = copiedBuffer(ByteOrder.BIG_ENDIAN, buf);
        ChannelBuffer cbuf = copiedBuffer(ByteOrder.LITTLE_ENDIAN, buf);

        sensor_msgs.Image image = publisherImg.newMessage();
        image.setWidth(1);
        image.setHeight(size);
        image.setData(cbuf);
        publisherImg.publish(image);
    }

    public static VideoStream getInstance() {
        if(instance == null){
            instance = new VideoStream();
        }
        return instance;
    }

    public void setContext(Context cx) {
        context = cx;
    }

    void initMy() {

        djiDecoder = new DJIVideoStreamDecoder(context, VideoStream.getInstance());

        final BaseProduct product = FPVDemoApplication.getProductInstance();

        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                //handle h264
                djiDecoder.parse(videoBuffer, size);
            }
        };

        if (null == product || !product.isConnected()) {
            mCamera = null;
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mCamera = product.getCamera();
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            android.util.Log.d(TAG, "can't change mode of camera, error:" + djiError.getDescription());
                        } else {
                            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
                            }
                        }
                    }
                });


            }
        }

    }
}