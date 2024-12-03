package com.example;

import com.example.storage.RocksDBStorage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDBException;

import static org.junit.Assert.*;

public class RocksDBStorageTest {

    private RocksDBStorage storage;
    private String dbPath = "D:/bigdata_kv/kv/data/testdb_storage";

    @Before
    public void setUp() throws RocksDBException {
        storage = new RocksDBStorage(dbPath);
    }

    @After
    public void tearDown() {
        storage.close();
    }

    @Test
    public void testPutAndGet() throws RocksDBException {
        storage.put("key1", "value1");
        String value = storage.get("key1");
        assertEquals("value1", value);
    }

    @Test
    public void testDelete() throws RocksDBException {
        storage.put("key2", "value2");
        storage.delete("key2");
        String value = storage.get("key2");
        assertNull(value);
    }
}
