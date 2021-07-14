package com.herenpeng.chat.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * 聊天室客户端
 *
 * @author herenpeng
 * @since 2021-07-09 12:00:00
 */
public class ChatClient {

    public static void main(String[] args) {
        try (Selector selector = Selector.open();
             SocketChannel socket = SocketChannel.open()) {
            //连接服务端socket
            socket.connect(new InetSocketAddress("127.0.0.1", 12345));

            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isConnectable()) {
                        socket.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));
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
        try {
            StringBuilder sb = new StringBuilder();
            SocketChannel socket = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            while (socket.read(buffer) != -1) {
                buffer.flip();
                sb.append(new String(buffer.array(), buffer.position(), buffer.limit()));
                buffer.clear();
            }
            System.out.println(sb.toString());
        } catch (Exception e) {
            System.out.println("【系统消息】你已退出聊天室，开始认真工作吧");
            System.exit(0);
        }
    }
}
