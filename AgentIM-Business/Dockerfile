FROM bellsoft/liberica-openjdk-rocky:21.0.8-cds

LABEL maintainer="Lion Li"

RUN mkdir -p /ruoyi/business/logs  \
    /ruoyi/business/temp  \
    /ruoyi/skywalking/agent

WORKDIR /ruoyi/business

ENV SERVER_PORT=9220 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./target/AgentIM-Business.jar ./app.jar

SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           #-Dskywalking.agent.service_name=agentim-business \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
