# ElkGraph Web

A web page with a text editor for ELK Graph and a synchronized graphical view.

## How To Run

Prerequisite: [npm](https://www.npmjs.com) version 4.x.x

_Note:_ Currently this application depends on the current HEAD of [sprotty](https://github.com/theia-ide/sprotty). Due to [npm#2974](https://github.com/npm/npm/issues/2974), we cannot use a direct GitHub dependency, but need to clone sprotty locally.

```
git clone https://github.com/theia-ide/sprotty.git
cd sprotty/client
npm install
cd ../..

git clone https://github.com/OpenKieler/elkgraph-web.git
cd elkgraph-web/client
npm install
npm run build

cd ../server
./gradlew jettyRun
```

Then point your web browser to `http://0.0.0.0:8080/`
