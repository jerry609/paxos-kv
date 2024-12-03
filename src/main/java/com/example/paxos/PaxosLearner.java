package com.example.paxos;

import com.example.storage.RocksDBStorage;
import java.util.List;

public class PaxosLearner {

    private String learnedValue;
    private List<String> acceptorAddresses;
    private RocksDBStorage storage;

    public PaxosLearner(String learnedValue, List<String> acceptorAddresses, RocksDBStorage storage) {
        this.learnedValue = learnedValue;
        this.acceptorAddresses = acceptorAddresses;
        this.storage = storage;
    }

    public void learn() {
        System.out.println("Learning operation: " + learnedValue);
        // 解析并处理操作
        String[] parts = learnedValue.split(":");
        if (parts.length < 2) {
            System.err.println("Invalid operation format: " + learnedValue);
            return;
        }

        String operation = parts[0];
        String key = parts[1];

        try {
            switch (operation) {
                case "PUT":
                    if (parts.length != 3) {
                        System.err.println("Invalid PUT operation format: " + learnedValue);
                        return;
                    }
                    String putValue = parts[2];
                    // 处理 PUT 操作，通过 RocksDBStorage 存储
                    storage.put(key, putValue);
                    System.out.println("Applied PUT operation: key=" + key + ", value=" + putValue);
                    break;
                case "DELETE":
                    // 处理 DELETE 操作，通过 RocksDBStorage 删除
                    storage.delete(key);
                    System.out.println("Applied DELETE operation: key=" + key);
                    break;
                default:
                    System.err.println("Unknown operation: " + operation);
            }
        } catch (Exception e) {
            System.err.println("Failed to apply operation: " + learnedValue);
            e.printStackTrace();
        }
    }
}
