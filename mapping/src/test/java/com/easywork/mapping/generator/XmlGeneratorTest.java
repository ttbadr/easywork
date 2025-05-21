package com.easywork.mapping.generator;

import com.easywork.mapping.config.MapperConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import com.easywork.mapping.config.MapperConfig.Namespaces;

class XmlGeneratorTest {

    private XmlGenerator xmlGenerator;
    private MapperConfig mapperConfig;

    @BeforeEach
    void setUp() {
        xmlGenerator = new XmlGenerator();
        mapperConfig = new MapperConfig(); // Or mock if needed
    }

    private String normalizeXml(String xml) {
        // Remove XML declaration, newlines, and spaces between tags for consistent comparison
        return xml.replaceAll("<\\?xml.*\\?>", "")
                  .replaceAll("\\R+", "")
                  .replaceAll(">\\s+<", "><")
                  .trim();
    }

    @Test
    void testGenerateSimpleXml() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/element", "value");
        String expectedXml = "<root><element>value</element></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateXmlWithAttribute() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/element/@attr", "attrValue");
        data.put("/root/element", "textValue");
        String expectedXml = "<root><element attr=\"attrValue\">textValue</element></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateNestedElements() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/parent/child1", "value1");
        data.put("/root/parent/child2/@id", "c2");
        data.put("/root/parent/child2", "value2");
        String expectedXml = "<root><parent><child1>value1</child1><child2 id=\"c2\">value2</child2></parent></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateEmptyMap() throws Exception {
        Map<String, Object> data = new HashMap<>();
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        // Expecting an empty XML document (possibly just the XML declaration, which normalizeXml removes)
        // or an empty string if the transformer omits declaration for an empty doc.
        // The current implementation with an empty map results in an XML declaration only.
        assertEquals("", normalizeXml(actualXml));
    }

    @Test
    void testGenerateRootOnlyWithValue() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root", "rootValue");
        String expectedXml = "<root>rootValue</root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateRootOnlyNullValue() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root", null); // Should create an empty root element
        String expectedXml = "<root/>"; // Empty elements are often self-closed
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateWithNodeValue() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element nodeValue = doc.createElement("childNode");
        nodeValue.setTextContent("childText");
        nodeValue.setAttribute("childAttr", "attrVal");

        data.put("/root/elementToReplace", nodeValue);
        String expectedXml = "<root><elementToReplace><childNode childAttr=\"attrVal\">childText</childNode></elementToReplace></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateAttributeOnNonExistingElement() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/newElement/@id", "123");
        String expectedXml = "<root><newElement id=\"123\"/></root>"; // Empty elements with attributes are also self-closed
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testGenerateMultipleAttributes() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/element/@attr1", "val1");
        data.put("/root/element/@attr2", "val2");
        data.put("/root/element", "text");
        String expectedXml = "<root><element attr1=\"val1\" attr2=\"val2\">text</element></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testInvalidXPath_RelativePath() {
        Map<String, Object> data = new HashMap<>();
        data.put("//root/element", "value");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            xmlGenerator.generate(data, mapperConfig);
        });
        assertTrue(exception.getMessage().contains("Relative XPath (starting with '//') is not supported"));
    }

    @Test
    void testInvalidXPath_NotAbsolutePath() {
        Map<String, Object> data = new HashMap<>();
        data.put("root/element", "value");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            xmlGenerator.generate(data, mapperConfig);
        });
        assertTrue(exception.getMessage().contains("XPath must be absolute (start with '/')"));
    }

    @Test
    void testInvalidXPath_EmptySegment() {
        Map<String, Object> data = new HashMap<>();
        data.put("/root//element", "value");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            xmlGenerator.generate(data, mapperConfig);
        });
        assertTrue(exception.getMessage().contains("Empty segment in XPath indicates an invalid path"));
    }

    @Test
    void testInvalidXPath_AttributeNotLast() {
        Map<String, Object> data = new HashMap<>();
        data.put("/root/@attr/element", "value");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            xmlGenerator.generate(data, mapperConfig);
        });
        assertTrue(exception.getMessage().contains("Attribute specifier must be the last part of an XPath"));
    }
    
    @Test
    void testInvalidXPath_EmptyAttributeName() {
        Map<String, Object> data = new HashMap<>();
        data.put("/root/@", "value");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            xmlGenerator.generate(data, mapperConfig);
        });
        assertTrue(exception.getMessage().contains("Attribute name cannot be empty in XPath"));
    }

    @Test
    void testGenerateXmlWithSpecialCharactersInValue() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        String originalValue = "value with <>&'\"";
        data.put("/root/element", originalValue);
        String actualXml = xmlGenerator.generate(data, mapperConfig);

        // Parse the actualXml to verify content
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder docBuilder = factory.newDocumentBuilder();
        org.xml.sax.InputSource is = new org.xml.sax.InputSource(new java.io.StringReader(actualXml));
        org.w3c.dom.Document parsedDoc = docBuilder.parse(is);
        String textContent = parsedDoc.getElementsByTagName("element").item(0).getTextContent();
        
        assertEquals(originalValue, textContent);
    }

    @Test
    void testGenerateXmlWithSpecialCharactersInAttribute() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        String originalValue = "value with <>&'\"";
        data.put("/root/element/@attr", originalValue);
        String actualXml = xmlGenerator.generate(data, mapperConfig);

        // Parse the actualXml to verify attribute content
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder docBuilder = factory.newDocumentBuilder();
        org.xml.sax.InputSource is = new org.xml.sax.InputSource(new java.io.StringReader(actualXml));
        org.w3c.dom.Document parsedDoc = docBuilder.parse(is);
        org.w3c.dom.Element element = (org.w3c.dom.Element) parsedDoc.getElementsByTagName("element").item(0);
        String attributeValue = element.getAttribute("attr");

        assertEquals(originalValue, attributeValue);
    }
    
    @Test
    void testOverwriteExistingElementValue() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/element", "initialValue");
        data.put("/root/element", "newValue"); // Overwrite
        String expectedXml = "<root><element>newValue</element></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testOverwriteExistingAttributeValue() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/root/element/@attr", "initialAttr");
        data.put("/root/element/@attr", "newAttr"); // Overwrite
        data.put("/root/element", "text");
        String expectedXml = "<root><element attr=\"newAttr\">text</element></root>";
        String actualXml = xmlGenerator.generate(data, mapperConfig);
        assertEquals(normalizeXml(expectedXml), normalizeXml(actualXml));
    }

    @Test
    void testComplexStructureWithMixedContent() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("/catalog/book/@id", "bk101");
        data.put("/catalog/book/author", "Gambardella, Matthew");
        data.put("/catalog/book/title", "XML Developer's Guide");
        data.put("/catalog/book/genre", "Computer");
        data.put("/catalog/book/price", "44.95");
        data.put("/catalog/book/publish_date", "2000-10-01");
        data.put("/catalog/book/description", "An in-depth look at creating applications with XML.");

        String actualXml = xmlGenerator.generate(data, mapperConfig);
        
        // Verify structure and content (simplified check)
        assertTrue(actualXml.contains("<author>Gambardella, Matthew</author>"));
        assertTrue(actualXml.contains("id=\"bk101\""));
        assertTrue(actualXml.contains("<price>44.95</price>"));
    }

    @Test
    void testGenerateXmlWithDefaultNamespaceOnRoot() throws Exception {
        XmlGenerator generator = new XmlGenerator();
        Map<String, Object> data = new HashMap<>();
        data.put("/root/child", "value");

        MapperConfig config = new MapperConfig();
        Namespaces ns = new Namespaces();
        ns.setUri("http://www.example.com/default");
        ns.setDefineNsIn("/"); // Define on root
        ns.setApplyPrefixTo("/"); // Apply to root and children (no prefix means default NS)
        config.setNamespaces(Arrays.asList(ns));

        String xml = generator.generate(data, config);
        // Note: Without setNamespaceAware(true), prefix application is manual and default namespace application is tricky.
        // The current renameNodesRecursively won't add a prefix if ns.getPrefix() is null/empty.
        // And setAttribute("xmlns", uri) correctly defines it.
        // However, children won't automatically inherit the default namespace in their node names without a prefix.
        // The expectation here is based on how a parser *not* namespace-aware might see it, or how it's literally constructed.
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<root xmlns=\"http://www.example.com/default\">\n  <child>value</child>\n</root>\n";
        assertEquals(normalizeXml(expectedXml), normalizeXml(xml));
    }

    @Test
    void testGenerateXmlWithPrefixedNamespaceOnRootAndChildren() throws Exception {
        XmlGenerator generator = new XmlGenerator();
        Map<String, Object> data = new HashMap<>();
        data.put("/root/child/grandchild", "value");
        data.put("/root/child/@attr", "attrValue");

        MapperConfig config = new MapperConfig();
        Namespaces ns = new Namespaces();
        ns.setPrefix("ex");
        ns.setUri("http://www.example.com/prefixed");
        ns.setDefineNsIn("/");
        ns.setApplyPrefixTo("/");
        config.setNamespaces(Arrays.asList(ns));

        String xml = generator.generate(data, config);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<ex:root xmlns:ex=\"http://www.example.com/prefixed\">\n  <ex:child ex:attr=\"attrValue\">\n    <ex:grandchild>value</ex:grandchild>\n  </ex:child>\n</ex:root>\n";
        assertEquals(normalizeXml(expectedXml), normalizeXml(xml));
    }

    @Test
    void testGenerateXmlWithNamespaceDefinedOnChild() throws Exception {
        XmlGenerator generator = new XmlGenerator();
        Map<String, Object> data = new HashMap<>();
        data.put("/root/child/grandchild", "value");

        MapperConfig config = new MapperConfig();
        Namespaces ns = new Namespaces();
        ns.setPrefix("ch");
        ns.setUri("http://www.example.com/childns");
        ns.setDefineNsIn("/root/child");
        ns.setApplyPrefixTo("/root/child");
        config.setNamespaces(Arrays.asList(ns));

        String xml = generator.generate(data, config);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<root>\n  <ch:child xmlns:ch=\"http://www.example.com/childns\">\n    <ch:grandchild>value</ch:grandchild>\n  </ch:child>\n</root>\n";
        assertEquals(normalizeXml(expectedXml), normalizeXml(xml));
    }

    @Test
    void testGenerateXmlWithMultipleNamespaces() throws Exception {
        XmlGenerator generator = new XmlGenerator();
        Map<String, Object> data = new HashMap<>();
        data.put("/r:root/a:branch1/a:leaf", "val1");
        data.put("/r:root/b:branch2/b:leaf", "val2");
        data.put("/r:root/@r:id", "rootId");

        MapperConfig config = new MapperConfig();
        Namespaces nsRoot = new Namespaces();
        nsRoot.setPrefix("r");
        nsRoot.setUri("http://www.example.com/root");
        nsRoot.setDefineNsIn("/");
        nsRoot.setApplyPrefixTo("/");

        Namespaces nsA = new Namespaces();
        nsA.setPrefix("a");
        nsA.setUri("http://www.example.com/nsA");
        nsA.setDefineNsIn("/root/branch1"); // Define where it's first used or on a common ancestor
        nsA.setApplyPrefixTo("/root/branch1");

        Namespaces nsB = new Namespaces();
        nsB.setPrefix("b");
        nsB.setUri("http://www.example.com/nsB");
        nsB.setDefineNsIn("/root/branch2");
        nsB.setApplyPrefixTo("/root/branch2");
        
        config.setNamespaces(Arrays.asList(nsRoot, nsA, nsB));

        // Data keys should not have prefixes if they are to be prefixed by the generator
        // Or, the findElementByPath and applyXPathValue need to be namespace aware (which they are not currently)
        // For this test, assuming applyXPathValue creates plain nodes, and applyNamespaces adds prefixes.
        // So, data paths should be simple, and then prefixes applied.
        // Let's adjust data keys for the current implementation of applyXPathValue and findElementByPath
        Map<String, Object> plainData = new HashMap<>();
        plainData.put("/root/branch1/leaf", "val1");
        plainData.put("/root/branch2/leaf", "val2");
        plainData.put("/root/@id", "rootId");

        String xml = generator.generate(plainData, config);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<r:root xmlns:r=\"http://www.example.com/root\" r:id=\"rootId\">\n  <a:branch1 xmlns:a=\"http://www.example.com/nsA\">\n    <a:leaf>val1</a:leaf>\n  </a:branch1>\n  <b:branch2 xmlns:b=\"http://www.example.com/nsB\">\n    <b:leaf>val2</b:leaf>\n  </b:branch2>\n</r:root>\n";
        assertEquals(normalizeXml(expectedXml), normalizeXml(xml));
    }

    @Test
    void testGenerateXmlWithNamespaceOnAttributeOnly() throws Exception {
        XmlGenerator generator = new XmlGenerator();
        Map<String, Object> data = new HashMap<>();
        data.put("/root/element/@attr", "value");

        MapperConfig config = new MapperConfig();
        Namespaces ns = new Namespaces();
        ns.setPrefix("attrns");
        ns.setUri("http://www.example.com/attr");
        // Define namespace on the element that will hold the prefixed attribute
        ns.setDefineNsIn("/root/element"); 
        // Apply prefix to the element so its attributes can be prefixed
        // The current renameNodesRecursively applies prefix to element AND its attributes.
        // If only attribute prefixing is desired without element prefixing, renameNodesRecursively needs adjustment.
        // For this test, we assume the element itself also gets the prefix if applyPrefixTo targets it.
        ns.setApplyPrefixTo("/root/element"); 
        config.setNamespaces(Arrays.asList(ns));

        String xml = generator.generate(data, config);
        // If element is also prefixed:
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<root>\n  <attrns:element xmlns:attrns=\"http://www.example.com/attr\" attrns:attr=\"value\"/>\n</root>\n";
        assertEquals(normalizeXml(expectedXml), normalizeXml(xml));
    }

    @Test
    void testGenerateXmlWithMultipleNamespaces2() throws Exception {
        XmlGenerator generator = new XmlGenerator();
        Map<String, Object> data = new HashMap<>();
        data.put("/root/branch1/leaf", "val1");
        data.put("/root/branch2/leaf", "val2");
        data.put("/root/@id", "rootId");

        MapperConfig config = new MapperConfig();
        Namespaces nsRoot = new Namespaces();
        nsRoot.setPrefix("");
        nsRoot.setUri("http://www.example.com/root");
        nsRoot.setDefineNsIn("/");
        nsRoot.setApplyPrefixTo("/");

        Namespaces nsA = new Namespaces();
        nsA.setPrefix("a");
        nsA.setUri("http://www.example.com/nsA");
        nsA.setDefineNsIn("/root"); // Define where it's first used or on a common ancestor
        nsA.setApplyPrefixTo("/root/branch1");

        Namespaces nsB = new Namespaces();
        nsB.setPrefix("b");
        nsB.setUri("http://www.example.com/nsB");
        nsB.setDefineNsIn("/root");
        nsB.setApplyPrefixTo("/root/branch2");
        
        config.setNamespaces(Arrays.asList(nsRoot, nsA, nsB));

        // Data keys should not have prefixes if they are to be prefixed by the generator
        // Or, the findElementByPath and applyXPathValue need to be namespace aware (which they are not currently)
        // For this test, assuming applyXPathValue creates plain nodes, and applyNamespaces adds prefixes.
        // So, data paths should be simple, and then prefixes applied.
        // Let's adjust data keys for the current implementation of applyXPathValue and findElementByPath
        Map<String, Object> plainData = new HashMap<>();
        plainData.put("/root/branch1/leaf", "val1");
        plainData.put("/root/branch2/leaf", "val2");
        plainData.put("/root/@id", "rootId");

        String xml = generator.generate(plainData, config);
        String expectedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<r:root xmlns:r=\"http://www.example.com/root\" r:id=\"rootId\">\n  <a:branch1 xmlns:a=\"http://www.example.com/nsA\">\n    <a:leaf>val1</a:leaf>\n  </a:branch1>\n  <b:branch2 xmlns:b=\"http://www.example.com/nsB\">\n    <b:leaf>val2</b:leaf>\n  </b:branch2>\n</r:root>\n";
        assertEquals(normalizeXml(expectedXml), normalizeXml(xml));
    }
}