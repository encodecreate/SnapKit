/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.props;
import snap.util.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A PropArchiver subclass specifically to convert to/from XML.
 */
public class PropArchiverXML extends PropArchiver {

    /**
     * Converts a PropObject to XML.
     */
    public XMLElement convertPropObjectToXML(PropObject aPropObject)
    {
        // Convert native to node
        PropNode propNode = convertNativeToNode(null, aPropObject);

        // Convert node to XML
        String propName = propNode.getClassName();
        XMLElement xml = convertNodeToXML(propName, propNode);

        // Archive resources
        for (Resource resource : getResources()) {
            XMLElement resourceXML = new XMLElement("Resource");
            resourceXML.add("Name", resource.getName());
            resourceXML.setValueBytes(resource.getBytes());
            xml.add(resourceXML);
        }

        // Return
        return xml;
    }

    /**
     * Converts a PropObject to XML.
     */
    public byte[] convertPropObjectToXMLBytes(PropObject aPropObject)
    {
        XMLElement xml = convertPropObjectToXML(aPropObject);
        byte[] xmlBytes = xml.getBytes();
        return xmlBytes;
    }

    /**
     * Returns XML for PropNode.
     */
    protected XMLElement convertNodeToXML(String aPropName, PropNode aPropNode)
    {
        // Create XML element for PropNode
        XMLElement xml = new XMLElement(aPropName);

        // If PropNode.NeedsClassDeclaration, add Class attribute to XML
        if (aPropNode.isNeedsClassDeclaration()) {
            String className = aPropNode.getClassName();
            if (!className.equals(aPropName))
                xml.add("Class", className);
        }

        // Get configured Props
        List<Prop> props = aPropNode.getProps();

        // Iterate over PropNames and add XML for each
        for (Prop prop : props) {

            // Get Node value and whether it is node and/or array
            String propName = prop.getName();
            Object nodeValue = aPropNode.getNodeValueForPropName(propName);
            boolean isRelation = prop.isRelation();

            // Handle null
            if (nodeValue == null)
                xml.add(propName, "null");

            // Handle Relation prop
            else if (isRelation)
                convertNodeToXMLForPropRelation(xml, prop, nodeValue);

            // Handle String (non-Node) prop
            else {
                String stringValue = (String) nodeValue;
                convertNodeToXMLForPropSimple(xml, prop, stringValue);
            }
        }

        // Return xml
        return xml;
    }

    /**
     * Converts and adds a prop node relation value to XML.
     */
    protected void convertNodeToXMLForPropRelation(XMLElement xml, Prop prop, Object nodeValue)
    {
        // Get prop info
        String propName = prop.getName();
        boolean isArray = prop.isArray();

        // Handle Node array
        if (isArray) {

            // Get array
            PropNode[] nodeArray = (PropNode[]) nodeValue;

            // Create child XML
            XMLElement propXML = new XMLElement(propName);
            xml.addElement(propXML);

            // Handle PropNode array
            for (PropNode childNode : nodeArray) {
                String childName = childNode.getClassName();
                XMLElement childXML = convertNodeToXML(childName, childNode);
                propXML.addElement(childXML);
            }
        }

        // Handle Node object
        else {
            PropNode propNode = (PropNode) nodeValue;
            XMLElement propXML = convertNodeToXML(propName, propNode);
            xml.addElement(propXML);
        }
    }

    /**
     * Converts and adds a prop node string value to XML.
     */
    protected void convertNodeToXMLForPropSimple(XMLElement xml, Prop prop, String stringValue)
    {
        // Get prop info
        String propName = prop.getName();
        boolean isArray = prop.isArray();

        // Handle primitive array
        if (isArray) {
            XMLElement propXML = new XMLElement(propName);
            xml.addElement(propXML);
            propXML.setValue(stringValue);
        }

        // Handle primitive value
        else xml.add(propName, stringValue);
    }

    /**
     * Reads a PropObject from XML source.
     */
    public Object readPropObjectFromXMLSource(Object aSource)
    {
        // Get bytes from source - if not found or empty, complain
        byte[] xmlBytes = SnapUtils.getBytes(aSource);
        if (xmlBytes == null || xmlBytes.length == 0)
            throw new RuntimeException("XMLArchiver.readObject: Cannot read source: " + aSource);

        // Try to get SourceURL from source
        //if (getSourceURL() == null) { WebURL surl = WebURL.getURL(aSource); setSourceURL(surl); }

        // Read from bytes and return
        return readPropObjectFromXMLBytes(xmlBytes);
    }

    /**
     * Reads a PropObject from XML String.
     */
    public Object readPropObjectFromXMLString(String xmlString)
    {
        try {
            XMLParser xmlParser = new XMLParser();
            XMLElement xml = xmlParser.parseXMLFromString(xmlString);
            return readPropObjectFromXML(xml);
        }

        catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Reads a PropObject from XML.
     */
    public Object readPropObjectFromXMLBytes(byte[] theBytes)
    {
        XMLElement xml = XMLElement.readFromXMLBytes(theBytes);
        return readPropObjectFromXML(xml);
    }

    /**
     * Reads a PropObject from XML.
     */
    public PropObject readPropObjectFromXML(XMLElement anElement)
    {
        // Probably not helpful
        anElement.setIgnoreCase(true);

        // Read resources
        readResources(anElement);

        // Read PropNode from XML
        PropNode propNode = convertXMLToNode(null, null, anElement);

        // Convert PropNode (graph) to PropObject
        PropObject propObject = convertNodeToNative(propNode);

        // Return
        return propObject;
    }

    /**
     * Reads a PropNode from XML.
     */
    protected PropNode convertXMLToNode(PropNode aParent, Prop aProp, XMLElement anElement)
    {
        // Create PropObject for element
        PropObject propObject = createPropObjectForXML(aParent, aProp, anElement);

        // Create PropNode for propObject
        PropNode propNode = new PropNode(propObject, this);

        // Get list of configured XML attributes
        List<XMLAttribute> attributes = anElement.getAttributes();

        // Iterate over XML attributes and add node/native value for each
        for (XMLAttribute attr : attributes) {

            // Get attribute name (skip special Class attr)
            String propName = attr.getName();
            if (propName.equals("Class")) continue;

            // Get prop
            Prop prop = propObject.getPropForName(propName);
            if (prop == null) continue; // Should never happen

            // Add node value to PropNode
            String nodeValue = attr.getValue();
            addNodeValueForProp(propNode, prop, nodeValue);
        }

        // Get list of configured XML elements
        List<XMLElement> elements = anElement.getElements();

        // Iterate over XML elements and add node/native value for each
        for (XMLElement xml : elements) {

            // XML name
            String xmlName = xml.getName();

            // Get prop
            Prop prop = propObject.getPropForName(xmlName);
            if (prop == null) continue; // Should never happen

            // Handle array
            if (prop.isArray()) {

                // Handle Relation array: Get node value for XML and add to PropNode
                if (prop.isRelation()) {
                    PropNode[] nodeValue = convertXMLToNodeForXMLRelationArray(propNode, prop, xml);
                    propNode.addNodeValueForProp(prop, nodeValue);
                }

                // Handle simple array: Read array string and add to PropNode
                else {
                    String nodeValue = xml.getValue();
                    propNode.addNodeValueForProp(prop, nodeValue);
                }
            }

            // Handle relation: Get node value for XML and add to PropNode
            else {
                PropNode nodeValue = convertXMLToNode(propNode, prop, xml);
                propNode.addNodeValueForProp(prop, nodeValue);
            }
        }

        // Return
        return propNode;
    }

    /**
     * Reads a PropNode from XML.
     */
    protected PropNode[] convertXMLToNodeForXMLRelationArray(PropNode aParent, Prop aProp, XMLElement anElement)
    {
        // Get list of configured XML elements
        List<XMLElement> elements = anElement.getElements();

        // Create list
        List<PropNode> propNodes = new ArrayList<>(elements.size());

        // Iterate over XML elements and add node/native value for each
        for (XMLElement xml : elements) {

            PropNode propNode = convertXMLToNode(aParent, aProp, xml);
            if (propNode != null)
                propNodes.add(propNode);
        }

        // Return
        return propNodes.toArray(new PropNode[0]);
    }

    /**
     * Creates a PropObject for XML element.
     */
    protected PropObject createPropObjectForXML(PropNode aParent, Prop aProp, XMLElement anElement)
    {
        // If Prop.Preexisting, just instance from PropObject instead
        if (aProp != null && aProp.isPreexisting() && aParent != null) {
            PropObject propObject = aParent.getPropObject();
            Object existingInstance = propObject.getPropValue(aProp.getName());
            if (existingInstance instanceof PropObject)
                return (PropObject) existingInstance;
        }

        // If Class attribute set, try that
        String xmlClassName = anElement.getAttributeValue("Class");
        if (xmlClassName != null) {
            Class<?> cls = getClassForName(xmlClassName);
            if (cls != null)
                return createPropObjectForClass(cls);
        }

        // If Prop is Array, try XML name next
        if (aProp != null && aProp.isArray()) {
            String xmlName = anElement.getName();
            Class<?> xmlNameClass = getClassForName(xmlName);
            if (xmlNameClass != null)
                return createPropObjectForClass(xmlNameClass);
        }

        // Try Prop class attribute
        Class<?> propClass = aProp != null ? aProp.getDefaultPropClass() : null;
        if (propClass != null) {

            // If array, swap for component class
            if (propClass.isArray())
                propClass = propClass.getComponentType();

            return createPropObjectForClass(propClass);
        }

        // Try PropName as ClassMap name
        String propName = aProp != null ? aProp.getName() : null;
        propClass = getClassForName(propName);
        if (propClass != null) {
            return createPropObjectForClass(propClass);
        }

        // Try element name
        String xmlName = anElement.getName();
        Class<?> xmlNameClass = getClassForName(xmlName);
        if (xmlNameClass != null)
            return createPropObjectForClass(xmlNameClass);

        // Complain and return
        System.err.println("PropArchiverXML.createPropObjectForXML: Undetermined class for XML: " + anElement);
        return null;
    }

    /**
     * Reads resources from <Resource> elements in given xml (top-level) element, converts from ASCII encoding and
     * adds to archiver.
     */
    protected void readResources(XMLElement anElement)
    {
        // Get resources from top level <resource> tags
        for (int i = anElement.indexOf("Resource"); i >= 0; i = anElement.indexOf("Resource", i)) {

            // Get/remove current resource element
            XMLElement e = anElement.removeElement(i);

            // Get resource name and bytes
            String name = e.getAttributeValue("name");
            byte[] bytes = e.getValueBytes();

            // Add resource bytes for name
            addResource(name, bytes);
        }
    }

    /**
     * This FormatNode implementation allows PropArchiver to read/write to XML.
     */
    public static class XMLFormatConverter implements PropArchiverX.FormatConverter<Object> {

        /**
         * Creates a format node for given prop name.
         */
        public Object createFormatNode(String aPropName)
        {
            return new XMLElement(aPropName);
        }

        /**
         * Return child property keys.
         */
        @Override
        public String[] getChildKeys(Object aNode)
        {
            if (aNode instanceof XMLElement) {
                XMLElement xml = (XMLElement) aNode;
                int attrCount = xml.getAttributeCount();
                int elemCount = xml.getElementCount();
                String[] keys = new String[attrCount + elemCount];
                for (int i = 0; i < attrCount; i++)
                    keys[i] = xml.getAttribute(i).getName();
                for (int i = 0; i < elemCount; i++)
                    keys[i + attrCount] = xml.getElement(i).getName();
                return keys;
            }

            return new String[0];
        }

        /**
         * Return child property value for given key.
         */
        @Override
        public Object getChildNodeForKey(Object aNode, String aName)
        {
            // Handle XMLElement
            if (aNode instanceof XMLElement) {

                // Look for attribute
                XMLElement xml = (XMLElement) aNode;
                Object value = xml.getAttribute(aName);
                if (value != null)
                    return value;

                // Look for element
                value = xml.getElement(aName);
                if (value != null)
                    return value;

                // Special Class_Key support: XML can exclude explicit Class key if matches name
                if (aName.equals("Class"))
                    return xml.getName();
            }

            // Return not found
            return null;
        }

        /**
         * Returns the node value as string.
         */
        @Override
        public String getNodeValueAsString(Object aNode)
        {
            // Handle Attribute
            if (aNode instanceof XMLAttribute)
                return ((XMLAttribute) aNode).getValue();

            // Handle Element
            if (aNode instanceof XMLElement)
                return ((XMLElement) aNode).getValue();

            // Handle String (Probably Class_Key)
            if (aNode instanceof String)
                return (String) aNode;

            // Return not found
            return null;
        }

        /**
         * Returns array of nodes for a format node.
         */
        public Object[] getNodeValueAsArray(Object anArrayNode)
        {
            if (anArrayNode instanceof XMLElement)
                return ((XMLElement) anArrayNode).getElements().toArray();
            return null;
        }

        /**
         * Sets a node value.
         */
        public void setNodeValue(Object aNode, String aValue)
        {
            // Handle Attribute
            if (aNode instanceof XMLAttribute)
                ((XMLAttribute) aNode).setValue(aValue);

                // Handle Element
            else if (aNode instanceof XMLElement)
                ((XMLElement) aNode).setValue(aValue);

                // Handle unexpected
            else System.err.println("XMLFormatConverter.setNodeValue: Unexpected node: " + aNode);
        }

        /**
         * Sets a node value for given key.
         */
        public void setNodeValueForKey(Object aNode, String aKey, Object aValue)
        {
            // Handle Element
            if (aNode instanceof XMLElement) {
                XMLElement xml = (XMLElement) aNode;
                if (aValue instanceof XMLElement)
                    xml.addElement((XMLElement) aValue);
                else xml.add(aKey, aValue);
            }

            // Handle unexpected
            else System.err.println("XMLFormatConverter.setNodeValueForKey: Unexpected node: " + aNode);
        }

        /**
         * Adds a node array item for given array key.
         */
        public void addNodeArrayItemForKey(Object aNode, String aKey, Object aValue)
        {
            // Handle Element
            if (aNode instanceof XMLElement) {
                XMLElement xml = (XMLElement) aNode;

                // Get array element (create/add if missing)
                XMLElement arrayXML = xml.getElement(aKey);
                if (arrayXML == null) {
                    arrayXML = new XMLElement(aKey);
                    xml.addElement(arrayXML);
                }

                // Add item
                if (aValue instanceof XMLElement)
                    arrayXML.addElement((XMLElement) aValue);
                else System.err.println("XMLFormatConverter.addNodeArrayItemForKey: Unexpected array item node: " + aNode);
            }

            // Handle unexpected
            else System.err.println("XMLFormatConverter.addNodeArrayItemForKey: Unexpected array node: " + aNode);
        }
    }
}
