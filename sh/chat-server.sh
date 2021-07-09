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