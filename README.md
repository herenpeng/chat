# chat 聊天室

> 作者：herenpeng
>
> 一个基于 Java 网络编程功能编写的简易的即时通讯工具，可以直接在命令行中进行网络通讯。

## 聊天室申明

- 1、本聊天室仅为娱乐，请勿在该聊天室内谈论敏感内容，比如涉政，涉黄，账号密码等等！

- 2、聊天室内容明文传输，聊天信息泄露本聊天室概不负责！

- 3、本聊天室内容后台不做任何存储，聊天信息如果需要请自行保留！

- 4、最终解释权归程序作者及本聊天室所有！

## chat 聊天室使用

- 1、启动 `com.herenpeng.chat.ChatServer` 类 `main()` 方法，程序服务端启动。

- 2、启动 `com.herenpeng.chat.ChatClient` 类 `main()` 方法，程序客户端启动。

> chat 聊天室服务端服务必须启动，客户端服务可以启动多个，多个客户端服务之间可以相互通讯。

## chat 服务端服务

> chat 服务端服务，即 `com.herenpeng.chat.ChatServer` 类，可以将该 Java 类部署到服务器上。

### 部署服务器要求

1、JDK8 环境

2、良好的网络

### 部署服务器步骤

1、将 `ChatServer.java` 文件上传至服务器上。

> 如：将 `ChatServer.java` 文件上传至服务器 `/usr/web-project/chat` 目录下。

2、进入 `ChatServer.java` 文件所在的目录，编写部署脚本，并保存。

```shell script
cd /usr/web-project/chat
vim chat-server.sh
```
- chat-server.sh 内容

```shell script
CHAT_SERVER_DIR=/usr/web-project/chat
CHAT_SERVER=ChatServer
CHAT_LOG_FILE=${CHAT_SERVER_DIR}/chat.log
# 机器人启动参数
CHAT_ARGS_ROBOT=${2}

help() {
	echo "=================="
	echo "start 启动服务"
	echo "stop 停止服务"
	echo "restart 重启服务"
	echo "help 帮助"
	echo "=================="
}

start() {
	javac -encoding UTF-8 ${CHAT_SERVER}\.java
	if [ "${CHAT_ARGS_ROBOT}" == "" -o "${CHAT_ARGS_ROBOT}" != "robot" ]
	then
		nohup java -Dfile.encoding=UTF-8 ${CHAT_SERVER} >>${CHAT_LOG_FILE} 2>&1 &
	else
		echo "机器人已开启"
		nohup java -Dfile.encoding=UTF-8 ${CHAT_SERVER} robot >>${CHAT_LOG_FILE} 2>&1 &
	fi
	echo "服务${CHAT_SERVER}已启动"
}

stop() {
	PID=$(ps -ef | grep java | grep ChatServer | awk '{print $2}')
	if [ "${PID}" == "" ]
	then
		echo "服务${CHAT_SERVER}已停止"
	else
		kill ${PID}
		echo "服务${CHAT_SERVER}已停止"
	fi
}

restart() {
	start
	sleep 3
	stop
	echo "服务${CHAT_SERVER}已重启"
}

case ${1} in
	"")
		echo "=== 参数错误 ==="
		;;
	start)
		start
		;;
	stop)
		stop
		;;
	restart)
		restart
		;;
	*)
		help
		;;
esac

exit 0
```

3、给 `chat-server.sh` 脚本增加可执行权限，并执行脚本，启动服务

```shell script
# 增加可执行权限
cd /usr/web-project/chat
chmod +x chat-server.sh
# 启动服务
./chat-server.sh start robot
```

### Error: Could not find or load main class ChatServer

执行脚本启动服务的时候，可能会报一个 `Error: Could not find or load main class ChatServer` 的错误信息。

这是因为在 `ChatServer.java` 文件的最上方有一个包名，如果独立运行 Java 文件，将该包名去掉即可解决上述报错信息。

> 包名：package com.herenpeng.chat;

## chat 聊天室配置刷新

> chat 聊天室的配置可以在服务端启动时进行指定，比如 robot 配置，开启该配置可以启动服务端机器人。
>
> 但是在服务启动之后，如果需要在不关闭服务的情况下进行配置刷新，则需要使用 chat 聊天室 reloadChatCfg 功能。使用该功能，可以在不关闭服务的情况下，对服务端的配置进行动态修改。

### 配置刷新步骤

1、启动客户端，在聊天室昵称中输入 `@CHAT_CFG_RELOAD@`。

2、在 `请输入需要刷新的聊天室配置` 提示语下输入配置。

> 配置使用 `key1=value1&key2=value2` 的格式。
>
> 例如需要关闭聊天室机器人，可以使用配置 `robot=false` 进行配置刷新。
> 如果需要调整聊天室机器人的回复概率，可以使用配置 `robotPro=10`，该配置表示机器人回复的概率为 `1/10`。
> 上述两个配置需要一起刷新，可以使用 `robot=false&robotPro=10` 进行刷新。
