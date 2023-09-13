package mvc.context;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mvc.annotation.AutoWired;
import mvc.annotation.Controller;
import mvc.annotation.Service;
import mvc.http.RequestType;
import mvc.parser.XMLParser;

public class MyApplicationContext {
  private List<String> classFullPathes = new ArrayList<>();
  private String configFilePath = "";
  public ConcurrentHashMap<String, Object> ioc = new ConcurrentHashMap<>();

  public MyApplicationContext(String configFilePath) {
    this.configFilePath = configFilePath;
  }

  public void init(){
    String basePath = "";
    if(configFilePath.split(":")[0].equals("classpath")){
      basePath = XMLParser.getBasePackage( this.configFilePath.split(":")[1]);

    }
    String[] paths = basePath.split(",");
    for (String path :
        paths) {
      scanPackage(path);
    }
    try {
      createBeans();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  public void scanPackage(String path){
    //convert . to /
    URL url =
        this.getClass().getClassLoader()
            .getResource("/" + path.replaceAll("\\.", "/"));
    String filePath = url.getPath();
    File dir = new File(filePath);
    for (File f: dir.listFiles()){
      if(f.isDirectory()){
        scanPackage(path+"."+f.getName());
      }else {
        classFullPathes.add(path+"."+f.getName().replaceAll(".class",""));
      }
    }
  }

  public void createBeans()
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    if(classFullPathes.size()==0){
      return;
    }
    for (String classFullPath : classFullPathes) {
      Class clazz = Class.forName(classFullPath);
      if (clazz.isAnnotationPresent(Controller.class) || clazz.isAnnotationPresent(Service.class)){
        String beanName = clazz.getSimpleName().substring(0, 1).toLowerCase() +
            clazz.getSimpleName().substring(1);
        ioc.put(beanName,clazz.getConstructor().newInstance());
      }

    }
  }

  public void autoWire() throws IllegalAccessException {
    if(ioc.isEmpty()){
      return;
    }
    for (Map.Entry<String, Object> entry:
    ioc.entrySet()) {
      Object bean = entry.getValue();
      //fetch all fields in the class
      Field[] fields = bean.getClass().getDeclaredFields();
      for (Field field:
      fields) {
        if(field.isAnnotationPresent(AutoWired.class)){
          AutoWired autoWired = field.getAnnotation(AutoWired.class);
          String beanName = autoWired.value();
          //if bean name is not set, then try to find the bean that match the field name
          if(beanName.equals("")){
            beanName = field.getType().getSimpleName().substring(0, 1).toLowerCase()
                +field.getType().getSimpleName().substring(1);
          }
          Object autoWiredBean = ioc.get(beanName);
          if(autoWiredBean == null){
            throw new RuntimeException("bean not found");
          }
          field.setAccessible(true);
          field.set(bean,autoWiredBean);

        }
      }
    }
  }
}
