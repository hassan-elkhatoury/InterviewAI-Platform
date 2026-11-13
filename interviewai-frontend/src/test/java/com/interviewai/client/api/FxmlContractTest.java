package com.interviewai.client.api;

import org.junit.jupiter.api.Test;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.*;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class FxmlContractTest {
    @Test void movedViewsReferenceExistingClientControllersAndEventHandlers() throws Exception {
        var factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        Path views=Paths.get(getClass().getResource("/fxml").toURI());
        try(var files=Files.walk(views)) {
            for(Path path:files.filter(p -> p.toString().endsWith(".fxml")).toList()) {
                var document=factory.newDocumentBuilder().parse(path.toFile());
                String name=document.getDocumentElement().getAttributeNS("http://javafx.com/fxml/1","controller");
                if(name.isBlank()) continue;
                assertTrue(name.startsWith("com.interviewai.client.controller."),path.toString());
                Class<?> controller=Class.forName(name);
                var methods=Arrays.stream(controller.getDeclaredMethods()).map(java.lang.reflect.Method::getName).toList();
                NodeList nodes=document.getElementsByTagName("*");
                for(int i=0;i<nodes.getLength();i++) {
                    NamedNodeMap attributes=nodes.item(i).getAttributes();
                    for(int j=0;j<attributes.getLength();j++) {
                        Node attribute=attributes.item(j);
                        if(attribute.getNodeName().startsWith("on") && attribute.getNodeValue().startsWith("#"))
                            assertTrue(methods.contains(attribute.getNodeValue().substring(1)),path+": "+attribute.getNodeValue());
                    }
                }
            }
        }
    }
}
