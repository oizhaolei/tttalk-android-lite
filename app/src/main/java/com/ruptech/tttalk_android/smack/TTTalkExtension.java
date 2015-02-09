package com.ruptech.tttalk_android.smack;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhaolei on 15/1/30.
 */
public class TTTalkExtension implements PacketExtension {
    public static final String NAMESPACE = "http://jabber.org/protocol/tranlate";
    public static final String ELEMENT_NAME = "tttalk";
    private Map<String, String> map = new HashMap<String, String>();

    public TTTalkExtension(Map map) { this.map = map;}

    public String getValue(String key) {
        return map.get(key);
    }

    public void setValue(String key, String value) {
        map.put(key, value);
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\" ");
        for(Map.Entry<String, String> entry : map.entrySet()){
            buf.append(entry.getKey() + "=\"" + entry.getValue() + "\" ");
        }
        buf.append("/>");
        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {
        public Provider() {
        }

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            parser.next();
            String name = parser.getText();
            Map map = new HashMap<String, String>();
            if (parser.getAttributeCount() > 0) {

                for (int i = 0, n = parser.getAttributeCount(); i < n; i++) {
                    map.put(parser.getAttributeName(i), parser.getAttributeValue(i));
                }
            }

            return new TTTalkExtension(map);
        }
    }
}
