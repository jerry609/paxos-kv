package com.example.rpc;

import com.example.paxos.PaxosAcceptor;
import com.example.storage.RocksDBStorage;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.forAddress;

public class RPCServer {

    private Server kvServer;
    private Server paxosServer;
    private RocksDBStorage storage;

    /**
     * 启动 KVStore Server
     *
     * @param port              KVStore 服务器端口
     * @param dbPath            RocksDB 数据库路径
     * @param acceptorAddresses Paxos Acceptor 服务器地址列表
     */
    public void startKVServer(int port, String dbPath, List<String> acceptorAddresses) throws IOException, RocksDBException {
        storage = new RocksDBStorage(dbPath);
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", port);
        kvServer = forAddress(inetSocketAddress)
                .addService(new KVStoreServiceImpl(storage, acceptorAddresses))
                .build()
                .start();
        System.out.println("KVStore Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down KVStore server");
            RPCServer.this.stop();
            System.out.println("KVStore Server shut down");
        }));
    }

    /**
     * 启动仅 Paxos Acceptor Server
     *
     * @param port Paxos Acceptor 服务器端口
     */
    public void startPaxosOnlyServer(int port) throws IOException {
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", port);
        paxosServer = forAddress(inetSocketAddress)
                .addService(new PaxosAcceptor())
                .build()
                .start();
        System.out.println("Paxos Acceptor Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Paxos Acceptor server");
            paxosServer.shutdown();
            System.out.println("Paxos Acceptor Server shut down");
        }));
    }

    /**
     * 停止所有服务器
     */
    public void stop() {
        if (kvServer != null) {
            kvServer.shutdown();
        }
        if (paxosServer != null) {
            paxosServer.shutdown();
        }
        if (storage != null) {
            storage.close();
        }
    }

    /**
     * 阻塞直到服务器关闭
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (kvServer != null) {
            kvServer.awaitTermination();
        }
        if (paxosServer != null) {
            paxosServer.awaitTermination();
        }
    }

    /**
     * 主方法，启动所有服务器
     */
    public static void main(String[] args) throws IOException, InterruptedException, RocksDBException {
        // 使用绝对路径
        int kvPort = 50051;
        int paxosPort1 = 50052;
        int paxosPort2 = 50054;
        int paxosPort3 = 50056;
        String dbPath = "D:/bigdata_kv/kv/data/rocksdb";

        // 定义 Paxos Acceptor 地址
        List<String> acceptorAddresses = Arrays.asList(
                "127.0.0.1:50052",
                "127.0.0.1:50054",
                "127.0.0.1:50056"
        );

        RPCServer server = new RPCServer();

        // 启动 Paxos Acceptor 服务器
        server.startPaxosOnlyServer(paxosPort1);
        server.startPaxosOnlyServer(paxosPort2);
        server.startPaxosOnlyServer(paxosPort3);

        // 启动 KVStore 服务器
        server.startKVServer(kvPort, dbPath, acceptorAddresses);

        // 阻塞主线程直到服务器关闭
        server.blockUntilShutdown();
    }
}
