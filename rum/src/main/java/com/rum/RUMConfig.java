package com.rum;

public class RUMConfig {
    public static final String VERSION = "1.0.2";

    public static final int RECONN_COUNT_ONCE = 1;                      //一次重新连接流程中的尝试次数
    public static final int CONNCT_INTERVAL = 1 * 1000;                 //尝试重新连接的时间间隔(ms)
}
