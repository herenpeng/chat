package com.herenpeng.chat.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 聊天室客户端，NIO 实现版本
 *
 * @author herenpeng
 * @since 2021-07-09 12:00:00
 */
public class ChatClient {

    public static void main(String[] args) {
        try (Selector selector = Selector.open();
             SocketChannel socket = SocketChannel.open()) {
            //连接服务端socket
            socket.configureBlocking(false);
            socket.connect(new InetSocketAddress("127.0.0.1", 12345));
            socket.register(selector, SelectionKey.OP_CONNECT);

            new Thread(() -> handleWrite(socket)).start();
            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isConnectable()) {
                        if (socket.finishConnect()) {
                            socket.register(selector, SelectionKey.OP_READ);
                        } else {
                            exit(socket);
                        }
                    }
                    if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void handleRead(SelectionKey key) {
        SocketChannel socket = (SocketChannel) key.channel();
        try {
            StringBuilder sb = new StringBuilder();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int len;
            while ((len = socket.read(buffer)) > 0) {
                buffer.flip();
                sb.append(new String(buffer.array(), buffer.position(), buffer.limit()));
                buffer.clear();
            }
            System.out.println(sb);
            if (len == -1) {
                exit(socket);
            }
        } catch (Exception e) {
            e.printStackTrace();
            exit(socket);
        }
    }

    private static void handleWrite(SocketChannel socket) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入您的聊天室昵称：");
            while (true) {
                String msg = scanner.next();
                System.out.println("---------------------------");
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                while (buffer.hasRemaining()) {
                    socket.write(buffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            exit(socket);
        }
    }


    private static void exit(SocketChannel socket) {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("【系统消息】聊天室发生异常");
        } finally {
            System.out.println("【系统消息】你已退出聊天室");
            System.exit(0);
        }
    }

}
