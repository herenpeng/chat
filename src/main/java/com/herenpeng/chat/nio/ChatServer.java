package com.herenpeng.chat.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聊天室服务端，NIO 实现版本
 *
 * @author herenpeng
 * @since 2021-07-11 9:01
 */
public class ChatServer {

    /**
     * 启动类
     *
     * @param args 启动参数
     * @throws IOException 抛出IO异常
     */
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(12345));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        // 加载配置
        CHAT_CFG_RELOAD_PASSWORD = UUID.randomUUID().toString();
        logInfo("【系统消息】聊天室配置加载密钥：" + CHAT_CFG_RELOAD_PASSWORD);
        reloadChatCfg(null, args.length == 1 ? args[0] : null);

        new Thread(() -> start(selector)).start();

        logInfo("【系统消息】聊天室启动成功了！");
    }

    /**
     * 服务开始方法
     *
     * @param selector 选择器
     */
    private static void start(Selector selector) {
        try {
            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (Exception e) {
            logInfo("【系统消息】聊天室发生了异常……");
            e.printStackTrace();
        } finally {
            logInfo("【系统消息】正在关闭聊天室资源……");
        }
    }

    /**
     * 刷新配置标识密钥
     */
    private static String CHAT_CFG_RELOAD_PASSWORD;

    /**
     * 机器人是否开启的标识
     */
    private static final String robotCfgKey = "robot";
    /**
     * 机器人概率，值为5表示1/5的概率机器人回复
     */
    private static final String robotProCfgKey = "robotPro";

    private static final Map<String, String> chatCfg = new ConcurrentHashMap<>();

    static {
        // 是否开启机器人发送消息，默认不开启
        chatCfg.put(robotCfgKey, "false");
        chatCfg.put(robotProCfgKey, "5");
    }

    /**
     * 通过配置 key 获取布尔类型的值
     *
     * @param cfgKey 配置key
     * @return 布尔类型的值
     */
    private static boolean getBolByChatCfg(String cfgKey) {
        String cfgValue = chatCfg.get(cfgKey);
        return "true".equals(cfgValue);
    }

    /**
     * 通过配置 key 获取 int 类型的值
     *
     * @param cfgKey 配置key
     * @return int 类型的值
     */
    private static int getIntByChatCfg(String cfgKey) {
        String cfgValue = chatCfg.get(cfgKey);
        return Integer.parseInt(isEmpty(cfgValue) ? "0" : cfgValue);
    }


    /**
     * 保存所有用户socket的集合
     */
    private static final Map<SocketChannel, ChatUser> userDB = new ConcurrentHashMap<>();

    /**
     * 聊天记录分隔符
     */
    private static final String chatSeparate = "---------------------------";

    /**
     * 给所有的用户发送系统消息
     *
     * @param msg 系统消息
     * @throws IOException 抛出异常
     */
    private static void sendSysMsg(String msg) throws IOException {
        for (SocketChannel socket : userDB.keySet()) {
            String sysMsg = getCurrentTime() + "\n" + msg + "\n" + chatSeparate;
            sendMsgToUser(socket, sysMsg);
        }
    }

    /**
     * 发送消息给其他用户
     *
     * @param username 消息发送用户名称
     * @param self     消息发送的用户socket
     * @param msg      消息
     * @throws IOException 抛出异常
     */
    private static void sendMsgToOtherUser(SocketChannel self, String username, String msg) throws IOException {
        for (SocketChannel socket : userDB.keySet()) {
            if (socket.equals(self)) {
                continue;
            }
            String sendMsg = "（" + username + "） " + getCurrentTime() + "\n" + msg + "\n" + chatSeparate;
            sendMsgToUser(socket, sendMsg);
        }
    }

    /**
     * 给指定的用户发送消息，会自动在消息上下文拼接 消息发送时间，消息分隔符 等等
     *
     * @param username 消息发送用户名称
     * @param socket   消息发送的用户socket
     * @param msg      消息
     * @throws IOException 抛出异常
     */
    private static void sendMsgToUser(String username, SocketChannel socket, String msg) throws IOException {
        String sendMsg = "（" + username + "） " + getCurrentTime() + "\n" + msg + "\n" + chatSeparate;
        sendMsgToUser(socket, sendMsg);
    }

    /**
     * 给指定的用户发送消息，文本消息
     *
     * @param socket  消息发送的用户socket
     * @param sendMsg 消息
     * @throws IOException 抛出异常
     */
    private static void sendMsgToUser(SocketChannel socket, String sendMsg) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(sendMsg.getBytes());
        while (buffer.hasRemaining()) {
            socket.write(buffer);
        }
    }


    /**
     * 关闭服务
     *
     * @param server 服务
     */
    private static void close(ServerSocket server) {
        try {
            for (SocketChannel socket : userDB.keySet()) {
                socket.close();
            }
            server.close();
            userDB.clear();
        } catch (IOException e) {
            logInfo("【系统消息】关闭聊天室资源发生了异常……");
            e.printStackTrace();
        }
    }

    /**
     * 链接客户端
     *
     * @param key key
     * @throws IOException 抛出异常
     */
    private static void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel socket = server.accept();
        socket.configureBlocking(false);
        socket.register(key.selector(), SelectionKey.OP_READ);

        userDB.put(socket, new ChatUser());
        sendMsgToUser(socket, "============================\n" +
                "1、本聊天室仅为娱乐，请勿在该聊天室内谈论敏感内容，比如涉政，涉黄，账号密码等等！\n" +
                "2、聊天室内容明文传输，聊天信息泄露本聊天室概不负责！\n" +
                "3、本聊天室内容后台不做任何存储，聊天信息如果需要请自行保留！\n" +
                "4、最终解释权归本聊天室所有！\n" +
                "============================");
    }

    /**
     * 读取消息
     *
     * @param key SelectionKey 对象
     * @return 消息
     * @throws IOException
     */
    private static String readMsg(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        StringBuilder msg = new StringBuilder();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (socket.read(buffer) > 0) {
                buffer.flip();
                msg.append(new String(buffer.array(), buffer.position(), buffer.limit()));
                buffer.clear();
            }
        } catch (Exception e) {
            logout(socket);
        }
        return msg.toString();
    }


    /**
     * 用户开始聊天方法
     *
     * @param key SelectionKey 对象
     */
    private static void handleRead(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        String chatMsg = readMsg(key);
        ChatUser chatUser = userDB.get(socket);
        if (chatUser == null) {
            return;
        }
        // 如果用户名为空，说明没有登录
        if (isEmpty(chatUser.getUsername())) {
            chatUser.setUsername(chatMsg);
            if (CHAT_CFG_RELOAD_PASSWORD.equals(chatMsg)) {
                sendMsgToUser(socket, "【系统消息】请输入需要刷新的聊天室配置");
            } else {
                loginTip(socket, chatMsg);
                // 机器人欢迎
                robotWelcome(chatMsg);
            }
        } else {
            if (CHAT_CFG_RELOAD_PASSWORD.equals(chatMsg)) {
                // 刷新配置
                reloadChatCfg(socket, chatMsg);
            } else {
                sendMsgToOtherUser(socket, chatUser.getUsername(), chatMsg);
                // 机器人回复消息
                randomRobotReply(chatMsg);
            }
        }
    }

    /**
     * 登出操作
     *
     * @param socket SocketChannel对象
     */
    private static void logout(SocketChannel socket) throws IOException {
        ChatUser chatUser = userDB.remove(socket);
        socket.close();
        String username = chatUser.getUsername();
        if (isNotEmpty(username)) {
            String msg = "【系统消息】" + username + "已退出聊天室";
            logInfo(msg);
            sendSysMsg(msg);
        }
    }

    /**
     * 刷新聊天室的配置
     *
     * @param chatCfgStr 配置字符串
     * @param self       socket对象
     * @throws IOException 抛出异常
     */
    private static void reloadChatCfg(SocketChannel self, String chatCfgStr) throws IOException {
        if (isEmpty(chatCfgStr)) {
            return;
        }
        String[] cfgList = chatCfgStr.split("&");
        for (String cfgStr : cfgList) {
            String[] cfg = cfgStr.split("=");
            if (cfg.length != 2) {
                continue;
            }
            String key = cfg[0];
            if (chatCfg.containsKey(key)) {
                chatCfg.put(key, cfg[1]);
            }
        }
        // 刷新完配置发送通知
        StringBuilder sb = new StringBuilder();
        sb.append("【系统消息】聊天室配置已刷新\n");
        for (Map.Entry<String, String> entry : chatCfg.entrySet()) {
            sb.append("配置").append(entry.getKey()).append("当前值为：").append(entry.getValue()).append("\n");
        }
        sb.append(chatSeparate);
        logInfo(sb.toString());
        if (self != null) {
            sendMsgToUser(self, sb.toString());
            // 登出
            logout(self);
        }
    }


    /**
     * 用户登录时，发送系统提示
     *
     * @param username 用户名
     * @throws IOException 抛出异常
     */
    private static void loginTip(SocketChannel socket, String username) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("【系统消息】").append(username).append("已加入聊天室\n");
        logInfo(sb + "远端主机地址：" + socket.getRemoteAddress());
        sb.append("当前聊天室成员有：\n");
        List<String> usernameList = getLoginUsernames();
        for (int i = 0; i < usernameList.size(); i++) {
            sb.append(i + 1).append("、").append(usernameList.get(i));
            if (i < usernameList.size() - 1) {
                sb.append("\n");
            }
        }
        sendSysMsg(sb.toString());
    }

    /**
     * 打印日志
     *
     * @param message 日志西信息
     */
    private static void logInfo(String message) {
        System.out.println(getCurrentDateTime() + " " + message);
    }

    /**
     * 随机数对象
     */
    private static final Random random = new Random();

    /**
     * 随机回复消息集合
     */
    private static final List<String> replyMsgList = new ArrayList<>();

    static {
        replyMsgList.add("人生的路上，也许我们不惧伤身，但我们害怕伤心，也许我们不怕问题，但我们害怕丧失信心。黑夜来临，影响我们情绪的不是黑暗，而是孤独；寒风吹来，摧残我们意志的不是冰冷，而是心灵。只要心有所属，生活自有奇迹，人生活得就是一种心情，一种精神。");
        replyMsgList.add("我们都在 用力的活着\n酸甜苦辣里 醒过也醉过\n也曾倔强脆弱 依然执着\n相信花开以后 会结果");
        replyMsgList.add("软弱的人被生活折磨，强悍的人折磨生活。");
        replyMsgList.add("道可道，非常道；名可名，非常名。");
        replyMsgList.add("知其白，守其黑，为天下式。\n为天下式，常德不忒，复归于无极。");
        replyMsgList.add("残阳如血,落于江中,将江水也染成了猩红色,而我们的船,正渐渐驶向那团血色之中");
        replyMsgList.add("一旦希望之灯熄灭，生活就会突然变得黑暗。");
        replyMsgList.add("阅读使人充实，会谈使人敏捷，写作使人精确。");
        replyMsgList.add("我直接喷！");
        replyMsgList.add("不是吧，asir!");
        replyMsgList.add("桃之夭夭，灼灼其华。之子于归，宜其室家。");
        replyMsgList.add("一帘清雨，垂下了一汪泪，一份缠绵，揉断了心碎。");
        replyMsgList.add("用心聆听，深深呼吸，烟花雨，梨花月，寄一缕风的香魂，远离喧嚣。");
        replyMsgList.add("往事不必再提，人生已多风雨，我只愿风止于秋水，而我止于你。");
        replyMsgList.add("愿以一朵花的姿态行走世间，看得清世间繁杂却不在心中留下痕迹。花开成景，花落成诗。");
    }

    private static final List<String> nightReplyMsgList = new ArrayList<>();

    static {
        nightReplyMsgList.add("早点睡吧，命最重要！");
        nightReplyMsgList.add("太晚了，明天再聊！");
        nightReplyMsgList.add("我去洗澡了！");
        nightReplyMsgList.add("我要去睡觉了，不聊了！");
    }

    /**
     * 关键字机器人回复的消息
     */
    private static final Map<String, List<String>> keyWordReplyMsgMap = new ConcurrentHashMap<>();

    static {
        List<String> robot = new ArrayList<>();
        robot.add("我在！");
        robot.add("在呢！");
        robot.add("叫我做什么？");
        robot.add("别烦我，我现在很烦躁啊！");
        keyWordReplyMsgMap.put("机器人", robot);

        List<String> alive = new ArrayList<>();
        alive.add("我也在用力地活着啊！");
        alive.add("谁不是呢？");
        alive.add("直接用力啊！");
        alive.add("我们都在 用力的活着\n酸甜苦辣里 醒过也醉过\n也曾倔强脆弱 依然执着\n相信花开以后 会结果");
        keyWordReplyMsgMap.put("用力地活着", alive);
    }

    /**
     * 机器人列表
     */
    private static final List<Robot> robotList = new ArrayList<>();

    static {
        Robot robot1 = new Robot("机器人·风", replyMsgList, nightReplyMsgList, keyWordReplyMsgMap);
        robotList.add(robot1);

        Robot robot2 = new Robot("机器人·雪", replyMsgList, nightReplyMsgList, keyWordReplyMsgMap);
        robotList.add(robot2);

        Robot robot3 = new Robot("机器人·雪", replyMsgList, nightReplyMsgList, keyWordReplyMsgMap);
        robotList.add(robot3);

        Robot robot4 = new Robot("机器人·月", replyMsgList, nightReplyMsgList, keyWordReplyMsgMap);
        robotList.add(robot4);

        Robot robot5 = new Robot("机器人·马云", replyMsgList, null, keyWordReplyMsgMap);
        robotList.add(robot5);
    }

    /**
     * 随机选择一个机器人
     *
     * @return 机器人
     */
    private static Robot randomRobot() {
        int i = random.nextInt(robotList.size());
        return robotList.get(i);
    }

    /**
     * 机器人欢迎语
     *
     * @param username 登入的用户
     * @throws IOException 抛出异常
     */
    private static void robotWelcome(String username) throws IOException {
        if (!getBolByChatCfg(robotCfgKey)) {
            return;
        }
        String welcomeMsg;
        if (username.contains("何")) {
            welcomeMsg = "欢迎何总进入聊天室";
        } else if (username.contains("肖")) {
            welcomeMsg = "欢迎肖总进入聊天室";
        } else if (username.contains("池")) {
            welcomeMsg = "欢迎池总进入聊天室";
        } else if (username.contains("李")) {
            welcomeMsg = "欢迎李总进入聊天室";
        } else {
            welcomeMsg = "欢迎" + username + "进入聊天室";
        }
        sendMsgToOtherUser(null, randomRobot().getUsername(), welcomeMsg);
    }


    /**
     * 随机机器人回复消息
     *
     * @param msg 用户发的消息
     */
    private static void randomRobotReply(String msg) throws IOException {
        if (!getBolByChatCfg(robotCfgKey)) {
            return;
        }
        Robot robot = randomRobot();
        // 随机一条关键字消息回复，如果回复了关键字，就不回复其他消息
        String sendMsg = robot.randomKeyWordReplyMsg(msg);
        // 获取概率，因为默认值为0，所以需要进行一下判断
        int robotProCfgValue = getIntByChatCfg(robotProCfgKey);
        if (isEmpty(sendMsg) && robotProCfgValue > 0) {
            // 五分之一的概率会回复消息
            int i = random.nextInt(robotProCfgValue);
            if (i == 0) {
                if (isNight()) {
                    sendMsg = robot.randomNightReplyMsg();
                } else {
                    sendMsg = robot.randomReplyMsg();
                }
            }
        }
        if (sendMsg != null) {
            sendMsgToOtherUser(null, robot.getUsername(), sendMsg);
        }
    }

    /**
     * 获取当前在线的所有玩家名称
     *
     * @return 当前在线的所有玩家名称
     */
    private static List<String> getLoginUsernames() {
        return userDB.values().stream().map(ChatUser::getUsername).filter(Objects::nonNull).collect(Collectors.toList());
    }


    /**
     * 判断时间是否是 11:00 - 04:59 晚上
     *
     * @return 是返回true，否则返回false
     */
    private static boolean isNight() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= 23 || hour <= 4;
    }

    /**
     * 时间格式化对象
     */
    private static final SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DateTimeSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取当前的时间的格式化字符串
     *
     * @return 当前的时间的格式化字符串
     */
    private static synchronized String getCurrentTime() {
        return timeSdf.format(new Date());
    }

    /**
     * 获取当前的日期时间的格式化字符串
     *
     * @return 当前的日期时间的格式化字符串
     */
    private static synchronized String getCurrentDateTime() {
        return DateTimeSdf.format(new Date());
    }

    /**
     * 判断一个字符串是否为空
     *
     * @param string 字符串
     * @return 为空返回true，否则返回false
     */
    private static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * 判断一个字符串是否不为空
     *
     * @param string 字符串
     * @return 不为空返回true，否则返回false
     */
    private static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    /**
     * 机器人对象
     */
    private static class Robot {
        // 机器人名称
        private final String username;
        // 机器人随机回复
        private final List<String> replyMsgList;
        // 机器人晚上回复
        private final List<String> nightReplyMsgList;
        // 机器人关键字回复
        private final Map<String, List<String>> keyWordReplyMsgMap;

        public Robot(String username, List<String> replyMsgList, List<String> nightReplyMsgList, Map<String, List<String>> keyWordReplyMsgMap) {
            this.username = username;
            this.replyMsgList = replyMsgList;
            this.nightReplyMsgList = nightReplyMsgList;
            this.keyWordReplyMsgMap = keyWordReplyMsgMap;
        }

        public String getUsername() {
            return username;
        }

        /**
         * 随机一条回复消息
         *
         * @return 回复消息，没有消息返回null
         */
        public String randomReplyMsg() {
            if (this.replyMsgList.isEmpty()) {
                return null;
            }
            int i = random.nextInt(this.replyMsgList.size());
            return this.replyMsgList.get(i);
        }

        /**
         * 随机一条晚上回复的消息
         *
         * @return 晚上回复的消息，没有消息返回null
         */
        public String randomNightReplyMsg() {
            if (this.nightReplyMsgList.isEmpty()) {
                return null;
            }
            int i = random.nextInt(this.nightReplyMsgList.size());
            return this.nightReplyMsgList.get(i);
        }

        /**
         * 根据消息随机一条回复消息，
         *
         * @param msg 消息
         * @return 没有命中关键字活着没有消息返回null
         */
        public String randomKeyWordReplyMsg(String msg) {
            if (this.keyWordReplyMsgMap.isEmpty()) {
                return null;
            }
            // 触发关键字回复消息
            for (Map.Entry<String, List<String>> entry : this.keyWordReplyMsgMap.entrySet()) {
                if (msg.contains(entry.getKey())) {
                    List<String> msgList = entry.getValue();
                    int i = random.nextInt(msgList.size());
                    return msgList.get(i);
                }
            }
            return null;
        }
    }

    /**
     * 封装的 ChatUser
     */
    private static class ChatUser {

        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

}
