FROM openjdk:16-alpine
ARG workdir=/usr/rich-rss
WORKDIR $workdir

ARG CORE_VERSION
ARG GIT_HASH
ENV spring_profiles_active="prod" \
    AUTH_TOKEN_ANONYMOUS_VALIDFORDAYS="" \
    PUPPETEER_HOST="" \
    LOG_LEVEL="error" \
    CORE_VERSION=$CORE_VERSION \
    MASTER_URL="https://richrss.migor.org" \
    GIT_HASH=$GIT_HASH \
    ENABLE_FULLTEXT="" \
    MAX_RECOVERY_PER_FEED="4" \
    PUPPETEER_TIMEOUT_MS="10000"

EXPOSE 8080
RUN addgroup -g 1000 java && \
    adduser -u 1000 -G java -s /bin/sh -D java
COPY --chown=java:java docker-entrypoint.sh ./
COPY --chown=java:java build/libs/app.jar app.jar
RUN mkdir static && \
    echo "serve static assets <a href=\"https://docs.docker.com/engine/reference/commandline/run/#mount-volume--v---read-only\" target=\"_blank\">by mounting them</a> in $workdir/static" > static/index.html && \
    chmod -R g-w . && \
    chmod -R o= . && \
    chown -R java .
USER java
# use supervisord https://gdevillele.github.io/engine/admin/using_supervisord/
CMD ["sh", "./docker-entrypoint.sh"]
