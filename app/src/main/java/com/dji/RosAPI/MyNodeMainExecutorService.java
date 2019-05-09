package com.dji.RosAPI;

import com.google.common.base.Preconditions;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;
import org.ros.RosCore;
import org.ros.concurrent.ListenerGroup;
import org.ros.concurrent.SignalRunnable;
import org.ros.exception.RosRuntimeException;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class MyNodeMainExecutorService extends Service implements NodeMainExecutor {

    private static final String TAG = "MyNdMainExecutorService";

    // NOTE(damonkohler): If this is 0, the notification does not show up.
    private static final int ONGOING_NOTIFICATION = 1;

    public static final String ACTION_START = "org.ros.android.ACTION_START_NODE_RUNNER_SERVICE";
    public static final String ACTION_SHUTDOWN = "org.ros.android.ACTION_SHUTDOWN_NODE_RUNNER_SERVICE";
    public static final String EXTRA_NOTIFICATION_TITLE = "org.ros.android.EXTRA_NOTIFICATION_TITLE";
    public static final String EXTRA_NOTIFICATION_TICKER = "org.ros.android.EXTRA_NOTIFICATION_TICKER";

    public static final String NOTIFICATION_CHANNEL_ID = "org.ros.android";
    public static final String CHANNEL_NAME = "ROS Android background service";

    private final NodeMainExecutor nodeMainExecutor;
    private final IBinder binder;
    private final ListenerGroup<MyNodeMainExecutorServiceListener> listeners;

    private Handler handler;
    private WakeLock wakeLock;
    private WifiLock wifiLock;
    private RosCore rosCore;
    private URI masterUri;
//    private URI masterUri = URI.create("http://192.168.1.20:11311/");
    private String rosHostname;

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MyNodeMainExecutorService getService() {
            return MyNodeMainExecutorService.this;
        }
    }

    public MyNodeMainExecutorService() {
        super();
        rosHostname = null;
        nodeMainExecutor = DefaultNodeMainExecutor.newDefault();
        binder = new LocalBinder();
        listeners =
                new ListenerGroup<MyNodeMainExecutorServiceListener>(
                        nodeMainExecutor.getScheduledExecutorService());
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
        int wifiLockType = WifiManager.WIFI_MODE_FULL;
        try {
            wifiLockType = WifiManager.class.getField("WIFI_MODE_FULL_HIGH_PERF").getInt(null);
        } catch (Exception e) {
            // We must be running on a pre-Honeycomb device.
            Log.w(TAG, "Unable to acquire high performance wifi lock.");
        }
        WifiManager wifiManager = WifiManager.class.cast(getApplicationContext().getSystemService(WIFI_SERVICE));
        wifiLock = wifiManager.createWifiLock(wifiLockType, TAG);
        wifiLock.acquire();
    }

    @Override
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration,
                        Collection<NodeListener> nodeListeneners) {
        nodeMainExecutor.execute(nodeMain, nodeConfiguration, nodeListeneners);
    }

    @Override
    public void execute(NodeMain nodeMain, NodeConfiguration nodeConfiguration) {
        execute(nodeMain, nodeConfiguration, null);
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return nodeMainExecutor.getScheduledExecutorService();
    }

    @Override
    public void shutdownNodeMain(NodeMain nodeMain) {
        nodeMainExecutor.shutdownNodeMain(nodeMain);
    }

    @Override
    public void shutdown() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MyNodeMainExecutorService.this);
                builder.setMessage("Continue shutting down?");
                builder.setPositiveButton("Shutdown", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        forceShutdown();
                    }
                });
                builder.setNegativeButton("Cancel", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_TOAST);
                alertDialog.show();
            }
        });
    }

    public void forceShutdown() {
        signalOnShutdown();
        stopForeground(true);
        stopSelf();
    }

    public void addListener(MyNodeMainExecutorServiceListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MyNodeMainExecutorServiceListener listener)
    {
        listeners.remove(listener);
    }

    private void signalOnShutdown() {
        listeners.signal(new SignalRunnable<MyNodeMainExecutorServiceListener>() {
            @Override
            public void run(MyNodeMainExecutorServiceListener nodeMainExecutorServiceListener) {
                nodeMainExecutorServiceListener.onShutdown(MyNodeMainExecutorService.this);
            }
        });
    }

    @Override
    public void onDestroy() {
        toast("Shutting down...");
        nodeMainExecutor.shutdown();
        if (rosCore != null) {
            rosCore.shutdown();
        }
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (wifiLock.isHeld()) {
            wifiLock.release();
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent.getAction() == null) {
//            return START_NOT_STICKY;
//        }
//        if (intent.getAction().equals(ACTION_START)) {
////            Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TICKER));
////            Preconditions.checkArgument(intent.hasExtra(EXTRA_NOTIFICATION_TITLE));
////            Intent notificationIntent = new Intent(this, MyNodeMainExecutorService.class);
////            notificationIntent.setAction(MyNodeMainExecutorService.ACTION_SHUTDOWN);
////            PendingIntent pendingIntent = PendingIntent.getService(this, 0, notificationIntent, 0);
////            Notification notification = buildNotification(intent, pendingIntent);
////
////            startForeground(ONGOING_NOTIFICATION, notification);
//        }
//        if (intent.getAction().equals(ACTION_SHUTDOWN)) {
////            shutdown();
//        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public URI getMasterUri() {
        return masterUri;
    }

    public void setMasterUri(URI uri) {
        masterUri = uri;
    }

    public void setRosHostname(String hostname) {
        rosHostname = hostname;
    }

    public String getRosHostname() {
        return rosHostname;
    }
    /**
     * This version of startMaster can only create private masters.
     *
     * @deprecated use {@link public void startMaster(Boolean isPrivate)} instead.
     */
    @Deprecated
    public void startMaster() {
        startMaster(true);
    }

    /**
     * Starts a new ros master in an AsyncTask.
     * @param isPrivate
     */
    public void startMaster(boolean isPrivate) {
        AsyncTask<Boolean, Void, URI> task = new AsyncTask<Boolean, Void, URI>() {
            @Override
            protected URI doInBackground(Boolean[] params) {
                MyNodeMainExecutorService.this.startMasterBlocking(params[0]);
                return MyNodeMainExecutorService.this.getMasterUri();
            }
        };
        task.execute(isPrivate);
        try {
            task.get();
        } catch (InterruptedException e) {
            throw new RosRuntimeException(e);
        } catch (ExecutionException e) {
            throw new RosRuntimeException(e);
        }
    }

    /**
     * Private blocking method to start a Ros Master.
     * @param isPrivate
     */
    private void startMasterBlocking(boolean isPrivate) {
        if (isPrivate) {
            rosCore = RosCore.newPrivate();
        } else if (rosHostname != null) {
            rosCore = RosCore.newPublic(rosHostname, 11311);
        } else {
            rosCore = RosCore.newPublic(11311);
        }
        rosCore.start();
        try {
            rosCore.awaitStart();
        } catch (Exception e) {
            throw new RosRuntimeException(e);
        }
        masterUri = rosCore.getUri();
    }

    public void toast(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MyNodeMainExecutorService.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private Notification buildNotification(Intent intent, PendingIntent pendingIntent) {
//        Notification notification = null;
//        Notification.Builder builder = null;
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
//            NotificationChannel chan = new NotificationChannel(
//                    NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
//            chan.setLightColor(Color.BLUE);
//            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            assert manager != null;
//            manager.createNotificationChannel(chan);
//            builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
//        } else {
//            builder = new Notification.Builder(this);
//        }
//        notification = builder.setContentIntent(pendingIntent)
//                .setOngoing(true)
//                .setSmallIcon(R.mipmap.icon)
//                .setTicker(intent.getStringExtra(EXTRA_NOTIFICATION_TICKER))
//                .setWhen(System.currentTimeMillis())
//                .setContentTitle(intent.getStringExtra(EXTRA_NOTIFICATION_TITLE))
//                .setAutoCancel(true)
//                .setContentText("Tap to shutdown.")
//                .build();
//        return notification;
//    }
}
