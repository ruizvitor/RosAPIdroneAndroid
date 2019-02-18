//package com.github.rosjava.android.ros_hello_world_app;
package com.dji.FPVDemo;

import android.os.Bundle;

//import android.support.v7.app.NotificationCompat;
//import android.support.v4.app.NotificationCompat;
//import android.support.v4.content.res.ResourcesCompat;

import java.net.URI;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class RosHelloWorldApp extends RosActivity {
    //    public RosHelloWorldApp() {
//        this("RosHelloWorldApp", "RosHelloWorldApp");
//    }
    public RosHelloWorldApp() {
        this("RosHelloWorldApp", "RosHelloWorldApp", URI.create("http://192.168.1.104:11311/"));
    }
//
//    protected RosHelloWorldApp(String notificationTicker, String notificationTitle) {
//        super(notificationTicker, notificationTitle);
//    }


    protected RosHelloWorldApp(String notificationTicker, String notificationTitle, URI uri) {
        super(notificationTicker, notificationTitle, uri);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ros_main);
    }

//    @Override
//    protected void init(NodeMainExecutor nodeMainExecutor) {
//    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        Talker talker = new Talker();
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());
        nodeConfiguration.setNodeName("talker");
        nodeMainExecutor.execute(talker, nodeConfiguration);

        Listener listener = new Listener();
        nodeConfiguration.setNodeName("listener");
        nodeMainExecutor.execute(listener, nodeConfiguration);
    }
}
