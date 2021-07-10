CHAT_SERVER_DIR=/usr/web-project/chat
CHAT_SERVER=ChatServer
CHAT_LOG_FILE=${CHAT_SERVER_DIR}/chat.log
# 聊天室启动参数
CHAT_CFG=${2}

help() {
	echo "=================="
	echo "start 启动服务"
	echo "stop 停止服务"
	echo "restart 重启服务"
	echo "find 查找服务"
	echo "help 帮助"
	echo "=================="
}

start() {
	javac -encoding UTF-8 ${CHAT_SERVER}\.java
	nohup java -Dfile.encoding=UTF-8 ${CHAT_SERVER} ${CHAT_CFG} >>${CHAT_LOG_FILE} 2>&1 &
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

find() {
  PID=$(ps -ef | grep java | grep ChatServer | awk '{print $2}')
  if [ "${PID}" == "" ]
	then
		echo "服务${CHAT_SERVER}已停止"
	else
		echo "服务${CHAT_SERVER}正在运行：PID=${PID}"
	fi
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
  find)
    find
    ;;
	*)
		help
		;;
esac

exit 0