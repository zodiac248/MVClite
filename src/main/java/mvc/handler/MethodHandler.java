package mvc.handler;

import java.lang.reflect.Method;

public class MethodHandler {
  private String url;
  private Method method;
  private Object Controller;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Method getMethod() {
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  public Object getController() {
    return Controller;
  }

  public void setController(Object controller) {
    Controller = controller;
  }
}
