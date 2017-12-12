# ScreenCast
Screen cast android app to server and render on web

### For the client
Make sure you change the server url in `ImageStreamer.kt`

```kotlin
socket = IO.socket("http://192.168.1.108:3000")
```

### For the server
1. Run `cd server`
2. Run `npm install`
3. Run `node index.js` and goto `localhost:3000` for the result

![Alt Text](https://github.com/nongdenchet/ScreenCast/blob/master/sample.gif)
