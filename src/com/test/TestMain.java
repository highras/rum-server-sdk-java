package com.test;

import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;

import com.rum.RUMClient;

import java.util.*;

public class TestMain {

    public static void main(String[] args) {

        System.out.println(new String("rum test!"));

        new Thread(new Runnable() {

            @Override
            public void run() {

                // case 1
                baseTest();
            }
        }).start();
    }

    public static void baseTest() {

        RUMClient client = new RUMClient(
                41000015,
                "affc562c-8796-4714-b8ae-4b061ca48a6b",
                "52.83.220.166",
                13609,
                true,
                5000
        );

        FPEvent.IListener listener = new FPEvent.IListener() {

            @Override
            public void fpEvent(EventData event) {

                switch (event.getType()) {
                    case "connect":

                        System.out.println("base test connect");
                        break;
                    case "close":

                        System.out.println("base test closed");
                        break;
                    case "error":

                        System.out.println("base test error: " + event.getException().getMessage());
                        break;
                }
            }
        };

        client.getEvent().addListener("connect", listener);
        client.getEvent().addListener("close", listener);
        client.getEvent().addListener("error", listener);

        client.connect();

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
        client.customEvent("error", attrs, 5000, new FPCallback.ICallback() {

            @Override
            public void callback(CallbackData cbd) {

                Object obj = cbd.getPayload();

                if (obj != null) {

                    System.out.println("customEvent sent ok");
                } else {

                    System.err.println("customEvent sent err: " + cbd.getException().getMessage());
                }
            }
        });

        // test customEvents
        client.customEvents(events, 5000, new FPCallback.ICallback() {

            @Override
            public void callback(CallbackData cbd) {

                Object obj = cbd.getPayload();

                if (obj != null) {

                    System.out.println("customEvents sent ok");
                } else {

                    System.err.println("customEvents sent err: " + cbd.getException().getMessage());
                }
            }
        });
    }
}
