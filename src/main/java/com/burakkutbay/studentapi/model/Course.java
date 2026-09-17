package com.burakkutbay.studentapi.model;

import java.util.LinkedHashMap;
import java.util.SequencedMap;

import com.burakkutbay.studentapi.json.JsonSerializable;

public record Course(String code, String name, int credits, String department) implements JsonSerializable {

    @Override
    public SequencedMap<String, Object> toMap() {
        var map = new LinkedHashMap<String, Object>();
        map.put("code", code);
        map.put("name", name);
        map.put("credits", credits);
        map.put("department", department);
        return map;
    }
}
