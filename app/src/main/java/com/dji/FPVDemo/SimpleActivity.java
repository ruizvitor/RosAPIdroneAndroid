package com.dji.FPVDemo;

import android.os.Bundle;


import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

public class SimpleActivity extends RosActivity {

    /*
        START ROS CONFIG
     */
    public SimpleActivity() {
        this("RosDrone", "RosDrone", URI.create("http://192.168.1.21:11311/"));//phantom wifi
        //this("RosDrone", "RosDrone", URI.create("http://192.168.1.104:11311/"));//dinf3
    }

    protected SimpleActivity(String notificationTicker, String notificationTitle, URI uri) {
        super(notificationTicker, notificationTitle, uri);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mix);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());

//        TalkerListener talkerListener = new TalkerListener();
//        nodeConfiguration.setNodeName("talkerListener");
//        nodeMainExecutor.execute(talkerListener, nodeConfiguration);
        VideoDownloader videoDownloader = new VideoDownloader();
        nodeConfiguration.setNodeName("videoDownloader");
        nodeMainExecutor.execute(videoDownloader, nodeConfiguration);
    }

    @Override
    public void onDestroy() {
        nodeMainExecutorService.forceShutdown();
        super.onDestroy();
    }
    /*
        END ROS CONFIG
     */

}
