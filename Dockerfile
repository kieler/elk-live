FROM alpine:latest

LABEL authors="Arnd Plumhoff <plumhoff@email.uni-kiel.de>, Sascha Hoppe <sho@informatik.uni-kiel.de>"

ARG ELKLIVE_UID=1002

RUN apk add --update --no-cache yarn git gradle curl

RUN adduser elklive -h /elklive -D -u ${ELKLIVE_UID}

USER elklive

RUN git clone https://github.com/kieler/elk-live --depth=1 /elklive

WORKDIR "/elklive/client"
RUN yarn install && yarn run build

WORKDIR "/elklive/server"
RUN ./gradlew build

EXPOSE 8080

CMD ["./gradlew", "jettyRun", "--args='-m=SIGTERM'"]
