package com.dji.RosAPI;

public interface MyNodeMainExecutorServiceListener {

    /**
     * @param nodeMainExecutorService the {@link MyNodeMainExecutorService} that was shut down
     */
    void onShutdown(MyNodeMainExecutorService nodeMainExecutorService);
}