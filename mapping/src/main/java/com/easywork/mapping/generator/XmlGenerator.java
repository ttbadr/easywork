package com.easywork.mapping.generator;

import com.easywork.mapping.config.MapperConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlGenerator implements FormatGenerator {

    @Override
    public String generate(Map<String, Object> data, MapperConfig config) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        // 启用命名空间支持，以便使用 DOM Level 2 的命名空间 API
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // 对XPath进行排序，有助于按父->子的顺序创建节点
        List<String> sortedXPaths = new ArrayList<>(data.keySet());
        Collections.sort(sortedXPaths);

        for (String targetXPath : sortedXPaths) {
            Object value = data.get(targetXPath);
            applyXPathValue(doc, targetXPath, value, config);
        }

        // 将Document对象转换为XML字符串
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 格式化输出
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); // 缩进量
        // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); // 如果不需要XML声明

        // Apply namespaces after all nodes are created and values set
        if (config != null && config.getNamespaces() != null && !config.getNamespaces().isEmpty()) {
            applyNamespaces(doc, config);
        }

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private Element findElementByPath(Node rootNode, String xpathStr) {
        if (xpathStr == null || xpathStr.trim().isEmpty() || !xpathStr.startsWith("/")) {
            return null;
        }

        Node currentNode = rootNode;
        String pathSegment = xpathStr.substring(1); // Remove leading "/"

        // If the path starts with the rootNode's name, and rootNode is indeed the document root,
        // then we adjust the pathSegment to be relative to this rootNode.
        // This handles cases where xpathStr is like "/root/child" and rootNode is the "root" element.
        if (rootNode == rootNode.getOwnerDocument().getDocumentElement() && pathSegment.startsWith(rootNode.getNodeName())) {
            if (pathSegment.equals(rootNode.getNodeName())) { // Path is just "/rootName", target is rootNode itself
                return (Element) rootNode;
            } else if (pathSegment.startsWith(rootNode.getNodeName() + "/")) {
                pathSegment = pathSegment.substring(rootNode.getNodeName().length() + 1);
            } else {
                // Path like "/rootNameSomethingElse" where rootNode is "rootName" - this is not a child of rootNode with that name.
                return null;
            }
        }
        
        // If pathSegment is now empty, it means the original path was referring to the rootNode itself (e.g. "/" or "/rootName" after stripping).
        if (pathSegment.isEmpty()) {
             // This condition also catches xpathStr = "/" if rootNode is documentElement.
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) currentNode;
            }
            return null;
        }

        String[] parts = pathSegment.split("/");
        for (String part : parts) {
            // An empty part can occur if pathSegment became empty and was split (e.g. "/") or due to "//"
            if (part.isEmpty()) {
                // If parts has only one empty string, it means pathSegment was empty (e.g. original xpathStr was "/")
                // and we should have returned currentNode if it's an element (handled by the block above).
                // If there are multiple parts and one is empty, it's an invalid path like /a//b
                return null; 
            }
            if (part.startsWith("@")) { // Attributes or empty parts not handled here
                return null;
            }
            String elementName = part.replaceAll("\\[.*\\]", ""); // Basic predicate stripping
            NodeList children = currentNode.getChildNodes();
            Node nextNode = null;
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getLocalName().equals(elementName)) {
                    nextNode = child;
                    break;
                }
            }
            if (nextNode == null) {
                return null; // Path does not exist
            }
            currentNode = nextNode;
        }

        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
            return (Element) currentNode;
        }
        return null;
    }

    private String getElementPath(Element element) {
        StringBuilder path = new StringBuilder();
        Node current = element;
        while (current != null && current.getNodeType() == Node.ELEMENT_NODE) {
            String nodeName = current.getNodeName();
            // 移除前缀（如果有）
            if (nodeName.contains(":")) {
                nodeName = nodeName.substring(nodeName.indexOf(":") + 1);
            }
            path.insert(0, nodeName);
            path.insert(0, "/");
            current = current.getParentNode();
        }
        return path.toString();
    }

    private void applyNamespaces(Document doc, MapperConfig config) {
        List<MapperConfig.Namespaces> namespaces = config.getNamespaces();
        if (namespaces == null) return;

        // 遍历所有命名空间配置
        for (MapperConfig.Namespaces nsConfig : namespaces) {
            String uri = nsConfig.getUri();
            String prefix = nsConfig.getPrefix();
            String defineNsIn = nsConfig.getDefineNsIn();
            String applyPrefixTo = nsConfig.getApplyPrefixTo();

            if (uri == null || defineNsIn == null || defineNsIn.trim().isEmpty()) continue;

            // 找到需要定义命名空间的元素
            Element targetElement = findElementByPath(doc.getDocumentElement(), defineNsIn);
            if (targetElement == null && "/".equals(defineNsIn)) {
                targetElement = doc.getDocumentElement();
            }
            if (targetElement == null) continue;

            // 设置命名空间声明
            if (prefix == null || prefix.trim().isEmpty()) {
                targetElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", uri);
            } else {
                targetElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + prefix.trim(), uri);
            }

            // 如果需要应用前缀
            if (applyPrefixTo != null && !applyPrefixTo.trim().isEmpty() && prefix != null && !prefix.trim().isEmpty()) {
                Element startElement = findElementByPath(doc.getDocumentElement(), applyPrefixTo);
                if (startElement == null && "/".equals(applyPrefixTo)) {
                    startElement = doc.getDocumentElement();
                }
                if (startElement != null) {
                    // 获取当前元素的路径
                    String elementPath = getElementPath(startElement);
                    
                    // 找到最匹配的命名空间配置
                    MapperConfig.Namespaces bestMatch = null;
                    int maxMatchLength = -1;
                    
                    for (MapperConfig.Namespaces ns : namespaces) {
                        String nsPath = ns.getApplyPrefixTo();
                        if (nsPath != null && !nsPath.isEmpty()) {
                            if (elementPath.equals(nsPath) || elementPath.startsWith(nsPath + "/")) {
                                int matchLength = nsPath.length();
                                if (matchLength > maxMatchLength) {
                                    maxMatchLength = matchLength;
                                    bestMatch = ns;
                                }
                            }
                        }
                    }
                    
                    // 应用最匹配的命名空间
                    if (bestMatch != null) {
                        applyNamespaceToElement(startElement, bestMatch.getUri(), bestMatch.getPrefix().trim(), doc, config);
                    } else if ("/".equals(applyPrefixTo)) {
                        // 如果没有更具体的匹配，且当前是根路径配置，则应用当前命名空间
                        applyNamespaceToElement(startElement, uri, prefix.trim(), doc, config);
                    }
                }
            }
        }
    }

    private void applyNamespaceToElement(Element element, String namespaceUri, String prefix, Document doc, MapperConfig config) {
        // 创建新元素，使用命名空间 URI
        String localName = element.getNodeName().contains(":") ?
            element.getNodeName().substring(element.getNodeName().indexOf(":") + 1) :
            element.getNodeName();
        Element newElement = doc.createElementNS(namespaceUri, prefix + ":" + localName);

        // 首先添加命名空间声明
        org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            if (attrName.startsWith("xmlns")) {
                newElement.setAttribute(attrName, attrValue);
            }
        }

        // 然后添加其他属性
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getNodeName();
            String attrValue = attr.getNodeValue();

            if (!attrName.startsWith("xmlns")) {
                // 为属性添加命名空间
                String attrLocalName = attrName.contains(":") ?
                    attrName.substring(attrName.indexOf(":") + 1) : attrName;
                if (!attrName.contains(":")) {
                    // 如果属性没有前缀，添加前缀
                    newElement.setAttributeNS(namespaceUri, prefix + ":" + attrLocalName, attrValue);
                } else {
                    // 如果属性已经有前缀，保持原样
                    newElement.setAttribute(attrName, attrValue);
                }
            }
        }

        // 处理子节点
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) child;
                String childLocalName = childElement.getNodeName().contains(":") ?
                    childElement.getNodeName().substring(childElement.getNodeName().indexOf(":") + 1) :
                    childElement.getNodeName();
                
                // 检查子元素是否已经有命名空间
                String childPrefix = childElement.getNodeName().contains(":") ?
                    childElement.getNodeName().substring(0, childElement.getNodeName().indexOf(":")) : null;
                
                // 获取子元素的路径
                String childPath = getElementPath(childElement);
                
                // 查找子元素的命名空间配置
                MapperConfig.Namespaces childNs = null;
                int maxMatchLength = -1;
                
                for (MapperConfig.Namespaces ns : config.getNamespaces()) {
                    String nsPath = ns.getApplyPrefixTo();
                    if (nsPath != null && !nsPath.isEmpty()) {
                        if (childPath.equals(nsPath) || childPath.startsWith(nsPath + "/")) {
                            int matchLength = nsPath.length();
                            if (matchLength > maxMatchLength) {
                                maxMatchLength = matchLength;
                                childNs = ns;
                            }
                        }
                    }
                }
                
                Element newChild;
                if (childNs != null) {
                    // 使用子元素的命名空间
                    newChild = doc.createElementNS(childNs.getUri(), childNs.getPrefix().trim() + ":" + childLocalName);
                } else {
                    // 如果没有特定的命名空间配置，使用父元素的命名空间
                    newChild = doc.createElementNS(namespaceUri, prefix + ":" + childLocalName);
                }
                
                // 复制子元素的属性
                org.w3c.dom.NamedNodeMap childAttrs = childElement.getAttributes();
                for (int j = 0; j < childAttrs.getLength(); j++) {
                    Node attr = childAttrs.item(j);
                    String attrName = attr.getNodeName();
                    String attrValue = attr.getNodeValue();
                    
                    if (attrName.startsWith("xmlns")) {
                        newChild.setAttribute(attrName, attrValue);
                    } else {
                        String attrLocalName = attrName.contains(":") ?
                            attrName.substring(attrName.indexOf(":") + 1) : attrName;
                        if (!attrName.contains(":")) {
                            // 如果属性没有前缀，使用当前元素的命名空间
                            if (childPrefix != null) {
                                newChild.setAttribute(attrName, attrValue);
                            } else {
                                newChild.setAttributeNS(namespaceUri, prefix + ":" + attrLocalName, attrValue);
                            }
                        } else {
                            // 如果属性已经有前缀，保持原样
                            newChild.setAttribute(attrName, attrValue);
                        }
                    }
                }
                
                // 递归处理子元素的子节点
                NodeList grandChildren = childElement.getChildNodes();
                for (int k = 0; k < grandChildren.getLength(); k++) {
                    Node grandChild = grandChildren.item(k);
                    if (grandChild.getNodeType() == Node.ELEMENT_NODE) {
                        Element grandChildElement = (Element) grandChild;
                        String grandChildLocalName = grandChildElement.getNodeName().contains(":") ?
                            grandChildElement.getNodeName().substring(grandChildElement.getNodeName().indexOf(":") + 1) :
                            grandChildElement.getNodeName();
                        
                        // 获取孙元素的路径
                        String grandChildPath = getElementPath(grandChildElement);
                        
                        // 查找孙元素的命名空间配置
                        MapperConfig.Namespaces grandChildNs = null;
                        int grandChildMaxMatchLength = -1;
                        
                        for (MapperConfig.Namespaces ns : config.getNamespaces()) {
                            String nsPath = ns.getApplyPrefixTo();
                            if (nsPath != null && !nsPath.isEmpty()) {
                                if (grandChildPath.equals(nsPath) || grandChildPath.startsWith(nsPath + "/")) {
                                    int matchLength = nsPath.length();
                                    if (matchLength > grandChildMaxMatchLength) {
                                        grandChildMaxMatchLength = matchLength;
                                        grandChildNs = ns;
                                    }
                                }
                            }
                        }
                        
                        Element newGrandChild;
                        if (grandChildNs != null) {
                            // 使用孙元素的命名空间
                            newGrandChild = doc.createElementNS(grandChildNs.getUri(), 
                                grandChildNs.getPrefix().trim() + ":" + grandChildLocalName);
                        } else if (childNs != null) {
                            // 如果没有特定的命名空间配置，使用父元素（子元素）的命名空间
                            newGrandChild = doc.createElementNS(childNs.getUri(), 
                                childNs.getPrefix().trim() + ":" + grandChildLocalName);
                        } else {
                            // 如果父元素也没有特定的命名空间配置，使用根元素的命名空间
                            newGrandChild = doc.createElementNS(namespaceUri, prefix + ":" + grandChildLocalName);
                        }
                        
                        // 复制孙元素的属性
                        org.w3c.dom.NamedNodeMap grandChildAttrs = grandChildElement.getAttributes();
                        for (int m = 0; m < grandChildAttrs.getLength(); m++) {
                            Node attr = grandChildAttrs.item(m);
                            String attrName = attr.getNodeName();
                            String attrValue = attr.getNodeValue();
                            
                            if (attrName.startsWith("xmlns")) {
                                newGrandChild.setAttribute(attrName, attrValue);
                            } else {
                                String attrLocalName = attrName.contains(":") ?
                                    attrName.substring(attrName.indexOf(":") + 1) : attrName;
                                if (!attrName.contains(":")) {
                                    if (grandChildNs != null) {
                                        newGrandChild.setAttributeNS(grandChildNs.getUri(), 
                                            grandChildNs.getPrefix().trim() + ":" + attrLocalName, attrValue);
                                    } else if (childNs != null) {
                                        newGrandChild.setAttributeNS(childNs.getUri(), 
                                            childNs.getPrefix().trim() + ":" + attrLocalName, attrValue);
                                    } else {
                                        newGrandChild.setAttributeNS(namespaceUri, 
                                            prefix + ":" + attrLocalName, attrValue);
                                    }
                                } else {
                                    newGrandChild.setAttribute(attrName, attrValue);
                                }
                            }
                        }
                        
                        // 复制孙元素的文本内容
                        if (grandChildElement.getTextContent() != null) {
                            newGrandChild.setTextContent(grandChildElement.getTextContent());
                        }
                        
                        newChild.appendChild(newGrandChild);
                    } else {
                        Node importedNode = doc.importNode(grandChild, true);
                        newChild.appendChild(importedNode);
                    }
                }
                
                newElement.appendChild(newChild);
            } else {
                Node importedNode = doc.importNode(child, true);
                newElement.appendChild(importedNode);
            }
        }

        // 替换原始元素
        Node parent = element.getParentNode();
        if (parent != null) {
            parent.replaceChild(newElement, element);
        } else if (doc.getDocumentElement() == element) {
            doc.removeChild(element);
            doc.appendChild(newElement);
        }
    }


    private void applyXPathValue(Document doc, String xpathStr, Object value, MapperConfig config) throws Exception {
        if (xpathStr == null || xpathStr.trim().isEmpty()) {
            throw new IllegalArgumentException("XPath string cannot be null or empty.");
        }

        Node currentNode = doc;
        // 移除开头的 '/' 以便统一处理，但要确保路径是绝对的
        String effectivePath = xpathStr.startsWith("/") ? xpathStr.substring(1) : xpathStr;
        if (xpathStr.startsWith("//")) {
            throw new IllegalArgumentException("Relative XPath (starting with '//') is not supported for creation: " + xpathStr);
        }
        if (!xpathStr.startsWith("/")) {
             // 如果config中定义了默认根节点，可以在这里处理
             // 否则，我们期望所有路径都是从根开始的绝对路径
             throw new IllegalArgumentException("XPath must be absolute (start with '/'): " + xpathStr);
        }


        String[] parts = effectivePath.split("/");
        Element elementToModify = null;
        String attributeName = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                // 连续的'/'或末尾的'/'会导致空part
                if (i == parts.length -1 && attributeName == null) { // Trailing slash for an element path
                    break;
                }
                throw new IllegalArgumentException("Empty segment in XPath indicates an invalid path: " + xpathStr);
            }

            if (part.startsWith("@")) {
                if (currentNode.getNodeType() != Node.ELEMENT_NODE) {
                    throw new Exception("Cannot set attribute on a non-element node: " +
                                        currentNode.getNodeName() + " for XPath " + xpathStr);
                }
                elementToModify = (Element) currentNode;
                attributeName = part.substring(1);
                if (attributeName.isEmpty()) {
                    throw new IllegalArgumentException("Attribute name cannot be empty in XPath: " + xpathStr);
                }
                if (i != parts.length - 1) {
                    // 属性必须是路径的最后一部分
                    throw new IllegalArgumentException("Attribute specifier must be the last part of an XPath: " + xpathStr);
                }
                break; // 找到属性，结束路径遍历
            } else {
                // 元素部分。可能包含谓词，如 [N] 或 [@attr='val']
                // 为了创建，我们简化处理：提取名称，如果不存在则创建。
                // 谓词使得选择复杂化，但对于创建，我们可能只创建命名元素。
                // 更复杂的谓词处理（如创建特定索引的元素或基于属性值的元素）需要更复杂的逻辑。
                String elementName = part.replaceAll("\\[.*\\]", ""); // 基础的谓词剥离
                if (elementName.isEmpty() || elementName.contains(":") && elementName.split(":")[1].isEmpty()) { // e.g. "ns:"
                    throw new IllegalArgumentException("Invalid element name in XPath part: '" + part + "' from " + xpathStr);
                }
                // TODO: 如果需要，从 'config' 处理命名空间 (例如，如果 elementName 包含前缀 "ns:name")

                NodeList children = currentNode.getChildNodes();
                Node nextNode = null;
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String childName = child.getNodeName();
                        String childLocalName = childName.contains(":") ? 
                            childName.substring(childName.indexOf(":") + 1) : childName;
                        String targetLocalName = elementName.contains(":") ? 
                            elementName.substring(elementName.indexOf(":") + 1) : elementName;
                        if (childLocalName.equals(targetLocalName)) {
                            nextNode = child;
                            break; // 找到已存在的元素
                        }
                    }
                }

                if (nextNode == null) { // 元素不存在，创建它
                    // 检查是否需要应用命名空间
                    Element newElement;
                    if (config != null && config.getNamespaces() != null) {
                        // 查找适用于当前路径的命名空间配置
                        String currentPath = "/" + String.join("/", java.util.Arrays.asList(parts).subList(0, i + 1));
                        // 获取当前元素的本地名称
                        String localName = elementName.contains(":") ? 
                            elementName.substring(elementName.indexOf(":") + 1) : elementName;
                        MapperConfig.Namespaces matchingNs = null;
                        int maxMatchLength = -1;
                        for (MapperConfig.Namespaces ns : config.getNamespaces()) {
                            if (ns.getApplyPrefixTo() != null) {
                                String applyPath = ns.getApplyPrefixTo();
                                if (currentPath.equals(applyPath) || 
                                    (currentPath + "/").startsWith(applyPath + "/")) {
                                    // 选择最长匹配的路径
                                    if (applyPath.length() > maxMatchLength) {
                                        matchingNs = ns;
                                        maxMatchLength = applyPath.length();
                                    }
                                }
                            }
                        }
                        
                        if (matchingNs != null && matchingNs.getUri() != null && matchingNs.getPrefix() != null) {
                            // 使用命名空间创建元素
                            String prefix = matchingNs.getPrefix().trim();
                            localName = elementName.contains(":") ? 
                                elementName.substring(elementName.indexOf(":") + 1) : elementName;
                            // 使用本地名称创建带命名空间的元素
                            newElement = doc.createElementNS(matchingNs.getUri(), prefix + ":" + localName);
                        } else {
                            newElement = doc.createElement(elementName);
                        }
                    } else {
                        newElement = doc.createElement(elementName);
                    }

                    if (currentNode == doc) { // 当前节点是Document，所以这是根元素
                        if (doc.getDocumentElement() != null) {
                            String existingName = doc.getDocumentElement().getNodeName();
                            String existingLocalName = existingName.contains(":") ? 
                                existingName.substring(existingName.indexOf(":") + 1) : existingName;
                            String newLocalName = elementName.contains(":") ? 
                                elementName.substring(elementName.indexOf(":") + 1) : elementName;
                            if (!existingLocalName.equals(newLocalName)) {
                                throw new Exception("Document already has a root element '" +
                                                    existingName +
                                                    "', cannot create another root '" + elementName + "' for XPath: " + xpathStr);
                            }
                            nextNode = doc.getDocumentElement();
                        } else {
                            doc.appendChild(newElement);
                        }
                    } else {
                        currentNode.appendChild(newElement);
                    }
                    if (nextNode == null) { // if not reassigned to existing root
                        nextNode = newElement;
                    }
                }
                currentNode = nextNode;
                if (i == parts.length - 1) { // 这是目标元素
                     if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                        elementToModify = (Element) currentNode;
                    } else {
                        // Should not happen if logic is correct
                        throw new Exception("Path resolution did not end on an Element node for element XPath: " + xpathStr);
                    }
                }
            }
        }
        
        // 设置值
        if (attributeName != null && elementToModify != null) {
            // 设置属性值
            if (value instanceof String) {
                elementToModify.setAttribute(attributeName, (String) value);
            } else if (value instanceof Node) { // 不常见，但可以取其文本内容
                elementToModify.setAttribute(attributeName, ((Node)value).getTextContent());
            } else if (value != null) {
                elementToModify.setAttribute(attributeName, String.valueOf(value));
            } else {
                elementToModify.setAttribute(attributeName, ""); // 或者根据需求移除属性
            }
        } else if (elementToModify != null) {
            // 设置元素内容
            if (value instanceof String) {
                elementToModify.setTextContent((String) value);
            } else if (value instanceof Node) {
                // 如果是Node，导入并替换（或追加）
                // 清理现有子节点，以"设置"节点内容
                while(elementToModify.hasChildNodes()){
                    elementToModify.removeChild(elementToModify.getFirstChild());
                }
                Node importedNode = doc.importNode((Node) value, true);
                elementToModify.appendChild(importedNode);
            } else if (value != null) {
                elementToModify.setTextContent(String.valueOf(value));
            } else {
                 elementToModify.setTextContent(""); // 设置为空字符串或不操作
            }
        } else if (xpathStr.equals("/") && doc.getDocumentElement() == null && value instanceof Node && ((Node)value).getNodeType() == Node.ELEMENT_NODE) {
            // 特殊情况: XPath是"/"，值是一个元素节点，且文档还没有根节点
            Node importedNode = doc.importNode((Node) value, true);
            doc.appendChild(importedNode);
        }
        else {
            // 如果xpathStr是"/"但值不是Node，或者其他未处理情况
            // if (! (config.get().size() == 1 && xpathStr.equals("/") && value == null) ) { //允许唯一的根路径且值为null的情况，生成空根
                 //throw new Exception("Could not determine target element or attribute for XPath: " + xpathStr + " with value type: " + (value == null ? "null" : value.getClass().getName()));
                 // If it's just the root path and value is null, it might mean an empty root element.
                 // The current logic should have created the root element if path was just "/rootname"
                 // This path might be reached if an XPath is just "/" and value is not a Node.
                 // Or if data map was empty and this method was somehow called.
            // }
        }
    }
}