module com.billcom.drools.camtest {
    requires javafx.controls;
    requires javafx.fxml;
    requires webcam.capture;
    requires java.desktop;
    requires java.mail;
    requires activation;
    requires org.bytedeco.opencv;
    requires com.google.api.client;
    requires com.google.api.client.json.gson;
    requires google.api.client;
    requires com.google.api.services.drive;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires javafx.media;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.core;
    requires slf4j.api;


    opens com.billcom.drools.camtest to javafx.fxml;
    exports com.billcom.drools.camtest;
    exports com.billcom.drools.camtest.controller;
    opens com.billcom.drools.camtest.controller to javafx.fxml;
    exports com.billcom.drools.camtest.util;
    opens com.billcom.drools.camtest.util to javafx.fxml;
}
