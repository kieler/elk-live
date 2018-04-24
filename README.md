# ElkGraph Web

A web page with a text editor for ELK Graph and a synchronized graphical view.

## How To Run

Prerequisite: [yarn](https://yarnpkg.com/)

```
git clone https://github.com/OpenKieler/elkgraph-web.git
cd elkgraph-web/client
yarn install
yarn run build

cd ../server
./gradlew jettyRun
```

Then point your web browser to `http://localhost:8080/`
