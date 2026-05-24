package com.dabai.easy_lowcode.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimpleCache LRU 缓存测试
 */
class SimpleCacheTest {

    @Test
    void testPutAndGet() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void testGetNonExistentKey() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        assertNull(cache.get("nonexistent"));
    }

    @Test
    void testLruEviction() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");

        assertNull(cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));
    }

    @Test
    void testAccessOrderResetsLru() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        cache.get("key1");

        cache.put("key4", "value4");

        assertEquals("value1", cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testRemove() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        cache.put("key1", "value1");
        cache.remove("key1");
        assertNull(cache.get("key1"));
    }

    @Test
    void testClear() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    void testSize() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        assertEquals(0, cache.size());
        cache.put("key1", "value1");
        assertEquals(1, cache.size());
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
    }

    @Test
    void testOverwrite() {
        SimpleCache<String, String> cache = new SimpleCache<>(3);
        cache.put("key1", "value1");
        cache.put("key1", "value2");
        assertEquals("value2", cache.get("key1"));
        assertEquals(1, cache.size());
    }
}
