package com.example.paxos;

import com.example.rpc.Kvstore;
import com.example.rpc.PaxosServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicLong;

public class PaxosAcceptor extends PaxosServiceGrpc.PaxosServiceImplBase {

    private final AtomicLong highestPromisedProposal = new AtomicLong(0);
    private final AtomicLong highestAcceptedProposal = new AtomicLong(0);
    private String acceptedValue = null;

    @Override
    public void prepare(Kvstore.PrepareRequest req, StreamObserver<Kvstore.PrepareResponse> responseObserver) {
        long proposalNumber = req.getProposalNumber();
        System.out.println("Acceptor received Prepare request with proposal number: " + proposalNumber);

        if (proposalNumber > highestPromisedProposal.get()) {
            highestPromisedProposal.set(proposalNumber);
            Kvstore.PrepareResponse response = Kvstore.PrepareResponse.newBuilder().setPromise(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            System.out.println("Acceptor promised proposal number: " + proposalNumber);
        } else {
            Kvstore.PrepareResponse response = Kvstore.PrepareResponse.newBuilder().setPromise(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            System.out.println("Acceptor rejected proposal number: " + proposalNumber);
        }
    }

    @Override
    public void accept(Kvstore.AcceptRequest req, StreamObserver<Kvstore.AcceptResponse> responseObserver) {
        long proposalNumber = req.getProposalNumber();
        String value = req.getValue();
        System.out.println("Acceptor received Accept request with proposal number: " + proposalNumber + " and value: " + value);

        if (proposalNumber >= highestPromisedProposal.get()) {
            highestPromisedProposal.set(proposalNumber);
            highestAcceptedProposal.set(proposalNumber);
            acceptedValue = value;
            Kvstore.AcceptResponse response = Kvstore.AcceptResponse.newBuilder().setAccepted(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            System.out.println("Acceptor accepted proposal number: " + proposalNumber + " with value: " + value);
        } else {
            Kvstore.AcceptResponse response = Kvstore.AcceptResponse.newBuilder().setAccepted(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            System.out.println("Acceptor rejected Accept request with proposal number: " + proposalNumber);
        }
    }

    @Override
    public void learn(Kvstore.LearnRequest req, StreamObserver<Kvstore.LearnResponse> responseObserver) {
        String value = req.getValue();
        System.out.println("Acceptor received Learn request with value: " + value);
        // 这里可以实现将学习到的值应用到存储中，但不涉及 RocksDB
        try {
            // 假设有一个共享的 RocksDBStorage 实例
            // 这里没有具体实现，请根据您的项目需求实现
            System.out.println("Acceptor learned the value: " + value);
            Kvstore.LearnResponse response = Kvstore.LearnResponse.newBuilder().setSuccess(true).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            System.err.println("Error during learning value: " + value);
            e.printStackTrace();
            Kvstore.LearnResponse response = Kvstore.LearnResponse.newBuilder().setSuccess(false).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
