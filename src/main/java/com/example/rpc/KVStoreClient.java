package com.example.rpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class KVStoreClient {
    private final KVStoreServiceGrpc.KVStoreServiceBlockingStub blockingStub;

    public KVStoreClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = KVStoreServiceGrpc.newBlockingStub(channel);
    }

    public boolean put(String key, String value) {
        Kvstore.PutRequest request = Kvstore.PutRequest.newBuilder()
                .setKey(key)
                .setValue(value)
                .build();
        try {
            Kvstore.PutResponse response = blockingStub.put(request);
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String get(String key) {
        Kvstore.GetRequest request = Kvstore.GetRequest.newBuilder()
                .setKey(key)
                .build();
        try {
            Kvstore.GetResponse response = blockingStub.get(request);
            if (response.getFound()) {
                return response.getValue();
            } else {
                return null;
            }
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean delete(String key) {
        Kvstore.DeleteRequest request = Kvstore.DeleteRequest.newBuilder()
                .setKey(key)
                .build();
        try {
            Kvstore.DeleteResponse response = blockingStub.delete(request);
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }
}
