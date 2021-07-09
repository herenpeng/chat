package com.herenpeng.chat;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * 聊天室客户端
 *
 * @author herenpeng
 * @since 2021-07-09 12:00:00
 */
public class ChatClient {

    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 12345)) {
            // 读取服务端发的消息
            new Thread(() -> readMsg(socket)).start();
            OutputStream os = socket.getOutputStream();
            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入您的聊天室昵称：");
            while (true) {
                String chat = scanner.next();
                System.out.println("---------------------------");
                os.write(chat.getBytes());
            }
        } catch (Exception e) {
            System.out.println("【系统消息】聊天室炸了，BUG之神降临了");
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static void readMsg(Socket socket) {
        try {
            while (true) {
                InputStream is = socket.getInputStream();
                byte[] bytes = new byte[1024];
                int len = is.read(bytes);
                System.out.println(new String(bytes, 0, len));
            }
        } catch (Exception e) {
            System.out.println("【系统消息】你已退出聊天室，开始认真工作吧");
            System.exit(0);
        }
    }

}
