FROM alpine:3.19

LABEL authors="Arnd Plumhoff <plumhoff@email.uni-kiel.de>, Sascha Hoppe <sho@informatik.uni-kiel.de>"

ARG ELKLIVE_UID=1002
ARG ELKLIVE_GID=1002

RUN apk add --update --no-cache yarn git gradle curl

RUN addgroup -g ${ELKLIVE_GID} elklive \
 && adduser elklive -h /elklive -D -u ${ELKLIVE_UID} -G elklive

USER elklive

COPY --chown=elklive:elklive . /elklive

WORKDIR "/elklive/client"
RUN yarn install && yarn run build

WORKDIR "/elklive/server"
RUN ./gradlew build

EXPOSE 8080

CMD ["./gradlew", "jettyRun", "--args='-m=SIGTERM'"]
