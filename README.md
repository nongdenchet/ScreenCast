# ScreenCast
Screen cast android app to server and render on web

### For the client
Make sure you change the server url in `ImageStreamer.kt`

```kotlin
socket = IO.socket("http://10.0.2.2:3000")
```

### For the server
1. Run `cd server`
2. Run `npm install`
3. Run `npm start` and go to `localhost:3000` for the result

### Demo
![Alt Text](https://github.com/nongdenchet/ScreenCast/blob/master/sample.gif)
