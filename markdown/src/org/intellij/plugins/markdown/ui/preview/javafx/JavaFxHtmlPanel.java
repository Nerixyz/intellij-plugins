package org.intellij.plugins.markdown.ui.preview.javafx;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.intellij.markdown.html.HtmlGenerator;
import org.intellij.plugins.markdown.settings.MarkdownApplicationSettings;
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel;
import org.intellij.plugins.markdown.ui.preview.PreviewStaticServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Instantiated by reflection
@SuppressWarnings("unused")
public class JavaFxHtmlPanel extends MarkdownHtmlPanel {

  private static final NotNullLazyValue<String> MY_SCRIPTING_LINES = new NotNullLazyValue<String>() {
    @NotNull
    @Override
    protected String compute() {
      return SCRIPTS.stream()
        .map(s -> "<script src=\"" + PreviewStaticServer.getScriptUrl(s) + "\"></script>")
        .reduce((s, s2) -> s + "\n" + s2)
        .orElseGet(String::new);
    }
  };
  
  @NotNull
  private final JFXPanel myPanel;
  @Nullable
  private WebView myWebView;
  @Nullable
  private String myInlineCss;
  @NotNull
  private String[] myCssUris = ArrayUtil.EMPTY_STRING_ARRAY;
  @NotNull
  private String myCSP = "";
  @NotNull
  private String myLastRawHtml = "";
  @NotNull
  private final ScrollPreservingListener myScrollPreservingListener = new ScrollPreservingListener();
  @NotNull
  private final BridgeSettingListener myBridgeSettingListener = new BridgeSettingListener();

  public JavaFxHtmlPanel() {
    //System.setProperty("prism.lcdtext", "false");
    //System.setProperty("prism.text", "t2k");
    myPanel = new JFXPanelWrapper();

    Platform.runLater(() -> {
      myWebView = new WebView();

      updateFontSmoothingType(myWebView, MarkdownApplicationSettings.getInstance().getMarkdownPreviewSettings().isUseGrayscaleRendering());
      myWebView.setContextMenuEnabled(false);

      final WebEngine engine = myWebView.getEngine();
      engine.getLoadWorker().stateProperty().addListener(myBridgeSettingListener);
      engine.getLoadWorker().stateProperty().addListener(myScrollPreservingListener);

      engine.loadContent("<html><body>" + getScriptingLines() + "</body></html>");

      final Scene scene = new Scene(myWebView);
      myPanel.setScene(scene);
    });

    subscribeForGrayscaleSetting();
  }

  private void subscribeForGrayscaleSetting() {
    MessageBusConnection settingsConnection = ApplicationManager.getApplication().getMessageBus().connect(this);
    MarkdownApplicationSettings.SettingsChangedListener settingsChangedListener =
      new MarkdownApplicationSettings.SettingsChangedListener() {
        @Override
        public void onSettingsChange(@NotNull final MarkdownApplicationSettings settings) {
          Platform.runLater(() -> {
            if (myWebView != null) {
              updateFontSmoothingType(myWebView, settings.getMarkdownPreviewSettings().isUseGrayscaleRendering());
            }
          });
        }
      };
    settingsConnection.subscribe(MarkdownApplicationSettings.SettingsChangedListener.TOPIC, settingsChangedListener);
  }

  private static void updateFontSmoothingType(@NotNull WebView view, boolean isGrayscale) {
    final FontSmoothingType typeToSet;
    if (isGrayscale) {
      typeToSet = FontSmoothingType.GRAY;
    }
    else {
      typeToSet = FontSmoothingType.LCD;
    }
    view.fontSmoothingTypeProperty().setValue(typeToSet);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myPanel;
  }

  @Override
  public void setHtml(@NotNull String html) {
    myLastRawHtml = html;
    final String htmlToRender = prepareHtml(html);

    Platform.runLater(() -> getWebViewGuaranteed().getEngine().loadContent(htmlToRender));
  }

  private String prepareHtml(@NotNull String html) {
    return ImageRefreshFix.setStamps(html
      .replace("<head>", "<head>"
                         + "<meta http-equiv=\"Content-Security-Policy\" content=\"" + myCSP + "\"/>"
                         + getCssLines(myInlineCss, myCssUris) + "\n" + getScriptingLines()));
  }

  @Override
  public void setCSS(@Nullable String inlineCss, @NotNull String... fileUris) {
    myInlineCss = inlineCss;
    myCssUris = fileUris;
    myCSP = PreviewStaticServer.createCSP(ContainerUtil.map(SCRIPTS, s -> PreviewStaticServer.getScriptUrl(s)),
                                          ContainerUtil.concat(
                                            ContainerUtil.map(STYLES, s -> PreviewStaticServer.getStyleUrl(s)),
                                            ContainerUtil.filter(fileUris, s -> s.startsWith("http://") || s.startsWith("https://")),
                                            Stream.of(myInlineCss)
                                              .map(PreviewStaticServer::getCSPHash)
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toList())
                                          ));
    setHtml(myLastRawHtml);
  }

  @Override
  public void render() {
    Platform.runLater(() -> {
      getWebViewGuaranteed().getEngine().reload();
      myPanel.repaint();
    });
  }

  @Override
  public void scrollToMarkdownSrcOffset(final int offset) {
    Platform.runLater(() -> {
      getWebViewGuaranteed().getEngine().executeScript(
        "if ('__IntelliJTools' in window) " +
        "__IntelliJTools.scrollToOffset(" + offset + ", '" + HtmlGenerator.Companion.getSRC_ATTRIBUTE_NAME() + "');"
      );
      final Object result = getWebViewGuaranteed().getEngine().executeScript(
        "document.documentElement.scrollTop || document.body.scrollTop");
      if (result instanceof Number) {
        myScrollPreservingListener.myScrollY = ((Number)result).intValue();
      }
    });
  }

  @Override
  public void dispose() {
    Platform.runLater(() -> {
      getWebViewGuaranteed().getEngine().getLoadWorker().stateProperty().removeListener(myScrollPreservingListener);
      getWebViewGuaranteed().getEngine().getLoadWorker().stateProperty().removeListener(myBridgeSettingListener);
    });
  }

  @NotNull
  private WebView getWebViewGuaranteed () {
    if (myWebView == null) {
      throw new IllegalStateException("WebView should be initialized by now. Check the caller thread");
    }
    return myWebView;
  }

  @NotNull
  private static String getScriptingLines() {
      return MY_SCRIPTING_LINES.getValue();
  }

  public static class JavaPanelBridge {
    static final JavaPanelBridge INSTANCE = new JavaPanelBridge();

    public void openInExternalBrowser(@NotNull String link) {
      SafeOpener.openLink(link);
    }

    public void log(@Nullable String text) {
      Logger.getInstance(JavaPanelBridge.class).warn(text);
    }
  }
  
  private class BridgeSettingListener implements ChangeListener<State> {
    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
        JSObject win
          = (JSObject)getWebViewGuaranteed().getEngine().executeScript("window");
        win.setMember("JavaPanelBridge", JavaPanelBridge.INSTANCE);
    }
  }
  
  private class ScrollPreservingListener implements ChangeListener<State> {
    volatile int myScrollY = 0;

    @Override
    public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
      if (newValue == State.RUNNING) {
        final Object result =
          getWebViewGuaranteed().getEngine().executeScript("document.documentElement.scrollTop || document.body.scrollTop");
        if (result instanceof Number) {
          myScrollY = ((Number)result).intValue();
        }
      }
      else if (newValue == State.SUCCEEDED) {
        getWebViewGuaranteed().getEngine()
          .executeScript("document.documentElement.scrollTop = document.body.scrollTop = " + myScrollY);
      }
    }
  } 
}
