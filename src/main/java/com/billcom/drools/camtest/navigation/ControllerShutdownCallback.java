package com.billcom.drools.camtest.navigation;

import com.billcom.drools.camtest.controller.Shutdown;

@FunctionalInterface
interface ControllerShutdownCallback {
    void shutdown(Shutdown controller);
}
