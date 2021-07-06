FROM alpine:latest

LABEL maintainer="Arnd Plumhoff <plumhoff@email.uni-kiel.de>"

ARG ELKLIVE_UID=1002

RUN apk update && \
    apk add yarn git gradle

RUN adduser elklive -h /elklive -D -u ${ELKLIVE_UID}

USER elklive

RUN git clone https://github.com/kieler/elk-live --depth=1 /elklive

WORKDIR "/elklive/client"
RUN yarn install && yarn run build

WORKDIR "/elklive/server"
RUN ./gradlew build

EXPOSE 8080

CMD ["./gradlew", "jettyRun", "--args='-m=SIGTERM'"]
