FROM alpine:latest

LABEL maintainer="Arnd Plumhoff <plumhoff@email.uni-kiel.de>"

ARG ELKWEB_UID=1002

RUN apk update && \
    apk add yarn git gradle

RUN adduser elkweb -h /elkweb -D -u ${ELKWEB_UID}

USER elkweb

RUN git clone https://github.com/OpenKieler/elkgraph-web.git --depth=1 /elkweb

WORKDIR "/elkweb/client"
RUN yarn install && yarn run build

WORKDIR "/elkweb/server"
RUN ./gradlew build

EXPOSE 8080

<<<<<<< HEAD
=======
WORKDIR "/elkweb/server"
>>>>>>> 16f21b37cb29a1e1f161fef3f95b9b1b43d37361
CMD ["./gradlew", "jettyRun", "--args='-m=SIGTERM'"]
