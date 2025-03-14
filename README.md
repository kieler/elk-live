# ElkGraph Web

A web page with a text editor for ELK Graph and a synchronized graphical view.

[Check it out here.](https://rtsys.informatik.uni-kiel.de/elklive/)

Uses:

- [ELK](http://www.eclipse.org/elk)
- [elkjs](https://github.com/OpenKieler/elkjs)
- [sprotty](https://github.com/eclipse/sprotty)

## How to Run

### Gitpod

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/OpenKieler/elkgraph-web)

The easiest way to start working with this project is to open it in
[gitpod.io](https://gitpod.io) with the button above.

### Build Locally

If you would like to compile and run it on your own machine, follow the instructions below.

Prerequisites: [yarn](https://yarnpkg.com/), [Java](https://jdk.java.net)

```
git clone https://github.com/kieler/elk-live.git
cd elk-live/client
yarn install
yarn run build

cd ../server
./gradlew jettyRun
```

Then point your web browser to http://localhost:8080/

This project provides a container based runtime environment for the
[elk-live](https://github.com/kieler/elk-live) project.

## Docker

You can build and/or run images with [Docker](https://www.docker.com).

### Pre-Built Docker Image

You can start a container with a pre-built image by running `docker run -p 8080:8080 ghcr.io/kieler/elk-live:master`.
After the container is fully booted, you can visit http://localhost:8080/ in your browser.

### Local Image Build

You can build a container image locally by using the provided `Dockerfile` or, more conveniently, use the provided `docker-compose.yml` file.
This allows you to customize the runtime environment locally and even actively develop with nicely separated build and runtime dependencies.

To easily get started with a locally built image, follow the steps below

```terminal
cp .env.example .env
vi .env # adapt settings via provided environment
docker-compose up --build
```

## Hosting
If you would like to host elk-live yourself you can use the automatically built [Docker container](https://github.com/kieler/elk-live/pkgs/container/elk-live). There is also an [example configuration](https://github.com/kieler/elk-live/blob/master/docker-compose.yml) for Docker Compose.

## How to Release

To release a new version simply [add a new release](https://github.com/kieler/elk-live/releases/new) and tag it (e.g. with ` v0.1.1`).

This will build the selected branch and docker image, which will be tagged as `latest`.
