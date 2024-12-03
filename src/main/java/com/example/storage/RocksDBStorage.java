package com.example.storage;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

public class RocksDBStorage {
    static {
        RocksDB.loadLibrary();
    }

    private RocksDB db;

    public RocksDBStorage(String dbPath) throws RocksDBException {
        // 确保父目录存在
        File dbDir = new File(dbPath).getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            boolean created = dbDir.mkdirs();
            if (!created) {
                throw new RocksDBException("Failed to create directory: " + dbDir.getAbsolutePath());
            } else {
                System.out.println("Created directory: " + dbDir.getAbsolutePath());
            }
        } else {
            System.out.println("Directory already exists: " + (dbDir != null ? dbDir.getAbsolutePath() : "null"));
        }
        Options options = new Options().setCreateIfMissing(true);
        db = RocksDB.open(options, dbPath);
        System.out.println("Opened RocksDB at: " + dbPath);
    }

    public void put(String key, String value) throws RocksDBException {
        db.put(key.getBytes(), value.getBytes());
        System.out.println("Stored key: " + key + ", value: " + value);
    }

    public String get(String key) throws RocksDBException {
        byte[] value = db.get(key.getBytes());
        String result = value != null ? new String(value) : null;
        System.out.println("Retrieved key: " + key + ", value: " + result);
        return result;
    }

    public void delete(String key) throws RocksDBException {
        db.delete(key.getBytes());
        System.out.println("Deleted key: " + key);
    }

    public void close() {
        if (db != null) {
            db.close();
            System.out.println("Closed RocksDB.");
        }
    }
}
