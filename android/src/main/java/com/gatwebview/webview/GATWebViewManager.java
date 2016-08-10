package com.gatwebview.webview;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.ViewGroup.LayoutParams;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.SystemClock;
import com.facebook.react.common.build.ReactBuildConfig;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.webview.WebViewConfig;
import com.facebook.react.views.webview.events.TopLoadingErrorEvent;
import com.facebook.react.views.webview.events.TopLoadingFinishEvent;
import com.facebook.react.views.webview.events.TopLoadingStartEvent;

import android.content.Intent;
import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mars on 7/26/16 17:34.
 */
public class GATWebViewManager extends SimpleViewManager<WebView>{
    public static final String REACT_CLASS = "GATWebView";
    //callback event name
    private static final String E_NAME = "eName";
    //java script function name
    private static final String F_NAME = "fName";
    //callback field name
    private static final String JS_JSON_NAME = "jsJson";
    private ReactContext mReactContext;
    private static final String HTML_ENCODING = "UTF-8";
    private static final String HTML_MIME_TYPE = "text/html; charset=utf-8";

    private static final String HTTP_METHOD_POST = "POST";

    public static final int COMMAND_GO_BACK = 1;
    public static final int COMMAND_GO_FORWARD = 2;
    public static final int COMMAND_RELOAD = 3;
    public static final int COMMAND_STOP_LOADING = 4;

    // Use `webView.loadUrl("about:blank")` to reliably reset the view
    // state and release page resources (including any running JavaScript).
    private static final String BLANK_URL = "about:blank";

    private WebViewConfig mWebViewConfig;

    public GATWebViewManager() {
        mWebViewConfig = new WebViewConfig() {
            public void configWebView(WebView webView) {
            }
        };
    }

    public GATWebViewManager(WebViewConfig webViewConfig) {
        mWebViewConfig = webViewConfig;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected WebView createViewInstance(ThemedReactContext reactContext) {
        GATWebView webView = new GATWebView(reactContext);
        webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
      }
    });
    reactContext.addLifecycleEventListener(webView);
    mWebViewConfig.configWebView(webView);
    webView.getSettings().setBuiltInZoomControls(true);
    webView.getSettings().setDisplayZoomControls(false);

    // Fixes broken full-screen modals/galleries due to body height being 0.
    webView.setLayoutParams(
            new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        mReactContext = reactContext;
        if (ReactBuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        return webView;
    }

    @ReactProp(name = "javaScriptEnabled")
    public void setJavaScriptEnabled(WebView view, boolean enabled) {
        view.getSettings().setJavaScriptEnabled(enabled);
    }

    @ReactProp(name = "scalesPageToFit")
    public void setScalesPageToFit(WebView view, boolean enabled) {
        view.getSettings().setUseWideViewPort(!enabled);
    }

    @ReactProp(name = "domStorageEnabled")
    public void setDomStorageEnabled(WebView view, boolean enabled) {
        view.getSettings().setDomStorageEnabled(enabled);
    }


    @ReactProp(name = "userAgent")
    public void setUserAgent(WebView view, @javax.annotation.Nullable String userAgent) {
        if (userAgent != null) {
            // TODO(8496850): Fix incorrect behavior when property is unset (uA == null)
            view.getSettings().setUserAgentString(userAgent);
        }
    }

    @ReactProp(name = "mediaPlaybackRequiresUserAction")
    public void setMediaPlaybackRequiresUserAction(WebView view, boolean requires) {
        view.getSettings().setMediaPlaybackRequiresUserGesture(requires);
    }

    @ReactProp(name = "injectedJavaScript")
    public void setInjectedJavaScript(WebView view, @javax.annotation.Nullable String injectedJavaScript) {
        ((GATWebView) view).setInjectedJavaScript(injectedJavaScript);
    }
    @ReactProp(name = "javascriptInterface")
    public void setJavascriptInterface(WebView view, @Nullable ReadableMap source) {
        if (source != null && source.hasKey(F_NAME)) {
            view.addJavascriptInterface(new JavaScriptObject(view), source.getString(F_NAME));
        }
    }

    @ReactProp(name = "source")
    public void setSource(WebView view, @javax.annotation.Nullable ReadableMap source) {
        if (source != null) {
            if (source.hasKey("html")) {
                String html = source.getString("html");
                if (source.hasKey("baseUrl")) {
                    view.loadDataWithBaseURL(
                            source.getString("baseUrl"), html, HTML_MIME_TYPE, HTML_ENCODING, null);
                } else {
                    view.loadData(html, HTML_MIME_TYPE, HTML_ENCODING);
                }
                return;
            }
            if (source.hasKey("uri")) {
                String url = source.getString("uri");
                if (source.hasKey("method")) {
                    String method = source.getString("method");
                    if (method.equals(HTTP_METHOD_POST)) {
                        byte[] postData = null;
                        if (source.hasKey("body")) {
                            String body = source.getString("body");
                            try {
                                postData = body.getBytes("UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                postData = body.getBytes();
                            }
                        }
                        if (postData == null) {
                            postData = new byte[0];
                        }
                        view.postUrl(url, postData);
                        return;
                    }
                }
                HashMap<String, String> headerMap = new HashMap<>();
                if (source.hasKey("headers")) {
                    ReadableMap headers = source.getMap("headers");
                    ReadableMapKeySetIterator iter = headers.keySetIterator();
                    while (iter.hasNextKey()) {
                        String key = iter.nextKey();
                        headerMap.put(key, headers.getString(key));
                    }
                }
                view.loadUrl(url, headerMap);
                return;
            }
        }
        view.loadUrl(BLANK_URL);
    }

    @Override
    protected void addEventEmitters(ThemedReactContext reactContext, WebView view) {
        // Do not register default touch emitter and let WebView implementation handle touches
        view.setWebViewClient(new GATWebViewClient(reactContext));
    }

    @Override
    public @javax.annotation.Nullable
    Map<String, Integer> getCommandsMap() {
        return MapBuilder.of(
                "goBack", COMMAND_GO_BACK,
                "goForward", COMMAND_GO_FORWARD,
                "reload", COMMAND_RELOAD,
                "stopLoading", COMMAND_STOP_LOADING);
    }

    @Override
    public void receiveCommand(WebView root, int commandId, @javax.annotation.Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_GO_BACK:
                root.goBack();
                break;
            case COMMAND_GO_FORWARD:
                root.goForward();
                break;
            case COMMAND_RELOAD:
                root.reload();
                break;
            case COMMAND_STOP_LOADING:
                root.stopLoading();
                break;
        }
    }

    @Override
    public void onDropViewInstance(WebView webView) {
        super.onDropViewInstance(webView);
        ((ThemedReactContext) webView.getContext()).removeLifecycleEventListener((GATWebView) webView);
        ((GATWebView) webView).cleanupCallbacksAndDestroy();
    }



    private static class GATWebViewClient extends WebViewClient {

        private ReactContext mReactContext;
        public GATWebViewClient(ReactContext reactContext){
            this.mReactContext = reactContext;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
          if (url.startsWith("http://") || url.startsWith("https://")) {
         return false;
       }else if(url.contains("gatapp://")){
           WritableMap event = Arguments.createMap();
           event.putString(JS_JSON_NAME, url.substring(url.indexOf("//")+2));
           mReactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                   view.getId(),
                   "topChange",
                   event);
           return true;
       } else {
         Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         view.getContext().startActivity(intent);
         return true;
       }

        }

        private boolean mLastLoadFailed = false;

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);

            if (!mLastLoadFailed) {
                GATWebView gatWebView = (GATWebView) webView;
                gatWebView.callInjectedJavaScript();
                emitFinishEvent(webView, url);
            }
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap favicon) {
            super.onPageStarted(webView, url, favicon);
            mLastLoadFailed = false;
            dispatchEvent(
                    webView,
                    new TopLoadingStartEvent(
                            webView.getId(),
                            SystemClock.nanoTime(),
                            createWebViewEvent(webView, url)));
        }

        @Override
        public void onReceivedError(
                WebView webView,
                int errorCode,
                String description,
                String failingUrl) {
            super.onReceivedError(webView, errorCode, description, failingUrl);
            mLastLoadFailed = true;

            // In case of an error JS side expect to get a finish event first, and then get an error event
            // Android WebView does it in the opposite way, so we need to simulate that behavior
            emitFinishEvent(webView, failingUrl);

            WritableMap eventData = createWebViewEvent(webView, failingUrl);
            eventData.putDouble("code", errorCode);
            eventData.putString("description", description);

            dispatchEvent(
                    webView,
                    new TopLoadingErrorEvent(webView.getId(), SystemClock.nanoTime(), eventData));
        }

        @Override
        public void doUpdateVisitedHistory(WebView webView, String url, boolean isReload) {
            super.doUpdateVisitedHistory(webView, url, isReload);

            dispatchEvent(
                    webView,
                    new TopLoadingStartEvent(
                            webView.getId(),
                            SystemClock.nanoTime(),
                            createWebViewEvent(webView, url)));
        }

        private void emitFinishEvent(WebView webView, String url) {
            dispatchEvent(
                    webView,
                    new TopLoadingFinishEvent(
                            webView.getId(),
                            SystemClock.nanoTime(),
                            createWebViewEvent(webView, url)));
        }

        private static void dispatchEvent(WebView webView, Event event) {
            ReactContext reactContext = (ReactContext) webView.getContext();
            EventDispatcher eventDispatcher =
                    reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();
            eventDispatcher.dispatchEvent(event);
        }

        private WritableMap createWebViewEvent(WebView webView, String url) {
            WritableMap event = Arguments.createMap();
            event.putDouble("target", webView.getId());
            // Don't use webView.getUrl() here, the URL isn't updated to the new value yet in callbacks
            // like onPageFinished
            event.putString("url", url);
            event.putBoolean("loading", !mLastLoadFailed && webView.getProgress() != 100);
            event.putString("title", webView.getTitle());
            event.putBoolean("canGoBack", webView.canGoBack());
            event.putBoolean("canGoForward", webView.canGoForward());
            return event;
        }

    }

    /**
     * Subclass of {@link WebView} that implements {@link LifecycleEventListener} interface in order
     * to call {@link WebView#destroy} on activty destroy event and also to clear the client
     */
    private static class GATWebView extends WebView implements LifecycleEventListener {
        private @javax.annotation.Nullable
        String injectedJS;

        /**
         * WebView must be created with an context of the current activity
         *
         * Activity Context is required for creation of dialogs internally by WebView
         * Reactive Native needed for access to ReactNative internal system functionality
         *
         */
        public GATWebView(ThemedReactContext reactContext) {
            super(reactContext);
        }

        @Override
        public void onHostResume() {
            // do nothing
        }

        @Override
        public void onHostPause() {
            // do nothing
        }

        @Override
        public void onHostDestroy() {
            cleanupCallbacksAndDestroy();
        }

        public void setInjectedJavaScript(@javax.annotation.Nullable String js) {
            injectedJS = js;
        }

        public void callInjectedJavaScript() {
            if (getSettings().getJavaScriptEnabled() &&
                    injectedJS != null &&
                    !TextUtils.isEmpty(injectedJS)) {
                loadUrl("javascript:(function() {\n" + injectedJS + ";\n})();");
            }
        }

        private void cleanupCallbacksAndDestroy() {
            setWebViewClient(null);
            destroy();
        }
    }

    /**
     *
     */
    public static class JavaScriptObject {
        WebView mWebView;

        public JavaScriptObject(WebView webView) {
            this.mWebView = webView;
        }

        @JavascriptInterface
        public void onEventListener() {
            WritableMap event = Arguments.createMap();
            ReactContext reactContext = (ReactContext)mWebView.getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    mWebView.getId(),
                    "topChange",
                    event);
        }

        @JavascriptInterface
        public void onEventListener(String json) {

            WritableMap event = Arguments.createMap();
            event.putString(JS_JSON_NAME, json);
            ReactContext reactContext = (ReactContext)mWebView.getContext();
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    mWebView.getId(),
                    "topChange",
                    event);
        }

    }

}
