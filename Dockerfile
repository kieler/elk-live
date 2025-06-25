FROM ubuntu:jammy

LABEL authors="Arnd Plumhoff <plumhoff@email.uni-kiel.de>, Sascha Hoppe <sho@informatik.uni-kiel.de>"

ARG ELKLIVE_HOME=/elklive
ARG ELKLIVE_UID=1000
ARG ELKLIVE_GID=1000

RUN apk add --update --no-cache yarn git gradle curl

RUN addgroup -g ${ELKLIVE_GID} elklive \
 && adduser elklive -h ${ELKLIVE_HOME} -D -u ${ELKLIVE_UID} -G elklive

USER elklive

COPY --chown=elklive:elklive . ${ELKLIVE_HOME}

RUN cd ${ELKLIVE_HOME}/client && yarn install && yarn run build

WORKDIR "${ELKLIVE_HOME}/server"
RUN ./gradlew build

EXPOSE 8080

CMD ["./gradlew", "jettyRun", "--args='-m=SIGTERM'"]
