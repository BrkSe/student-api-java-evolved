package com.burakkutbay.studentapi.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class JsonRoundTripTest {

    @Test
    void parsesNestedStructures() {
        var map = JsonParser.parseObject("""
                { "ad": "Ay\\u015fe", "yas": 21, "gno": 3.75, "aktif": true, "not": null,
                  "dersler": ["CENG101", "MATH101"] }
                """);
        assertEquals("Ayşe", map.get("ad"));
        assertEquals(21L, map.get("yas"));
        assertEquals(3.75, map.get("gno"));
        assertEquals(true, map.get("aktif"));
        assertNull(map.get("not"));
        assertEquals(2, assertInstanceOf(List.class, map.get("dersler")).size());
    }

    @Test
    void writesEscapedStrings() {
        var map = new LinkedHashMap<String, Object>();
        map.put("mesaj", "satır1\nsatır2 \"alıntı\" \\");
        map.put("liste", Arrays.asList(1, 2.5, null));
        assertEquals("""
                {"mesaj":"satır1\\nsatır2 \\"alıntı\\" \\\\","liste":[1,2.5,null]}""", JsonWriter.toJson(map));
    }

    @Test
    void roundTripPreservesValues() {
        var json = """
                {"a":[1,2,{"b":"c"}],"d":false}""";
        assertEquals(json, JsonWriter.toJson(JsonParser.parse(json)));
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThrows(JsonException.class, () -> JsonParser.parse("{\"a\":1} x"));
    }

    @Test
    void rejectsNonObjectWhenObjectExpected() {
        assertThrows(JsonException.class, () -> JsonParser.parseObject("[1,2,3]"));
    }
}
