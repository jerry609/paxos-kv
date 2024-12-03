package com.example.rpc;

import com.example.paxos.PaxosProposer;
import com.example.storage.RocksDBStorage;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class KVStoreServiceImpl extends KVStoreServiceGrpc.KVStoreServiceImplBase {

    private final RocksDBStorage storage;
    private final List<String> acceptorAddresses;

    public KVStoreServiceImpl(RocksDBStorage storage, List<String> acceptorAddresses) {
        this.storage = storage;
        this.acceptorAddresses = acceptorAddresses;
    }

    @Override
    public void put(Kvstore.PutRequest req, StreamObserver<Kvstore.PutResponse> responseObserver) {
        String key = req.getKey();
        String value = req.getValue();
        String operation = "PUT:" + key + ":" + value;

        System.out.println("Received PUT request: key=" + key + ", value=" + value);

        PaxosProposer proposer = new PaxosProposer(operation, acceptorAddresses, storage);
        boolean success = proposer.propose();

        Kvstore.PutResponse response = Kvstore.PutResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if (success) {
            System.out.println("PUT operation proposed and learned successfully: " + operation);
        } else {
            System.err.println("PUT operation failed: " + operation);
        }
    }

    @Override
    public void get(Kvstore.GetRequest req, StreamObserver<Kvstore.GetResponse> responseObserver) {
        String key = req.getKey();
        System.out.println("Received GET request for key: " + key);
        try {
            String value = storage.get(key);
            if (value != null) {
                Kvstore.GetResponse response = Kvstore.GetResponse.newBuilder().setValue(value).setFound(true).build();
                responseObserver.onNext(response);
                System.out.println("GET response: key=" + key + ", value=" + value);
            } else {
                Kvstore.GetResponse response = Kvstore.GetResponse.newBuilder().setFound(false).build();
                responseObserver.onNext(response);
                System.out.println("GET response: key=" + key + " not found.");
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            Kvstore.GetResponse response = Kvstore.GetResponse.newBuilder().setFound(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            System.err.println("Error during GET operation for key: " + key);
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Kvstore.DeleteRequest req, StreamObserver<Kvstore.DeleteResponse> responseObserver) {
        String key = req.getKey();
        String operation = "DELETE:" + key;

        System.out.println("Received DELETE request for key: " + key);

        PaxosProposer proposer = new PaxosProposer(operation, acceptorAddresses, storage);
        boolean success = proposer.propose();

        Kvstore.DeleteResponse response = Kvstore.DeleteResponse.newBuilder().setSuccess(success).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if (success) {
            System.out.println("DELETE operation proposed and learned successfully: " + operation);
        } else {
            System.err.println("DELETE operation failed: " + operation);
        }
    }
}
