# fpnn rtm sdk java #

底层基于NIO实现, 支持FPNN加密.

#### 关于依赖 ####

* [fpnn.jar](https://github.com/highras/fpnn-sdk-java)
* [json.jar](https://github.com/stleary/JSON-java)
* [msgpack-core.jar](https://github.com/msgpack/msgpack-java)

#### 关于线程 ####

* 一个计时器线程`ThreadPool.getInstance().startTimerThread()`, 负责超时检测/安全检查 
    * 默认实现`Executors.newScheduledThreadPool(1)`, 构造`RTMClient`时可以选择是否启用该线程
    * 如果已有计时器, `NIOCore.getInstance().checkSecond()` 周期性调用该方法，以进行超时检查（建议频率1s）

* 一个线程池, 接口`ThreadPool.IThreadPool` 
    * 默认实现`Executors.newFixedThreadPool(FPConfig.MAX_THREAD_COUNT)`
    * 如需自己管理线程，实现该接口并注册线程池`ThreadPool.getInstance().setPool(IThreadPool value)`

* 不要阻塞事件触发和回调, 否则线程池将被耗尽

#### 一个例子 ####

```java
import com.fpnn.callback.CallbackData;
import com.fpnn.callback.FPCallback;
import com.fpnn.event.EventData;
import com.fpnn.event.FPEvent;
import com.rtm.RTMClient;
import com.rtm.RTMConfig;

// 创建Client
RTMClient client = new RTMClient(
    1017,
    "10d09e42-05d3-4d3c-b97a-50c8f27aa6c7",
    "rtm-nx-front.ifunplus.cn",
    13315,
    true,
    20 * 1000,
    true
);

// 添加监听
client.getEvent().addListener("connect", new FPEvent.IListener() {

    @Override
    public void fpEvent(FPEvent event) {

        System.out.println("Connected!");

        // 发送消息
        client.sendMessage(778877, 778899, (byte) 8, "hello !", "", 0, 5 * 1000, new FPCallback.ICallback() {

            @Override
            public void callback(FPCallback cbd) {

                Object obj = cbd.getPayload();

                if (obj != null) {

                    Map payload = (Map) obj;
                    System.out.println("\n[DATA] sendMessage:");
                    System.out.println(payload.toString());
                } else {

                    System.err.println("\n[ERR] sendMessage:");
                    System.err.println(cbd.getException().getMessage());
                }
            }
        });
    }
});

client.getEvent().addListener("close", new FPEvent.IListener() {

    @Override
    public void fpEvent(FPEvent event) {

        System.out.println("Closed!");
    }
});

client.getEvent().addListener("error", new FPEvent.IListener() {

    @Override
    public void fpEvent(FPEvent event) {

        event.getException().printStackTrace();
    }
});

// push service
client.getProcessor().getEvent().addListener(RTMConfig.SERVER_PUSH.recvPing, new FPEvent.IListener() {

    @Override
    public void fpEvent(FPEvent event) {

        System.out.println("\n[PUSH] ".concat(event.getType()).concat(":"));
        System.out.println(event.getPayload().toString());
    }
});

// 开启连接
client.connect();

// destroy
// client.destroy();
// client = null;
```

#### 测试 ####

参考`src/com/test/TestMain.java`:

```java

// case 1
baseTest();

// case 2
// asyncStressTest();

// case 3
// singleClientConcurrentTest();
```

#### Events ####

* `event`:
    * `connect`: 连接成功 
    * `error`: 发生异常
    * `close`: 连接关闭

#### PushService ####

请参考 `RTMConfig.SERVER_PUSH` 成员

* `ping`: RTMGate主动ping
    * `data`: **(Map)**

* `pushmsg`: RTMGate主动推送P2P消息
    * `data.from`: **(long)** 发送者 id
    * `data.to`: **(long)** 接收者 id
    * `data.mtype`: **(byte)** 消息类型
    * `data.mid`: **(long)** 消息 id, 当前链接会话内唯一
    * `data.msg`: **(String)** 消息内容
    * `data.attrs`: **(String)** 发送时附加的自定义内容
    * `data.mtime`: **(long)** 

* `pushgroupmsg`: RTMGate主动推送Group消息
    * `data.from`: **(long)** 发送者 id
    * `data.gid`: **(long)** Group id
    * `data.mtype`: **(byte)** 消息类型
    * `data.mid`: **(long)** 消息 id, 当前链接会话内唯一
    * `data.msg`: **(String)** 消息内容
    * `data.attrs`: **(String)** 发送时附加的自定义内容
    * `data.mtime`: **(long)** 

* `pushroommsg`: RTMGate主动推送Room消息
    * `data.from`: **(long)** 发送者 id
    * `data.rid`: **(long)** Room id
    * `data.mtype`: **(byte)** 消息类型
    * `data.mid`: **(long)** 消息 id, 当前链接会话内唯一
    * `data.msg`: **(String)** 消息内容
    * `data.attrs`: **(String)** 发送时附加的自定义内容
    * `data.mtime`: **(long)** 

* `pushfile`: RTMGate主动推送P2P文件
    * `data.from`: **(long)** 发送者 id
    * `data.to`: **(long)** 接收者 id
    * `data.mtype`: **(byte)** 文件类型, 请参考 `RTMConfig.FILE_TYPE` 成员
    * `data.mid`: **(long)** 消息 id, 当前链接会话内唯一
    * `data.msg`: **(String)** 文件获取地址(url)
    * `data.attrs`: **(String)** 发送时附加的自定义内容
    * `data.mtime`: **(long)** 

* `pushgroupfile`: RTMGate主动推送Group文件
    * `data.from`: **(long)** 发送者 id
    * `data.gid`: **(long)** Group id
    * `data.mtype`: **(byte)** 文件类型, 请参考 `RTMConfig.FILE_TYPE` 成员
    * `data.mid`: **(long)** 消息 id, 当前链接会话内唯一
    * `data.msg`: **(String)** 文件获取地址(url)
    * `data.attrs`: **(String)** 发送时附加的自定义内容
    * `data.mtime`: **(long)** 

* `pushroomfile`: RTMGate主动推送Room文件
    * `data.from`: **(long)** 发送者 id
    * `data.rid`: **(long)** Room id
    * `data.mtype`: **(byte)** 文件类型, 请参考 `RTMConfig.FILE_TYPE` 成员
    * `data.mid`: **(long)** 消息 id, 当前链接会话内唯一
    * `data.msg`: **(String)** 文件获取地址(url)
    * `data.attrs`: **(String)** 发送时附加的自定义内容
    * `data.mtime`: **(long)** 

* `pushevent`: RTMGate主动推送事件 
    * `data.event`: **(String)** 事件名称, 请参考 `RTMConfig.SERVER_EVENT` 成员
    * `data.uid`: **(long)** 触发者 id
    * `data.time`: **(int)** 触发时间(s)
    * `data.endpoint`: **(String)** 对应的RTMGate地址
    * `data.data`: **(String)** `预留`

#### API ####

* `constructor(int pid, String secret, String host, int port, boolean reconnect, int timeout, boolean startTimerThread)`: 构造RTMClient
    * `pid`: **(int)** 应用编号, RTM提供
    * `secret`: **(String)** 应用加密, RTM提供
    * `host`: **(String)** 地址, RTM提供
    * `port`: **(int)** 端口, RTM提供
    * `reconnect`: **(boolean)** 是否自动重连
    * `timeout`: **(int)** 超时时间(ms), 默认: `30 * 1000`
    * `startTimerThread`: **(boolean)** 是否开启计时器线程 (负责超时检测/安全检查)

* `constructor(int pid, String secret, String endpoint, boolean reconnect, int timeout, boolean startTimerThread)`: 构造RTMClient
    * `pid`: **(int)** 应用编号, RTM提供
    * `secret`: **(String)** 应用加密, RTM提供
    * `endpoint`: **(String)** 地址与端口, RTM提供
    * `reconnect`: **(boolean)** 是否自动重连
    * `timeout`: **(int)** 超时时间(ms), 默认: `30 * 1000`
    * `startTimerThread`: **(boolean)** 是否开启计时器线程 (负责超时检测/安全检查)

* `getProcessor`: **(FPProcessor)** 监听PushService的句柄

* `destroy()`: 断开连接并销毁

* `connect()`: 开启连接(非加密模式)

* `connect(String curve, byte[] derKey, boolean streamMode, boolean reinforce)`: 开启加密连接
    * `curve`: **(String)** 加密协议
    * `derKey`: **(byte[])** 加密公钥, 秘钥文件RTM提供
    * `streamMode`: **(boolean)** 是否开启流加密, `目前Java API不支持流加密`
    * `reinforce`: **(boolean)** 是否开启加强 ` 128 : 256 `

* `connect(String curve, String derPath, boolean streamMode, boolean reinforce)`: 开启加密连接
    * `curve`: **(String)** 加密协议
    * `derPath`: **(String)** 加密公钥文件路径, 秘钥文件RTM提供
    * `streamMode`: **(boolean)** 是否开启流加密, `目前Java API不支持流加密`
    * `reinforce`: **(boolean)** 是否开启加强 ` 128 : 256 `

* `sendMessage(long from, long to, byte mtype, String msg, String attrs, long mid, int timeout, FPCallback.ICallback callback)`: 发送消息
    * `from`: **(long)** 发送方 id
    * `to`: **(long)** 接收方uid
    * `mtype`: **(byte)** 消息类型
    * `msg`: **(String)** 消息内容
    * `attrs`: **(String)** 消息附加信息, 没有可传`""`
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendMessages(long from, List<Long> tos, byte mtype, String msg, String attrs, long mid, int timeout, FPCallback.ICallback callback)`: 发送多人消息
    * `from`: **(long)** 发送方 id
    * `tos`: **(List(Long))** 接收方uids
    * `mtype`: **(byte)** 消息类型
    * `msg`: **(String)** 消息内容
    * `attrs`: **(String)** 消息附加信息, 没有可传`""`
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendGroupMessage(long from, long gid, byte mtype, String msg, String attrs, long mid, int timeout, FPCallback.ICallback callback)`: 发送group消息
    * `from`: **(long)** 发送方 id
    * `gid`: **(long)** group id
    * `mtype`: **(byte)** 消息类型
    * `msg`: **(String)** 消息内容
    * `attrs`: **(String)** 消息附加信息, 可传`""`
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendRoomMessage(long from, long rid, byte mtype, String msg, String attrs, long mid, int timeout, FPCallback.ICallback callback)`: 发送room消息
    * `from`: **(long)** 发送方 id
    * `rid`: **(long)** room id
    * `mtype`: **(byte)** 消息类型
    * `msg`: **(String)** 消息内容
    * `attrs`: **(String)** 消息附加信息, 可传`""`
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `broadcastMessage(long from, byte mtype, String msg, String attrs, long mid, int timeout, FPCallback.ICallback callback)`: 广播消息(andmin id)
    * `from`: **(long)** admin id
    * `mtype`: **(byte)** 消息类型
    * `msg`: **(String)** 消息内容
    * `attrs`: **(String)** 消息附加信息, 可传`""`
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `addFriends(long uid, List<Long> friends, int timeout, FPCallback.ICallback callback)`: 添加好友，每次最多添加100人
    * `uid`: **(long)** 用户 id
    * `friends`: **(List(Long))** 多个好友 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `deleteFriends(long uid, List<Long> friends, int timeout, FPCallback.ICallback callback)`: 删除好友, 每次最多删除100人
    * `uid`: **(long)** 用户 id
    * `friends`: **(List(Long))** 多个好友 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `getFriends(long uid, int timeout, FPCallback.ICallback callback)`: 获取好友
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(List(Long))**
            * `exception`: **(Exception)**

* `isFriend(long uid, long fuid, int timeout, FPCallback.ICallback callback)`: 判断好友关系
    * `uid`: **(long)** 用户 id
    * `fuid`: **(long)** 好友 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(boolean)**
            * `exception`: **(Exception)**

* `isFriends(long uid, List<Long> fuids, int timeout, FPCallback.ICallback callback)`: 过滤好友关系, 每次最多过滤100人
    * `uid`: **(long)** 用户 id
    * `fuids`: **(List(Long))** 多个好友 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(List(Long))**
            * `exception`: **(Exception)**

* `addGroupMembers(long gid, List<Long> uids, int timeout, FPCallback.ICallback callback)`: 添加group成员, 每次最多添加100人
    * `gid`: **(long)** group id
    * `uids`: **(List(Long))** 多个用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `deleteGroupMembers(long gid, List<Long> uids, int timeout, FPCallback.ICallback callback)`:  删除group成员, 每次最多删除100人
    * `gid`: **(long)** group id
    * `uids`: **(List(Long))** 多个用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `deleteGroup(long gid, int timeout, FPCallback.ICallback callback)`: 删除group
    * `gid`: **(long)** group id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `getGroupMembers(long gid, int timeout, FPCallback.ICallback callback)`: 获取group成员
    * `gid`: **(long)** group id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(List(Long))**
            * `exception`: **(Exception)**

* `isGroupMember(long gid, long uid, int timeout, FPCallback.ICallback callback)`: 是否group成员
    * `gid`: **(long)** group id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(boolean)**
            * `exception`: **(Exception)**

* `getUserGroups(long uid, int timeout, FPCallback.ICallback callback)`: 获取用户的group
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(List(Long))**
            * `exception`: **(Exception)**

* `getToken(long uid, int timeout, FPCallback.ICallback callback)`: 获取auth token
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(String)**
            * `exception`: **(Exception)**

* `getOnlineUsers(List<Long> uids, int timeout, FPCallback.ICallback callback)`: 获取在线用户, 每次最多获取200个
    * `uids`: **(List(Long))** 多个用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(List(Long))**
            * `exception`: **(Exception)**

* `addGroupBan(long gid, long uid, int btime, int timeout, FPCallback.ICallback callback)`: 阻止用户消息(group)
    * `gid`: **(long)** group id
    * `uid`: **(long)** 用户 id
    * `btime`: **(int)** 阻止时间(s)
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `removeGroupBan(long gid, long uid, int timeout, FPCallback.ICallback callback)`: 取消阻止(group)
    * `gid`: **(long)** group id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `addRoomBan(long rid, long uid, int btime, int timeout, FPCallback.ICallback callback)`: 阻止用户消息(room)
    * `rid`: **(long)** room id
    * `uid`: **(long)** 用户 id
    * `btime`: **(int)** 阻止时间(s)
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `removeRoomBan(long rid, long uid, int timeout, FPCallback.ICallback callback)`: 取消阻止(room)
    * `rid`: **(long)** room id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `addProjectBlack(long uid, int btime, int timeout, FPCallback.ICallback callback)`: 阻止用户消息(project)
    * `uid`: **(long)** 用户 id
    * `btime`: **(int)** 阻止时间(s)
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `removeProjectBlack(int uid, int timeout, FPCallback.ICallback callback)`: 取消阻止(project)
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `isBanOfGroup(long gid, long uid, int timeout, FPCallback.ICallback callback)`: 检查阻止(group)
    * `gid`: **(long)** group id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(boolean)**
            * `exception`: **(Exception)**

* `isBanOfRoom(long rid, long uid, int timeout, FPCallback.ICallback callback)`: 检查阻止(room)
    * `rid`: **(long)** room id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(boolean)**
            * `exception`: **(Exception)**

* `isProjectBlack(long uid, int timeout, FPCallback.ICallback callback)`: 检查阻止(project)
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(boolean)**
            * `exception`: **(Exception)**

* `fileToken(long from, String cmd, List<Long> tos, long to, long rid, long gid, int timeout, FPCallback.ICallback callback)`: 获取发送文件的token
    * `from`: **(long)** 发送方 id
    * `cmd`: **(string)** 文件发送方式`sendfile | sendfiles | sendroomfile | sendgroupfile | broadcastfile`
    * `tos`: **(List(Long))** 接收方 uids
    * `to`: **(long)** 接收方 uid
    * `rid`: **(long)** Room id
    * `gid`: **(long)** Group id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `exception`: **(Exception)**
            * `payload`: **(Map(token:string, endpoint:string))**

* `getGroupMessage(long gid, boolean desc, int num, long begin, long end, long lastid, int timeout, FPCallback.ICallback callback)`: 获取Group历史消息
    * `gid`: **(long)** Group id
    * `desc`: **(boolean)** `true`: 则从`end`的时间戳开始倒序翻页, `false`: 则从`begin`的时间戳顺序翻页
    * `num`: **(int)** 获取数量, **一次最多获取20条, 建议10条**
    * `begin`: **(long)** 开始时间戳, 毫秒, 默认`0`, 条件：`>=`
    * `end`: **(long)** 结束时间戳, 毫秒, 默认`0`, 条件：`<=`
    * `lastid`: **(long)** 最后一条消息的id, 第一次默认传`0`, 条件：`> or <`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `exception`: **(Exception)**
            * `payload`: **(Map(num:int,lastid:long,begin:long,end:long,msgs:List(GroupMsg)))**
                * `GroupMsg.id` **(long)**
                * `GroupMsg.from` **(long)**
                * `GroupMsg.mtype` **(byte)**
                * `GroupMsg.mid` **(long)**
                * `GroupMsg.deleted` **(boolean)**
                * `GroupMsg.msg` **(String)**
                * `GroupMsg.attrs` **(String)**
                * `GroupMsg.mtime` **(long)**

* `getRoomMessage(long rid, boolean desc, int num, long begin, long end, long lastid, int timeout, FPCallback.ICallback callback)`: 获取Room历史消息
    * `rid`: **(long)** Room id
    * `desc`: **(boolean)** `true`: 则从`end`的时间戳开始倒序翻页, `false`: 则从`begin`的时间戳顺序翻页
    * `num`: **(int)** 获取数量, **一次最多获取20条, 建议10条**
    * `begin`: **(long)** 开始时间戳, 毫秒, 默认`0`, 条件：`>=`
    * `end`: **(long)** 结束时间戳, 毫秒, 默认`0`, 条件：`<=`
    * `lastid`: **(long)** 最后一条消息的id, 第一次默认传`0`, 条件：`> or <`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `exception`: **(Exception)**
            * `payload`: **(Map(num:int,lastid:long,begin:long,end:long,msgs:List(RoomMsg)))**
                * `RoomMsg.id` **(long)**
                * `RoomMsg.from` **(long)**
                * `RoomMsg.mtype` **(byte)**
                * `RoomMsg.mid` **(long)**
                * `RoomMsg.deleted` **(boolean)**
                * `RoomMsg.msg` **(String)**
                * `RoomMsg.attrs` **(String)**
                * `RoomMsg.mtime` **(long)**

* `getBroadcastMessage(boolean desc, int num, long begin, long end, long lastid, int timeout, FPCallback.ICallback callback)`: 获取广播历史消息
    * `desc`: **(boolean)** `true`: 则从`end`的时间戳开始倒序翻页, `false`: 则从`begin`的时间戳顺序翻页
    * `num`: **(int)** 获取数量, **一次最多获取20条, 建议10条**
    * `begin`: **(long)** 开始时间戳, 毫秒, 默认`0`, 条件：`>=`
    * `end`: **(long)** 结束时间戳, 毫秒, 默认`0`, 条件：`<=`
    * `lastid`: **(long)** 最后一条消息的id, 第一次默认传`0`, 条件：`> or <`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `exception`: **(Exception)**
            * `payload`: **(Map(num:int,lastid:long,begin:long,end:long,msgs:List(BroadcastMsg)))**
                * `BroadcastMsg.id` **(long)**
                * `BroadcastMsg.from` **(long)**
                * `BroadcastMsg.mtype` **(byte)**
                * `BroadcastMsg.mid` **(long)**
                * `BroadcastMsg.deleted` **(boolean)**
                * `BroadcastMsg.msg` **(String)**
                * `BroadcastMsg.attrs` **(String)**
                * `BroadcastMsg.mtime` **(long)**

* `getP2PMessage(long uid, long ouid, boolean desc, int num, long begin, long end, long lastid, int timeout, FPCallback.ICallback callback)`: 获取P2P历史消息
    * `uid`: **(long)** 获取和两个用户之间的历史消息
    * `ouid`: **(long)** 获取和两个用户之间的历史消息
    * `desc`: **(boolean)** `true`: 则从`end`的时间戳开始倒序翻页, `false`: 则从`begin`的时间戳顺序翻页
    * `num`: **(int)** 获取数量, **一次最多获取20条, 建议10条**
    * `begin`: **(long)** 开始时间戳, 毫秒, 默认`0`, 条件：`>=`
    * `end`: **(long)** 结束时间戳, 毫秒, 默认`0`, 条件：`<=`
    * `lastid`: **(long)** 最后一条消息的id, 第一次默认传`0`, 条件：`> or <`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `exception`: **(Exception)**
            * `payload`: **(Map(num:int,lastid:long,begin:long,end:long,msgs:List(P2PMsg)))**
                * `P2PMsg.id` **(long)**
                * `P2PMsg.direction` **(byte)**
                * `P2PMsg.mtype` **(byte)**
                * `P2PMsg.mid` **(long)**
                * `P2PMsg.deleted` **(boolean)**
                * `P2PMsg.msg` **(String)**
                * `P2PMsg.attrs` **(String)**
                * `P2PMsg.mtime` **(long)**

* `addRoomMember(long rid, long uid, int timeout, FPCallback.ICallback callback)`: 添加Room成员
    * `rid`: **(long)** Room id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `deleteRoomMember(long rid, long uid, int timeout, FPCallback.ICallback callback)`: 删除Room成员
    * `rid`: **(long)** Room id
    * `uid`: **(long)** 用户 id
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `addEvtListener(List<Long> gids, List<Long> rids, boolean p2p, List<String> events, int timeout, FPCallback.ICallback callback)`: 添加 `事件` / `消息` 监听
    * `gids`: **(List(Long))** 多个Group id
    * `rids`: **(List(Long))** 多个Room id
    * `p2p`: **(boolean)** P2P消息
    * `events`: **(List(String))** 多个事件名称, 请参考 `RTMConfig.SERVER_EVENT` 成员
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `removeEvtListener(List<Long> gids, List<Long> rids, boolean p2p, List<String> events, int timeout, FPCallback.ICallback callback)`: 删除 `事件` / `消息` 监听
    * `gids`: **(List(Long))** 多个Group id
    * `rids`: **(List(Long))** 多个Room id
    * `p2p`: **(boolean)** P2P消息
    * `events`: **(List(String))** 多个事件名称, 请参考 `RTMConfig.SERVER_EVENT` 成员
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `setEvtListener(boolean all, int timeout, FPCallback.ICallback callback)`: 更新 `事件` / `消息` 监听
    * `all`: **(boolean)** `true`: 监听所有 `事件` / `消息`, `false`: 取消所有 `事件` / `消息` 监听
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**
        
* `setEvtListener(List<Long> gids, List<Long> rids, boolean p2p, List<String> events, int timeout, FPCallback.ICallback callback)`: 更新 `事件` / `消息` 监听
    * `gids`: **(List(Long))** 多个Group id
    * `rids`: **(List(Long))** 多个Room id
    * `p2p`: **(boolean)** P2P消息, `true`: 监听, `false`: 取消监听
    * `events`: **(List(String))** 多个事件名称, 请参考 `RTMConfig.SERVER_EVENT` 成员
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `addDevice(long uid, String apptype, String devicetoken, int timeout, FPCallback.ICallback callback)`: 添加设备, 应用信息
    * `uid`: **(long)** 用户 id
    * `apptype`: **(String)** 应用信息
    * `devicetoken`: **(String)** 设备 token
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**
        
* `removeDevice(long uid, String devicetoken, int timeout, FPCallback.ICallback callback)`: 删除设备, 应用信息
    * `uid`: **(long)** 用户 id
    * `devicetoken`: **(String)** 设备 token
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `deleteMessage(long mid, long from, long xid, byte type, int timeout, FPCallback.ICallback callback)`: 删除消息
    * `mid`: **(long)** 消息 id
    * `from`: **(long)** 发送方 id
    * `xid`: **(long)** 接收放 id, `rid/gid/to`
    * `type`: **(byte)** 消息发送分类 `1:P2P, 2:Group, 3:Room, 4:Broadcast`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**

* `kickout(long uid, String ce, int timeout, FPCallback.ICallback callback)`: 踢掉一个用户或者一个链接
    * `uid`: **(long)** 用户 id
    * `ce`: **(String)** 踢掉`ce`对应链接, 多用户登录情况
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map)**
            * `exception`: **(Exception)**
        
* `sendFile(long from, long to, byte mtype, byte[] fileBytes, long mid, int timeout, FPCallback.ICallback callback)`: 发送文件
    * `from`: **(long)** 发送方 id
    * `to`: **(long)** 接收方 uid
    * `mtype`: **(byte)** 文件类型
    * `fileBytes`: **(byte[])** 文件内容
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendFiles(long from, List<Long>tos, byte mtype, byte[] fileBytes, long mid, int timeout, FPCallback.ICallback callback)`: 给多人发送文件
    * `from`: **(long)** 发送方 id
    * `tos`: **(long)** 接收方 uids
    * `mtype`: **(byte)** 文件类型
    * `fileBytes`: **(byte[])** 文件内容
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendGroupFile(long from, long gid, byte mtype, byte[] fileBytes, long mid, int timeout, FPCallback.ICallback callback)`: 给Group发送文件
    * `from`: **(long)** 发送方 id
    * `gid`: **(long)** Group id
    * `mtype`: **(byte)** 文件类型
    * `fileBytes`: **(byte[])** 文件内容
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendRoomFile(long from, long rid, byte mtype, byte[] fileBytes, long mid, int timeout, FPCallback.ICallback callback)`: 给Room发送文件
    * `from`: **(long)** 发送方 id
    * `rid`: **(long)** Room id
    * `mtype`: **(byte)** 文件类型
    * `fileBytes`: **(byte[])** 文件内容
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**

* `sendRoomFile(long from, byte mtype, byte[] fileBytes, long mid, int timeout, FPCallback.ICallback callback)`: 给整个Project发送文件
    * `from`: **(long)** 发送方 id
    * `mtype`: **(byte)** 文件类型
    * `fileBytes`: **(byte[])** 文件内容
    * `mid`: **(long)** 消息 id, 用于过滤重复消息, 非重发时为`0`
    * `timeout`: **(int)** 超时时间(ms)
    * `callback`: **(FPCallback.ICallback)** 回调方法
        * `cbdata`: **(CallbackData)**
            * `payload`: **(Map(mtime:long))**
            * `exception`: **(Exception)**
            * `mid`: **(long)**
