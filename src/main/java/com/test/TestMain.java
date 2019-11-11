package com.test;

import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;

import com.rum.RUMClient;

import java.util.*;

public class TestMain {
    public static void main(String[] args) {
        RUMClient.RUMRegistration.register();
        System.out.println(new String("rum test!"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                baseTest();
            }
        }).start();
    }

    private static RUMClient client;

    static void baseTest() {
        client = new RUMClient(
                90300011,
                "18a1ccea-72e1-47c5-a84a-58543be27888",
                "52.83.220.166:13609",
                false,
                5 * 1000,
                true
        );
        client.getEvent().addListener("connect", new FPEvent.IListener() {
            @Override
            public void fpEvent(EventData evd) {
                System.out.println("base test connect");
                doTest();
            }
        });
        client.getEvent().addListener("close", new FPEvent.IListener() {
            @Override
            public void fpEvent(EventData evd) {
                System.out.println("base test closed, retry: " + evd.hasRetry());
            }
        });
        client.getEvent().addListener("error", new FPEvent.IListener() {
            @Override
            public void fpEvent(EventData evd) {
                evd.getException().printStackTrace();
            }
        });
        client.connect();
    }

    static void doTest() {
        Map attrs = new HashMap();
        attrs.put("test", 123);
        attrs.put("xxx", "yyy");

        Map ev_error = new HashMap();
        ev_error.put("ev", "error");
        ev_error.put("attrs", attrs);

        Map ev_info = new HashMap();
        ev_error.put("ev", "info");
        ev_error.put("attrs", attrs);

        List<Map> events = new ArrayList<Map>();
        events.add(ev_error);
        events.add(ev_info);

        // test customEvent
        client.customEvent("error", attrs, 5 * 1000, new FPCallback.ICallback() {
            @Override
            public void callback(CallbackData cbd) {
                Object obj = cbd.getPayload();
                if (obj != null) {
                    System.out.println("customEvent sent ok");
                } else {
                    System.err.println("customEvent err: " + cbd.getException().getMessage());
                }
            }
        });

        // test customEvents
        client.customEvents(events, 5 * 1000, new FPCallback.ICallback() {
            @Override
            public void callback(CallbackData cbd) {
                Object obj = cbd.getPayload();
                if (obj != null) {
                    System.out.println("customEvents sent ok");
                } else {
                    System.err.println("customEvents err: " + cbd.getException().getMessage());
                }
            }
        });
    }
}
