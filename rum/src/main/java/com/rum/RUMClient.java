package com.rum;

import com.fpnn.*;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;
import com.fpnn.encryptor.FPEncryptor;
import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.rum.json.JsonHelper;
import com.rum.msgpack.PayloadPacker;
import com.rum.msgpack.PayloadUnpacker;

import java.io.*;
import java.util.*;
import java.lang.StringBuilder;

public class RUMClient {

    class DelayConnLocker {
        public int status = 0;
        public int count = 0;
        public long timestamp = 0;
    }

    private int _pid;
    private int _timeout;
    private boolean _debug;
    private String _secret;
    private String _endpoint;
    private boolean _reconnect;
    private boolean _isClose;

    private long _session;
    private long _initSession;

    private String _rumId;
    private BaseClient _baseClient;

    private Object self_locker = new Object();
    private IDGenerator _midGenerator = new IDGenerator();
    private IDGenerator _eidGenerator = new IDGenerator();

    /**
     * @param {int}             pid
     * @param {String}          secret
     * @param {String}          endpoint
     * @param {boolean}         reconnect
     * @param {int}             timeout
     * @param {boolean}         debug
     */
    public RUMClient(int pid, String secret, String endpoint, boolean reconnect, int timeout, boolean debug) {
        if (pid <= 0) {
            System.out.println("[RUM] The 'pid' Is Zero Or Negative!");
            return;
        }
        if (secret == null || secret.isEmpty()) {
            System.out.println("[RUM] The 'secret' Is Null Or Empty!");
            return;
        }
        if (endpoint == null || endpoint.isEmpty()) {
            System.out.println("[RUM] The 'endpoint' Is Null Or Empty!");
            return;
        }

        System.out.println("[RUM] rum_sdk@" + RUMConfig.VERSION + ", fpnn_sdk@" + FPConfig.VERSION);

        this._pid = pid;
        this._secret = secret;
        this._endpoint = endpoint;
        this._reconnect = reconnect;
        this._timeout = timeout;
        this._debug = debug;
        this._initSession = this._eidGenerator.gen();
        ErrorRecorder.getInstance().setRecorder(new RUMErrorRecorder(this._event, this._debug));
    }

    private FPEvent _event = new FPEvent();

    public FPEvent getEvent() {
        return this._event;
    }

    public void setSession(long value) {
        synchronized (self_locker) {
            this._session = value;
        }
    }

    public void setRumId(String value) {
        synchronized (self_locker) {
            this._rumId = value;
        }
    }

    private void sendQuest(FPData data, FPCallback.ICallback callback, int timeout) {
        synchronized (self_locker) {
            if (this._baseClient != null) {
                this._baseClient.sendQuest(data, this._baseClient.questCallback(callback), timeout);
            }
        }
    }

    public void destroy() {
        synchronized (delayconn_locker) {
            delayconn_locker.status = 0;
            delayconn_locker.count = 0;
            delayconn_locker.timestamp = 0;
        }

        synchronized (self_locker) {
            this._isClose = true;
            this.getEvent().fireEvent(new EventData(this, "close", !this._isClose && this._reconnect));
            this.getEvent().removeListener();

            if (this._baseClient != null) {
                this._baseClient.close();
                this._baseClient = null;
            }

            this._session = 0;
            this._rumId = null;
        }
    }

    private EncryptInfo _encryptInfo;

    private void connect(EncryptInfo info) {
        if (info == null) {
            this.connect();
        } else {
            this.connect(info.curve, info.derKey, info.streamMode, info.reinforce);
        }
    }

    public void connect() {
        this.createBaseClient();
        synchronized (self_locker) {
            if (this._baseClient != null) {
                this._encryptInfo = null;
                this._baseClient.connect();
            }
        }
    }

    public void connect(String curve, byte[] derKey, boolean streamMode, boolean reinforce) {
        this.createBaseClient();
        synchronized (self_locker) {
            if (this._baseClient != null) {
                this._encryptInfo = new EncryptInfo(curve, derKey, streamMode, reinforce);
                this._baseClient.connect(curve, derKey, streamMode, reinforce);
            }
        }
    }

    public void connect(String curve, String derPath, boolean streamMode, boolean reinforce) {
        byte[] derKey = new LoadFile().read(derPath);
        if (derKey != null && derKey.length > 0) {
            this.connect(curve, derKey, streamMode, reinforce);
        }
    }

    private void createBaseClient() {
        synchronized (self_locker) {
            if (this._isClose) {
                return;
            }
            if (this._baseClient == null) {
                final RUMClient self = this;
                this._baseClient = new BaseClient(this._endpoint, this._timeout);
                this._baseClient.clientCallback = new FPClient.ICallback() {
                    @Override
                    public void clientConnect(EventData evd) {
                        self.onConnect(evd);
                    }
                    @Override
                    public void clientClose(EventData evd) {
                        self.onClose(evd);
                    }
                    @Override
                    public void clientError(EventData evd) {
                        self.onError(evd.getException());
                    }
                };
            }
        }
    }

    private void onConnect(EventData evd) {
        this.getEvent().fireEvent(new EventData(this, "connect"));
        synchronized (delayconn_locker) {
            delayconn_locker.count = 0;
        }
    }

    private void onClose(EventData evd) {
        synchronized (self_locker) {
            if (this._baseClient != null) {
                this._baseClient = null;
            }
        }
        this.getEvent().fireEvent(new EventData(this, "close", !this._isClose && this._reconnect));
        this.reconnect();
    }

    private void onError(Exception ex) {
        if (ex != null) {
            ErrorRecorder.getInstance().recordError(ex);
        }
    }

    private DelayConnLocker delayconn_locker = new DelayConnLocker();

    private void reconnect() {
        if (!this._reconnect) {
            return;
        }
        EncryptInfo info = null;
        synchronized (self_locker) {
            if (this._isClose) {
                return;
            }
            info = this._encryptInfo;
        }

        int count = 0;
        synchronized (delayconn_locker) {
            delayconn_locker.count++;
            count = delayconn_locker.count;
        }
        if (count <= RUMConfig.RECONN_COUNT_ONCE) {
            this.connect(info);
            return;
        }
        synchronized (delayconn_locker) {
            delayconn_locker.status = 1;
            delayconn_locker.timestamp = FPManager.getInstance().getMilliTimestamp();
        }
    }

    private void delayConnect(long timestamp) {
        synchronized (delayconn_locker) {
            if (delayconn_locker.status == 0) {
                return;
            }
            if (timestamp - delayconn_locker.timestamp < RUMConfig.CONNCT_INTERVAL) {
                return;
            }
            delayconn_locker.status = 0;
            delayconn_locker.count = 0;
        }

        EncryptInfo info = null;
        synchronized (self_locker) {
            info = this._encryptInfo;
        }
        this.connect(info);
    }

    /**
     *
     * @param {String}                  ev
     * @param {Map}                     attrs
     * @param {int}                     timeout
     * @param {FPCallback.ICallback}    callback
     *
     * @callback
     * @param {CallbackData}            cbdata
     *
     * <CallbackData>
     * @param {Map}                     payload
     * @param {Exception}               exception
     * </CallbackData>
     */
    public void customEvent(String ev, Map attrs, int timeout, FPCallback.ICallback callback) {
        List<Map> events = new ArrayList<Map>();
        events.add(this.buildEvent(ev, attrs));
        FPData data = this.buildSendData(events);

        if (data == null) {
            if (callback != null) {
                callback.callback(new CallbackData(new Exception("param error")));
            }
            return;
        }
        this.sendQuest(data, callback, timeout);
    }

    /**
     *
     * @param {List<Map>}               events
     * @param {int}                     timeout
     * @param {FPCallback.ICallback}    callback
     *
     * @callback
     * @param {CallbackData}            cbdata
     *
     * <CallbackData>
     * @param {Map}                     payload
     * @param {Exception}               exception
     * </CallbackData>
     */
    public void customEvents(List<Map> events, int timeout, FPCallback.ICallback callback) {
        FPData data = this.buildSendData(events);
        if (data == null) {
            if (callback != null) {
                callback.callback(new CallbackData(new Exception("param error")));
            }
            return;
        }
        this.sendQuest(data, callback, timeout);
    }

    private FPData buildSendData(List<Map> events) {
        List<Map> send_list = new ArrayList<Map>();
        Iterator<Map> iter = events.iterator();

        while(iter.hasNext()) {
            Map event = iter.next();
            if (event.containsKey("ev") && event.containsKey("attrs")) {
                Map ev = this.buildEvent((String)event.get("ev"), (Map)event.get("attrs"));
                send_list.add(ev);
            }
        }
        if (send_list.size() == 0) {
            return null;
        }

        long salt = this._midGenerator.gen();
        String sign = this.genSign(salt);
        Map payload = new HashMap() {
            {
                put("salt", salt);
                put("sign", sign);
                put("events", send_list);
            }
        };
        payload.put("pid", this._pid);

        FPData data = new FPData();
        data.setFlag(0x1);
        data.setMtype(0x1);
        data.setMethod("adds");
        byte[] bytes = new byte[0];
        PayloadPacker packer = new PayloadPacker();
        try {
            packer.pack(payload);
            bytes = packer.toByteArray();
        } catch (Exception ex) {
            ErrorRecorder.getInstance().recordError(ex);
            return null;
        }
        data.setPayload(bytes);
        return data;
    }

    private Map buildEvent(String ev, Map attrs) {
        int ts = FPManager.getInstance().getTimestamp();
        long eid = this._eidGenerator.gen();
        Map event = new HashMap() {
            {
                put("ev", ev);
                put("eid", eid);
                put("source", "java");
                put("attrs", attrs);
                put("ts", ts);
            }
        };
        synchronized (self_locker) {
            if (this._session == 0) {
                this._session = this._initSession;
            }
            if (this._rumId == null || this._rumId.isEmpty()) {
                this._rumId = this.UUID(0, 16, 'S');
            }

            event.put("sid", this._session);
            event.put("rid", this._rumId);
        }
        return event;
    }

    private String genSign(long slat) {
        StringBuffer sb = new StringBuffer(Integer.toString(this._pid));
        sb.append(":");
        sb.append(this._secret);
        sb.append(":");
        sb.append(slat);
        return FPManager.getInstance().md5(sb.toString());
    }

    private String UUID(int len, int radix, char fourteen) {
        char[] chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
        char[] uuid_chars = new char[36];

        if (radix == 0) {
            radix = chars.length;
        }

        if (len > 0) {
            // Compact form
            for (int i = 0; i < len; i++) {
                uuid_chars[i] = chars[ 0 | (int)(Math.random() * radix) ];
            }
        } else {
            // rfc4122, version 4 form
            int r;
            // Fill in random data.  At i==19 set the high bits of clock sequence as
            // per rfc4122, sec. 4.1.5
            for (int j = 0; j < 36; j++) {
                r = 0 | (int)(Math.random() * 16);
                uuid_chars[j] = chars[ (j == 19) ? (r & 0x3) | 0x8 : r ];
            }

            // rfc4122 requires these characters
            uuid_chars[8] = uuid_chars[13] = uuid_chars[18] = uuid_chars[23] = '-';
            uuid_chars[14] = fourteen;

            // add timestamp(ms) at prefix
            char[] ms_chars = String.valueOf(FPManager.getInstance().getMilliTimestamp()).toCharArray();
            for (int k = 0; k < ms_chars.length; k++) {
                uuid_chars[k] = ms_chars[k];
            }
        }
        return new String(uuid_chars);
    }

    class IDGenerator {
        private long count = 0;
        private StringBuilder sb = new StringBuilder(20);
        private Object lock_obj = new Object();

        public long gen() {
            synchronized (lock_obj) {
                if (++count > 999) {
                    count = 1;
                }
                sb.setLength(0);
                sb.append(FPManager.getInstance().getMilliTimestamp());

                if (count < 100) {
                    sb.append("0");
                }
                if (count < 10) {
                    sb.append("0");
                }
                sb.append(count);
                return Long.valueOf(sb.toString());
            }
        }
    }

    public static class RUMRegistration {
        public static void register() {
            FPManager.getInstance().init();
        }
    }

    class RUMErrorRecorder implements ErrorRecorder.IErrorRecorder {
        private boolean _debug;
        private FPEvent _event;

        public RUMErrorRecorder(FPEvent evt, boolean debug) {
            this._event = evt;
            this._debug = debug;
        }

        @Override
        public void recordError(Exception ex) {
            if (this._debug) {
                System.out.println(ex);
            }
            if (this._event != null) {
                this._event.fireEvent(new EventData(this, "error", ex));
            }
        }
    }

    class EncryptInfo {
        public String curve;
        public byte[] derKey;
        public boolean streamMode;
        public boolean reinforce;

        public EncryptInfo(String curve, byte[] derKey, boolean streamMode, boolean reinforce) {
            curve = curve;
            derKey = derKey;
            streamMode = streamMode;
            reinforce = reinforce;
        }
    }

    class BaseClient extends FPClient {
        public BaseClient(String endpoint, int timeout) {
            super(endpoint, timeout);
        }
        public BaseClient(String host, int port, int timeout) {
            super(host, port, timeout);
        }

        @Override
        public CallbackData sendQuest(FPData data, int timeout) {
            CallbackData cbd = null;

            try {
                cbd = super.sendQuest(data, timeout);
            } catch (Exception ex) {
                ErrorRecorder.getInstance().recordError(ex);
            }

            if (cbd != null) {
                this.checkFPCallback(cbd);
            }
            return cbd;
        }

        /**
         * @param {String}  curve
         * @param {byte[]}  derKey
         * @param {boolean} streamMode
         * @param {boolean} reinforce
         */
        public void connect(String curve, byte[] derKey, boolean streamMode, boolean reinforce) {
            if (derKey != null && derKey.length > 0) {
                if (this.encryptor(curve, derKey, streamMode, reinforce, false)) {
                    this.connect(new FPClient.IKeyData() {
                        @Override
                        public FPData getKeyData(FPEncryptor encryptor) {
                            byte[] bytes = new byte[0];
                            Map<String, Object> map = new HashMap<String, Object>();
                            map.put("publicKey", encryptor.cryptoInfo().selfPublicKey);
                            map.put("streamMode", encryptor.cryptoInfo().streamMode);
                            map.put("bits", encryptor.cryptoInfo().keyLength);

                            try {
                                PayloadPacker packer = new PayloadPacker();
                                packer.pack(map);
                                bytes = packer.toByteArray();
                            } catch (Exception ex) {
                                ErrorRecorder.getInstance().recordError(ex);
                            }

                            FPData data = new FPData();
                            if (bytes.length > 0) {
                                data.setPayload(bytes);
                            }
                            return data;
                        }
                    });
                    return;
                }
            }
            this.connect();
        }

        /**
         * @param {FPCallback.ICallback} callback
         */
        public FPCallback.ICallback questCallback(FPCallback.ICallback callback) {
            BaseClient self = this;
            return new FPCallback.ICallback() {
                @Override
                public void callback(CallbackData cbd) {
                    if (callback == null) {
                        return;
                    }
                    self.checkFPCallback(cbd);
                    callback.callback(cbd);
                }
            };
        }

        private void checkFPCallback(CallbackData cbd) {
            Map payload = null;
            FPData data = cbd.getData();
            Boolean isAnswerException = false;

            if (data != null) {
                if (data.getFlag() == 0) {
                    try {
                        JsonHelper.IJson json = JsonHelper.getInstance().getJson();
                        payload = json.toMap(data.jsonPayload());
                    } catch (Exception ex) {
                        ErrorRecorder.getInstance().recordError(ex);
                    }
                }

                if (data.getFlag() == 1) {
                    try {
                        PayloadUnpacker unpacker = new PayloadUnpacker(data.msgpackPayload());
                        payload = unpacker.unpack();
                    } catch (Exception ex) {
                        ErrorRecorder.getInstance().recordError(ex);
                    }
                }

                if (this.getPackage().isAnswer(data)) {
                    isAnswerException = data.getSS() != 0;
                }
            }
            cbd.checkException(isAnswerException, payload);
        }
    }

    class LoadFile {
        /**
         * @param {String} derPath
         */
        public byte[] read(String derPath) {
            File f = new File(derPath);

            if (!f.exists()) {
                System.err.println(new String("file not exists! path: ").concat(f.getAbsolutePath()));
                return null;
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) f.length());
            BufferedInputStream in = null;

            try {
                in = new BufferedInputStream(new FileInputStream(f));
                int buf_size = 1024;
                byte[] buffer = new byte[buf_size];
                int len = 0;

                while (-1 != (len = in.read(buffer, 0, buf_size))) {
                    bos.write(buffer, 0, len);
                }
                return bos.toByteArray();
            } catch (Exception ex) {
                ErrorRecorder.getInstance().recordError(ex);
            } finally {
                try {
                    in.close();
                    bos.close();
                } catch (Exception ex) {
                    ErrorRecorder.getInstance().recordError(ex);
                }
            }
            return null;
        }
    }
}
