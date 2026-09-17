package com.burakkutbay.studentapi.json;

import java.util.SequencedMap;

/// JSON'a dönüştürülebilen nesneler için sözleşme. Anahtar sırası çıktıda korunur.
public interface JsonSerializable {

    SequencedMap<String, Object> toMap();
}
