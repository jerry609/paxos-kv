package com.example.paxos;

import com.example.rpc.Kvstore;
import com.example.rpc.PaxosServiceGrpc;
import com.example.storage.RocksDBStorage;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.List;

public class PaxosProposer {

    private String proposal;
    private List<String> acceptorAddresses;
    private RocksDBStorage storage; // 新增字段

    // 构造函数接收 RocksDBStorage
    public PaxosProposer(String proposal, List<String> acceptorAddresses, RocksDBStorage storage) {
        this.proposal = proposal;
        this.acceptorAddresses = acceptorAddresses;
        this.storage = storage;
    }

    public boolean propose() {
        // 使用 long 类型的 proposal number
        long proposalNumber = System.currentTimeMillis();
        System.out.println("Proposing operation: " + proposal + " with proposal number: " + proposalNumber);

        // Phase 1: Prepare
        for (String address : acceptorAddresses) {
            String[] parts = address.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();

            PaxosServiceGrpc.PaxosServiceBlockingStub stub = PaxosServiceGrpc.newBlockingStub(channel);

            Kvstore.PrepareRequest request = Kvstore.PrepareRequest.newBuilder()
                    .setProposalNumber(proposalNumber) // 保持为 long 类型
                    .build();

            try {
                System.out.println("Sending Prepare request to " + address);
                Kvstore.PrepareResponse response = stub.prepare(request);
                if (!response.getPromise()) {
                    System.err.println("Acceptor at " + address + " rejected the proposal.");
                    channel.shutdown();
                    return false;
                }
                System.out.println("Acceptor at " + address + " promised proposal number: " + proposalNumber);
            } catch (StatusRuntimeException e) {
                System.err.println("RPC failed when sending Prepare to " + address + ": " + e.getStatus());
                channel.shutdown();
                return false;
            }

            channel.shutdown();
        }

        // Phase 2: Accept
        for (String address : acceptorAddresses) {
            String[] parts = address.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .build();

            PaxosServiceGrpc.PaxosServiceBlockingStub stub = PaxosServiceGrpc.newBlockingStub(channel);

            Kvstore.AcceptRequest request = Kvstore.AcceptRequest.newBuilder()
                    .setProposalNumber(proposalNumber) // 保持为 long 类型
                    .setValue(proposal)
                    .build();

            try {
                System.out.println("Sending Accept request to " + address);
                Kvstore.AcceptResponse response = stub.accept(request); // 正确调用 stub.accept(request)
                if (!response.getAccepted()) {
                    System.err.println("Acceptor at " + address + " did not accept the proposal.");
                    channel.shutdown();
                    return false;
                }
                System.out.println("Acceptor at " + address + " accepted proposal number: " + proposalNumber);
            } catch (StatusRuntimeException e) {
                System.err.println("RPC failed when sending Accept to " + address + ": " + e.getStatus());
                channel.shutdown();
                return false;
            }

            channel.shutdown();
        }

        // Phase 3: Learn
        PaxosLearner learner = new PaxosLearner(proposal, acceptorAddresses, storage); // 传递 storage
        learner.learn();

        System.out.println("Proposed operation successfully: " + proposal);

        return true;
    }
}
