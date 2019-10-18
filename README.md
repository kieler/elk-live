# ElkGraph Web

A web page with a text editor for ELK Graph and a synchronized graphical view.

[Check it out here.](https://rtsys.informatik.uni-kiel.de/elklive/)

Uses:
* [ELK](http://www.eclipse.org/elk)
* [elkjs](https://github.com/OpenKieler/elkjs)
* [sprotty](https://github.com/eclipse/sprotty)

## How To Run

[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/OpenKieler/elkgraph-web)

The easiest way to start working with this project is to open it in [gitpod.io](https://gitpod.io) with the button above. If you would like to compile and run it on your own machine, follow the instructions below.

Prerequisites: [yarn](https://yarnpkg.com/), [Java](https://jdk.java.net)

```
git clone https://github.com/OpenKieler/elkgraph-web.git
cd elkgraph-web/client
yarn install
yarn run build

cd ../server
./gradlew jettyRun
```

Then point your web browser to `http://localhost:8080/`
