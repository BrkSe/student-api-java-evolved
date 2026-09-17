package com.burakkutbay.studentapi.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class JsonRoundTripTest {

    @Test
    public void parsesNestedStructures() {
        Map map = JsonParser.parseObject(
                "{ \"ad\": \"Ay\\u015fe\", \"yas\": 21, \"gno\": 3.75, \"aktif\": true, \"not\": null,"
                        + " \"dersler\": [\"CENG101\", \"MATH101\"] }");
        assertEquals("Ayşe", map.get("ad"));
        assertEquals(new Long(21), map.get("yas"));
        assertEquals(new Double(3.75), map.get("gno"));
        assertEquals(Boolean.TRUE, map.get("aktif"));
        assertNull(map.get("not"));
        assertTrue(map.get("dersler") instanceof List);
        assertEquals(2, ((List) map.get("dersler")).size());
    }

    @Test
    public void writesEscapedStrings() {
        Map map = new LinkedHashMap();
        map.put("mesaj", "satır1\nsatır2 \"alıntı\" \\");
        List list = new ArrayList();
        list.add(new Integer(1));
        list.add(new Double(2.5));
        list.add(null);
        map.put("liste", list);
        assertEquals("{\"mesaj\":\"satır1\\nsatır2 \\\"alıntı\\\" \\\\\",\"liste\":[1,2.5,null]}",
                JsonWriter.toJson(map));
    }

    @Test
    public void roundTripPreservesValues() {
        String json = "{\"a\":[1,2,{\"b\":\"c\"}],\"d\":false}";
        assertEquals(json, JsonWriter.toJson(JsonParser.parse(json)));
    }

    @Test(expected = JsonException.class)
    public void rejectsTrailingGarbage() {
        JsonParser.parse("{\"a\":1} x");
    }

    @Test(expected = JsonException.class)
    public void rejectsNonObjectWhenObjectExpected() {
        JsonParser.parseObject("[1,2,3]");
    }
}
