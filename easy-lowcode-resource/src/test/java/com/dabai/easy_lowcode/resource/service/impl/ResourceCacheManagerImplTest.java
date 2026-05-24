package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.resource.model.FieldConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceCacheManagerImpl 单元测试
 */
class ResourceCacheManagerImplTest {

    private ResourceCacheManagerImpl cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new ResourceCacheManagerImpl();
    }

    @Test
    void testCacheAndGetAllowedColumns() {
        Long resourceId = 1L;
        Set<String> columns = new HashSet<>(Arrays.asList("id", "name", "status"));

        cacheManager.cacheAllowedColumns(resourceId, columns);
        Set<String> cached = cacheManager.getCachedAllowedColumns(resourceId);

        assertNotNull(cached);
        assertEquals(3, cached.size());
        assertTrue(cached.contains("id"));
        assertTrue(cached.contains("name"));
        assertTrue(cached.contains("status"));
    }

    @Test
    void testGetAllowedColumns_notCached() {
        Set<String> cached = cacheManager.getCachedAllowedColumns(999L);

        assertNull(cached);
    }

    @Test
    void testCacheAndGetFieldConfigs() {
        Long resourceId = 1L;
        FieldConfig field1 = new FieldConfig();
        field1.setColumnName("id");
        field1.setDataType("BIGINT");
        FieldConfig field2 = new FieldConfig();
        field2.setColumnName("name");
        field2.setDataType("VARCHAR");

        cacheManager.cacheFieldConfigs(resourceId, Arrays.asList(field1, field2));
        var cached = cacheManager.getCachedFieldConfigs(resourceId);

        assertNotNull(cached);
        assertEquals(2, cached.size());
        assertEquals("id", cached.get(0).getColumnName());
        assertEquals("name", cached.get(1).getColumnName());
    }

    @Test
    void testGetFieldConfigs_notCached() {
        var cached = cacheManager.getCachedFieldConfigs(999L);

        assertNull(cached);
    }

    @Test
    void testEvict_removesBothCaches() {
        Long resourceId = 1L;
        Set<String> columns = Collections.singleton("id");
        FieldConfig field = new FieldConfig();
        field.setColumnName("id");

        cacheManager.cacheAllowedColumns(resourceId, columns);
        cacheManager.cacheFieldConfigs(resourceId, Collections.singletonList(field));

        assertNotNull(cacheManager.getCachedAllowedColumns(resourceId));
        assertNotNull(cacheManager.getCachedFieldConfigs(resourceId));

        cacheManager.evict(resourceId);

        assertNull(cacheManager.getCachedAllowedColumns(resourceId));
        assertNull(cacheManager.getCachedFieldConfigs(resourceId));
    }

    @Test
    void testEvict_nonExistentResource() {
        assertDoesNotThrow(() -> cacheManager.evict(999L));
    }

    @Test
    void testCacheOverwritesPreviousValue() {
        Long resourceId = 1L;
        Set<String> columns1 = new HashSet<>(Arrays.asList("id", "name"));
        Set<String> columns2 = new HashSet<>(Collections.singletonList("email"));

        cacheManager.cacheAllowedColumns(resourceId, columns1);
        cacheManager.cacheAllowedColumns(resourceId, columns2);

        Set<String> cached = cacheManager.getCachedAllowedColumns(resourceId);
        assertEquals(1, cached.size());
        assertTrue(cached.contains("email"));
    }

    @Test
    void testMultipleResourcesIndependent() {
        Long resourceId1 = 1L;
        Long resourceId2 = 2L;

        cacheManager.cacheAllowedColumns(resourceId1, Collections.singleton("id"));
        cacheManager.cacheAllowedColumns(resourceId2, Collections.singleton("name"));

        assertEquals(1, cacheManager.getCachedAllowedColumns(resourceId1).size());
        assertEquals(1, cacheManager.getCachedAllowedColumns(resourceId2).size());
        assertTrue(cacheManager.getCachedAllowedColumns(resourceId1).contains("id"));
        assertTrue(cacheManager.getCachedAllowedColumns(resourceId2).contains("name"));
    }

    @Test
    void testEvictOnlyTargetResource() {
        Long resourceId1 = 1L;
        Long resourceId2 = 2L;

        cacheManager.cacheAllowedColumns(resourceId1, Collections.singleton("id"));
        cacheManager.cacheAllowedColumns(resourceId2, Collections.singleton("name"));

        cacheManager.evict(resourceId1);

        assertNull(cacheManager.getCachedAllowedColumns(resourceId1));
        assertNotNull(cacheManager.getCachedAllowedColumns(resourceId2));
    }
}
