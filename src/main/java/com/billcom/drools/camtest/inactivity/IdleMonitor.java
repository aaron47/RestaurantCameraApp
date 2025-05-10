package com.billcom.drools.camtest.inactivity;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.util.Duration;


public class IdleMonitor {
    private final Timeline idleTimeLine;
    private final EventHandler<Event> userEventHandler;

    public IdleMonitor(Duration idleTime, Runnable notifier, boolean startMonitoring) {
        this.idleTimeLine = new Timeline(new KeyFrame(idleTime, e -> notifier.run()));
        this.idleTimeLine.setCycleCount(Animation.INDEFINITE);

        this.userEventHandler = e -> this.notIdle();

        if (startMonitoring) {
            this.startMonitoring();
        }
    }

    public IdleMonitor(Duration idleTime, Runnable notifier) {
        this(idleTime, notifier, true);
    }

    public void registerEvent(Scene scene, EventType<? extends Event> eventType) {
        scene.addEventHandler(eventType, this.userEventHandler);
    }

    public void registerEvent(Node node, EventType<? extends Event> eventType) {
        node.addEventHandler(eventType, this.userEventHandler);
    }

    public void removeEvent(Scene scene, EventType<? extends Event> eventType) {
        scene.removeEventHandler(eventType, this.userEventHandler);
    }

    public void removeEvent(Node node, EventType<? extends Event> eventType) {
        node.removeEventHandler(eventType, this.userEventHandler);
    }

    public void notIdle() {
        if (this.idleTimeLine.getStatus() == Animation.Status.RUNNING) {
            this.idleTimeLine.playFromStart();
        }
    }

    public void startMonitoring() {
        this.idleTimeLine.playFromStart();
    }

    public void stopMonitoring() {
        this.idleTimeLine.stop();
    }
}
