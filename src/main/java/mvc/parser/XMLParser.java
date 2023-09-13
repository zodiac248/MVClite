package mvc.parser;


import java.io.InputStream;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;


public class XMLParser {

  public static String getBasePackage(String xmlFile) {


    SAXReader saxReader = new SAXReader();
    InputStream inputStream =
        XMLParser.class.getClassLoader().getResourceAsStream(xmlFile);
    try {
      Document document = saxReader.read(inputStream);
      Element rootElement = document.getRootElement();
      Element componentScanElement =
          rootElement.element("component-scan");
      Attribute attribute = componentScanElement.attribute("base-package");
      String basePackage = attribute.getText();
      return basePackage;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }


}
