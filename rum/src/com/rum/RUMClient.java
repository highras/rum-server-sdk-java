package com.rum;

import com.fpnn.ErrorRecorder;
import com.fpnn.FPClient;
import com.fpnn.FPData;
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;
import com.fpnn.encryptor.FPEncryptor;
import com.fpnn.event.EventData;
import com.fpnn.nio.ThreadPool;
import com.rum.json.JsonHelper;
import com.rum.msgpack.PayloadPacker;
import com.rum.msgpack.PayloadUnpacker;

import java.io.*;
import java.util.*;
import java.lang.StringBuilder;
import java.security.MessageDigest;

public class RUMClient extends BaseClient {

    private static class MidGenerator {

        static private long count = 0;
        static private StringBuilder sb = new StringBuilder(20);

        static public synchronized long gen() {

            long c = 0;

            if (++count >= 999) {

                count = 0;
            }

            c = count;

            sb.setLength(0);
            sb.append(System.currentTimeMillis());

            if (c < 100) {

                sb.append("0");
            }

            if (c < 10) {

                sb.append("0");
            }

            sb.append(c);

            return Long.valueOf(sb.toString());
        }
    }

    private int _pid;
    private String _secret;
    private long _session;
    private String _rumId;

    /**
     * @param {int}         pid
     * @param {String}      secret
     * @param {String}      host
     * @param {int}         port
     * @param {boolean}     reconnect
     * @param {int}         timeout
     */
    public RUMClient(int pid, String secret, String host, int port, boolean reconnect, int timeout) {

        super(host, port, reconnect, timeout, true);

        this._pid = pid;
        this._secret = secret;
    }

    @Override
    public void destroy() {

        super.destroy();

        this._session = 0;
        this._rumId = null;
    }

    public void setSession(long value) {

        this._session = value;
    }

    public void setRumId(String value) {

        this._rumId = value;
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

        long salt = MidGenerator.gen();

        Map payload = new HashMap();

        payload.put("pid", this._pid);
        payload.put("sign", this.genSign(salt));
        payload.put("salt", salt);
        payload.put("events", send_list);

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
        }

        data.setPayload(bytes);

        return data;
    }

    private Map buildEvent(String ev, Map attrs) {

        if (this._session == 0) {

            this._session = MidGenerator.gen();
        }

        if (this._rumId == null || this._rumId.isEmpty()) {

            this._rumId = this.UUID(0, 16, 's');
        }

        Map event = new HashMap();

        event.put("ev", ev);
        event.put("sid", this._session);
        event.put("rid", this._rumId);
        event.put("ts", Math.floor(System.currentTimeMillis() / 1000));
        event.put("eid", MidGenerator.gen());
        event.put("source", "java");
        event.put("attrs", attrs);

        return event;
    }

    private String genSign(long slat) {

        StringBuffer sb = new StringBuffer(Integer.toString(this._pid));

        sb.append(":");
        sb.append(this._secret);
        sb.append(":");
        sb.append(Long.toString(slat));

        return this.md5(sb.toString());
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
            char[] ms_chars = String.valueOf(System.currentTimeMillis()).toCharArray();

            for (int k = 0; k < ms_chars.length; k++) {

                uuid_chars[k] = ms_chars[k];
            }
        }

        return new String(uuid_chars);
    }
}

class BaseClient extends FPClient {

    public BaseClient(String endpoint, boolean reconnect, int timeout, boolean startTimerThread) {

        super(endpoint, reconnect, timeout);

        if (startTimerThread) {

            ThreadPool.getInstance().startTimerThread();
        }
    }

    public BaseClient(String host, int port, boolean reconnect, int timeout, boolean startTimerThread) {

        super(host, port, reconnect, timeout);

        if (startTimerThread) {

            ThreadPool.getInstance().startTimerThread();
        }
    }

    @Override
    protected void init(String host, int port, boolean reconnect, int timeout) {

        super.init(host, port, reconnect, timeout);
    }

    @Override
    public void sendQuest(FPData data, FPCallback.ICallback callback, int timeout) {

        super.sendQuest(data, this.questCallback(callback), timeout);
    }

    @Override
    public CallbackData sendQuest(FPData data, int timeout) {

        CallbackData cbd = null;

        try {

            cbd = super.sendQuest(data, timeout);
        }catch(Exception ex){

            this.getEvent().fireEvent(new EventData(this, "error", ex));
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
                        PayloadPacker packer = new PayloadPacker();

                        Map map = new HashMap();

                        map.put("publicKey", encryptor.cryptoInfo().selfPublicKey);
                        map.put("streamMode", encryptor.cryptoInfo().streamMode);
                        map.put("bits", encryptor.cryptoInfo().keyLength);

                        try {

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
     * @param {String}  curve
     * @param {String}  derPath
     * @param {boolean} streamMode
     * @param {boolean} reinforce
     */
    public void connect(String curve, String derPath, boolean streamMode, boolean reinforce) {

        byte[] bytes = new LoadFile().read(derPath);
        this.connect(curve, bytes, streamMode, reinforce);
    }

    /**
     * @param {byte[]} bytes
     */
    public String md5(byte[] bytes) {

        byte[] md5Binary = new byte[0];

        try {

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            md5Binary = md5.digest();
        } catch (Exception ex) {

            ErrorRecorder.getInstance().recordError(ex);
            return null;
        }

        return this.bytesToHexString(md5Binary, false);
    }

    /**
     * @param {String} str
     */
    public String md5(String str) {

        byte[] md5Binary = new byte[0];

        try {

            byte[] bytes = str.getBytes("UTF-8");

            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(bytes);
            md5Binary = md5.digest();
        } catch (Exception ex) {

            ErrorRecorder.getInstance().recordError(ex);
            return null;
        }

        return this.bytesToHexString(md5Binary, false);
    }

    /**
     * @param {byte[]}  bytes
     * @param {boolean} isLowerCase
     */
    public String bytesToHexString(byte[] bytes, boolean isLowerCase) {

        String from = isLowerCase ? "%02x" : "%02X";
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);

        for (byte b : bytes) {

            formatter.format(from, b);
        }

        return sb.toString();
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

                JsonHelper.IJson json = JsonHelper.getInstance().getJson();
                payload = json.toMap(data.jsonPayload());
            }

            if (data.getFlag() == 1) {

                PayloadUnpacker unpacker = new PayloadUnpacker(data.msgpackPayload());

                try {

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

            System.out.println(new String("file not exists! path: ").concat(f.getAbsolutePath()));
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
