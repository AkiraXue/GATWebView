# GATWebView
## Android implementation steps
1, add the following codes to your `your project folder/package.json`
```
"dependencies": {
    "react-native-gat-webview":"git+ssh://git@gitlab.wuxingdev.cn:frontend/GATWebView.git#0.13-webview"
  }
```
2, use command
```
$npm install
```
3, add the following codes to your `android/settings.gradle`
```
include ':app', ':react-native-gat-webview'
project(':react-native-gat-webview').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-gat-webview/android')
```
4, edit `android/app/build.gradle` and add the following line inside `dependencies`
```
compile project(':react-native-gat-webview')
```
5, add the following import to `MainApplication.java` of your application

```java
import com.gatwebview.GATWebViewPackage;
```

6, add the following code to add the package to `MainApplication.java`

```java
protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
            new MainReactPackage(),
            new GATWebViewPackage() //<- this
        );
    }
```
7, run `react-native run-android` to see if everything is compilable.

## Usage

just import the module
```js
import GATWebView from 'react-native-gat-webview';
```

#### javascriptInterface
This is Android native `WebView` method , implement `html->javascript` visit `react-native`.

#### onCallBackMessage
It's callback message for  `html->javascript` visit `react-native`.

## Simple Example
`WebViewComponent.js`
```js
'use strict';

import React,{Component} from 'react';
import {
  WebView,
  StyleSheet,
  View,
  Text,
  ToastAndroid,
} from 'react-native';
import GATWebView from 'react-native-gat-webview';
import URI from './webview.html';
class WebViewComponent extends Component{

  _onCallBackMessage(event:Event){
    ToastAndroid.show('Message:'+event,ToastAndroid.SHORT);
  }

  render(){
    return(
      <View style={{flex:1}}>
        <GATWebView
          source={require('./webview.html')}
          javaScriptEnabled={true}
          domStorageEnabled={true}
          startInLoadingState={true}
          javascriptInterface = {{fName:'react'}}
          onCallBackMessage = {this._onCallBackMessage}/>
        </View>
    );
  }

}

export default WebViewComponent;
```
`webview.html`
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
    <style type="text/css">
    .btn{
      margin-top: 100px;
    }
    </style>
</head>
<script  type="text/javascript">
function welcome(){
   //react is GATWebView->javascriptInterface define.
   react.onEventListener('{\'key1\':\'value1\',\'key2\':2}');
}
</script>
<body>
<input type="button" value="touch me" onclick="welcome();" class = "btn">
<br/>
<a href="gatapp://pay?{'status':'9'}">pay</a>
</body>
</html>

```
