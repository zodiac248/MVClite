package mvc.servlet;

import com.google.gson.Gson;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import mvc.annotation.Controller;
import mvc.annotation.RequestMapping;
import mvc.annotation.RequestParam;
import mvc.annotation.ResponseBody;
import mvc.context.MyApplicationContext;
import mvc.handler.APIHandler;

public class DispatchServlet extends HttpServlet {
  private HashMap<String, APIHandler> router;
  MyApplicationContext myApplicationContext;
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    this.doPost(request,response);

  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    executeDispatch(request, response);


  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    String configLocation =
        config.getInitParameter("contextConfigLocation");
    myApplicationContext = new MyApplicationContext(configLocation);
    myApplicationContext.init();
    ConcurrentHashMap<String, Object> ioc = myApplicationContext.ioc;
    if(ioc.isEmpty()){
      return;
    }
    for (Entry<String, Object> entry : ioc.entrySet()) {
      Class clazz = entry.getValue().getClass();
        if(clazz.isAnnotationPresent(Controller.class)){
        Method[] methods = clazz.getMethods();
          for (Method method : methods) {
            if(method.isAnnotationPresent(RequestMapping.class)){
              RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
              String url = requestMapping.value();
              String key = url+"_"+ requestMapping.method();
              APIHandler apiHandler = new APIHandler(key,method,entry.getValue());
              router.put(key,apiHandler);
            }
          }
      }
    }
  }

  private void executeDispatch(HttpServletRequest request,
      HttpServletResponse response) {

    APIHandler hspHandler = this.router.get(request.getMethod()+"_"+request.getRequestURI());
    try {
      if (null == hspHandler) {//说明用户请求的路径/资源不存在
        response.getWriter().print("<h1>404 NOT FOUND</h1>");
      } else {

        Class<?>[] parameterTypes =
            hspHandler.getMethod().getParameterTypes();

        //2. 创建一个参数数组[对应实参数组], 在后面反射调用目标方法时，会使用到
        Object[] params =
            new Object[parameterTypes.length];

        //3遍历parameterTypes形参数组,根据形参数组信息，将实参填充到实参数组

        for (int i = 0; i < parameterTypes.length; i++) {
          Class<?> parameterType = parameterTypes[i];

          if ("HttpServletRequest".equals(parameterType.getSimpleName())) {
            params[i] = request;
          } else if ("HttpServletResponse".equals(parameterType.getSimpleName())) {
            params[i] = response;
          }
        }

        request.setCharacterEncoding("utf-8");
        Map<String, String[]> parameterMap =
            request.getParameterMap();

        //2. 遍历parameterMap 将请求参数，按照顺序填充到实参数组params
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {

          //取出key，这name就是对应请求的参数名
          String name = entry.getKey();

          String value = entry.getValue()[0];

          int indexRequestParameterIndex =
              getParameterIndex(hspHandler.getMethod(), name);
          if (indexRequestParameterIndex != -1) {//找到对应的位置
            params[indexRequestParameterIndex] = value;
          } else {//说明并没有找到@RequestParam注解对应的参数,就会使用默认的机制进行配置[待..]

            List<String> parameterNames =
                getParameterNames(hspHandler.getMethod());
            for (int i = 0; i < parameterNames.size(); i++) {
              //如果请求参数名和目标方法的形参名一样，说明匹配成功
              if (name.equals(parameterNames.get(i))) {
                params[i] = value;//填充到实参数组
                break;
              }
            }
          }
        }


        Object result = hspHandler.getMethod().invoke(hspHandler.getController(), params);

        Method method = hspHandler.getMethod();
        if(method.isAnnotationPresent(ResponseBody.class)) {
          response.setContentType("application/json;charset=utf-8");
          Gson gson = new Gson();
          PrintWriter writer = response.getWriter();

          if (result instanceof ArrayList) {
            writer.write(gson.toJson(result));
          } else if (result instanceof Object) {
            writer.write(gson.toJson(result));
          }

          writer.flush();
          writer.close();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public int getParameterIndex(Method method, String name) {

    //1.得到method的所有形参参数
    Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      //取出当前的形参参数
      Parameter parameter = parameters[i];
      //判断parameter是不是有@RequestParam注解
      boolean annotationPresent = parameter.isAnnotationPresent(RequestParam.class);
      if (annotationPresent) {//说明有@RequestParam
        //取出当前这个参数的 @RequestParam(value = "xxx")
        RequestParam requestParamAnnotation =
            parameter.getAnnotation(RequestParam.class);
        String value = requestParamAnnotation.value();
        //这里就是匹配的比较
        if (name.equals(value)) {
          return i;
        }
      }
    }
    //如果没有匹配成功，就返回-1
    return -1;
  }

  //编写方法, 得到目标方法的所有形参的名称,并放入到集合中返回

  /**
   * @param method 目标方法
   * @return 所有形参的名称, 并放入到集合中返回
   */
  public List<String> getParameterNames(Method method) {

    List<String> parametersList = new ArrayList<>();
    //获取到所以的参数名->这里有一个小细节
    //在默认情况下 parameter.getName() 得到的名字不是形参真正名字
    //而是 [arg0, arg1, arg2...], 这里我们要引入一个插件，使用java8特性，这样才能解决
    Parameter[] parameters = method.getParameters();
    //遍历parameters 取出名称，放入parametersList
    for (Parameter parameter : parameters) {
      parametersList.add(parameter.getName());
    }
    return parametersList;
  }
}
