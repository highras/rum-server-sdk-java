# rum-server-sdk-java

## Usage

### Create

```
RUMClient client = new RUMClient(
    41000015,
    "affc562c-8796-4714-b8ae-4b061ca48a6b",
    "52.83.220.166",
    13609,
    true,
    5000
);

client.connect();
```

### Set Rum ID And Session ID . (Optional, If not specified, a random one will be generated)
```
client.setRumId(String rid);
client.setSession(long sid);
```

### Send Custom Event
```
Map attrs = new HashMap();
attrs.put("test", 123);
attrs.put("xxx", "yyy");

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
```

### Send Custom Events
```
Map ev_error = new HashMap();
ev_error.put("ev", "error");
ev_error.put("attrs", attrs);

Map ev_info = new HashMap();
ev_error.put("ev", "info");
ev_error.put("attrs", attrs);

List<Map> events = new ArrayList<Map>();
events.add(ev_error);
events.add(ev_info);

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
```

### Set Connected、Closed、Error Callback
```
client.getEvent().addListener("connect", new FPEvent.IListener() {

    @Override
    public void fpEvent(EventData evd) {

        System.out.println("test connect");
    }
});

client.getEvent().addListener("close", new FPEvent.IListener() {

    @Override
    public void fpEvent(EventData evd) {

        System.out.println("test closed");
    }
});

client.getEvent().addListener("error", new FPEvent.IListener() {

    @Override
    public void fpEvent(EventData evd) {

        System.out.println("test error: " + evd.getException().getMessage());
    }
});
```
