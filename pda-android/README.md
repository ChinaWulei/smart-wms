# Smart WMS PDA Android

这是原生 Android PDA App，不是 WebView 壳。

当前实现：

- 原生登录页
- 原生 Shipping Job 列表
- 原生扫货页面
- 原生“全部上车”操作
- 直接调用后端 API

## 默认后端地址

当前默认请求：

```text
http://10.0.2.2:8080/api
```

这个地址适合 Android 模拟器访问宿主机后端。

真机 PDA 使用时，需要修改：

```text
app/build.gradle
```

把：

```gradle
buildConfigField "String", "API_BASE_URL", "\"http://10.0.2.2:8080/api\""
```

改成仓库服务器内网后端地址，例如：

```gradle
buildConfigField "String", "API_BASE_URL", "\"http://192.168.1.20:8080/api\""
```

## 打包 APK

用 Android Studio 打开 `pda-android` 目录，然后：

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

生成的 APK 通常在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 后端依赖

先启动后端服务：

```powershell
docker compose up --build
```

真机 PDA 要访问电脑或服务器的局域网 IP，例如：

```text
http://192.168.1.20:8080/api
```
