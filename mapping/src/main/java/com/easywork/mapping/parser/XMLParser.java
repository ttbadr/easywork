package com.easywork.mapping.parser;

import com.easywork.mapping.config.MapperConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class XMLParser implements FormatParser {
    @Override
    public Map<String, Object> parse(String input, MapperConfig config) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(input)));
        
        XPath xPath = XPathFactory.newInstance().newXPath();
        Map<String, Object> result = new HashMap<>();
        
        // 遍历所有映射规则
        for (MapperConfig.MappingRule rule : config.getRules()) {
            String sourcePath = rule.getSource();
            if (sourcePath != null) {
                NodeList nodes = (NodeList) xPath.evaluate(sourcePath, doc, XPathConstants.NODESET);
                if (nodes.getLength() > 0) {
                    Node node = nodes.item(0);
                    result.put(rule.getTarget(), node.getTextContent());
                }
            }
        }
        
        return result;
    }
}