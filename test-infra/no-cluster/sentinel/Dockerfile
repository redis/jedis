FROM redis

ARG CONFIG_FILE=sentinel.conf
ARG MASTER_NAME=mymaster
ARG MASTER_HOST
ARG MASTER_PORT=6379
ARG MASTER_AUTH_USER=default
ARG MASTER_AUTH_PASS=foobared
ARG QUORUM=1

COPY $CONFIG_FILE /sentinel.conf
RUN sed -i "s/mymaster/${MASTER_NAME}/g" /sentinel.conf && \
    sed -i "s/^sentinel monitor .*$/sentinel monitor ${MASTER_NAME} ${MASTER_HOST} ${MASTER_PORT} ${QUORUM}/g" /sentinel.conf && \
    sed -i "s/^sentinel auth-user .*$/sentinel auth-user ${MASTER_NAME} ${MASTER_AUTH_USER}/g" /sentinel.conf && \
    sed -i "s/^sentinel auth-pass .*$/sentinel auth-pass ${MASTER_NAME} ${MASTER_AUTH_PASS}/g" /sentinel.conf && \
    chown redis:redis /sentinel.conf

EXPOSE 26379

ENTRYPOINT [ "redis-server" , "/sentinel.conf", "--sentinel" ]
