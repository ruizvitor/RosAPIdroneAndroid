package com.dji.RosAPI.internal.controller;


import android.app.Application;
import android.content.Context;


import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Main application
 */
public class DJISampleApplication extends Application {

    public static final String TAG = DJISampleApplication.class.getName();

    private static BaseProduct product;
    private static BluetoothProductConnector bluetoothConnector = null;
    private static Application app = null;

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    public static synchronized BluetoothProductConnector getBluetoothProductConnector() {
        bluetoothConnector = DJISDKManager.getInstance().getBluetoothProductConnector();
        return bluetoothConnector;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }

    public static synchronized HandHeld getHandHeldInstance() {
        if (!isHandHeldConnected()) {
            return null;
        }
        return (HandHeld) getProductInstance();
    }

    public static Application getInstance() {
        return DJISampleApplication.app;
    }


    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        com.secneo.sdk.Helper.install(this);
        app = this;
    }


}