package com.example;

import com.example.rpc.KVStoreClient;
import com.example.rpc.RPCServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestKVStore {

    private static RPCServer kvServer;
    private static List<RPCServer> paxosServers = new ArrayList<>();
    private static List<Thread> paxosThreads = new ArrayList<>();
    private static Thread kvServerThread;

    @BeforeClass
    public static void setUp() throws IOException, InterruptedException, RocksDBException {
        // 使用共享的 RocksDB 路径
        String sharedDbPath = "D:/bigdata_kv/kv/data/shareddb";

        // 清理之前的 RocksDB 目录（确保没有其他进程使用）
        // 注意：实际生产环境中请谨慎操作，避免误删重要数据
        java.nio.file.Path path = java.nio.file.Paths.get(sharedDbPath);
        if (java.nio.file.Files.exists(path)) {
            java.nio.file.Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .map(java.nio.file.Path::toFile)
                .forEach(java.io.File::delete);
            System.out.println("Cleaned existing RocksDB directory: " + sharedDbPath);
        }

        // 定义 Paxos Acceptor 端口
        List<Integer> paxosPorts = Arrays.asList(50052, 50054, 50056);
        List<String> acceptorAddresses = new ArrayList<>();
        for (int port : paxosPorts) {
            acceptorAddresses.add("127.0.0.1:" + port);
        }

        // 启动 Paxos Acceptor 服务器
        for (int port : paxosPorts) {
            RPCServer paxosServer = new RPCServer();
            Thread paxosThread = new Thread(() -> {
                try {
                    paxosServer.startPaxosOnlyServer(port);
                    paxosServer.blockUntilShutdown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            paxosThread.start();
            paxosServers.add(paxosServer);
            paxosThreads.add(paxosThread);
        }

        // 启动 KVStore 服务器
        kvServer = new RPCServer();
        kvServerThread = new Thread(() -> {
            try {
                kvServer.startKVServer(50051, sharedDbPath, acceptorAddresses);
                kvServer.blockUntilShutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        kvServerThread.start();

        // 等待服务器启动
        Thread.sleep(5000);
    }

    @AfterClass
    public static void tearDown() {
        // 停止 KVStore 服务器
        if (kvServer != null) {
            kvServer.stop();
        }
        if (kvServerThread != null) {
            kvServerThread.interrupt();
        }

        // 停止 Paxos Acceptor 服务器
        for (RPCServer paxosServer : paxosServers) {
            paxosServer.stop();
        }
        for (Thread paxosThread : paxosThreads) {
            paxosThread.interrupt();
        }

        // 确保所有线程都已停止
        for (Thread paxosThread : paxosThreads) {
            try {
                paxosThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            kvServerThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPutAndGet() {
        // 连接到 KVStore Server
        KVStoreClient client = new KVStoreClient("127.0.0.1", 50051);
        boolean success = client.put("testKey", "testValue");
        assertTrue(success);

        // 等待 Paxos 学习
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 检查 KVStore Server
        KVStoreClient client2 = new KVStoreClient("127.0.0.1", 50051);
        String value1 = client2.get("testKey");

        assertEquals("testValue", value1);
    }

    @Test
    public void testDelete() {
        // 连接到 KVStore Server
        KVStoreClient client = new KVStoreClient("127.0.0.1", 50051);
        boolean putSuccess = client.put("deleteKey", "toDelete");
        assertTrue(putSuccess);

        // 等待 Paxos 学习
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean deleted = client.delete("deleteKey");
        assertTrue(deleted);

        // 等待 Paxos 学习
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 检查 KVStore Server
        KVStoreClient client2 = new KVStoreClient("127.0.0.1", 50051);
        String value1 = client2.get("deleteKey");

        assertNull(value1);
    }
}
