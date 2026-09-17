package com.burakkutbay.studentapi.json;

import java.util.Map;

/**
 * JSON'a dönüştürülebilen nesneler için sözleşme.
 */
public interface JsonSerializable {

    Map toMap();
}
