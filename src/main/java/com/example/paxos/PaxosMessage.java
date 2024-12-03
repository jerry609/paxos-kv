package com.example.paxos;

import lombok.Data;

@Data
public class PaxosMessage {
    public enum Type {
        PREPARE,
        PROMISE,
        ACCEPT_REQUEST,
        ACCEPTED,
        LEARN
    }

    private Type type;
    private int proposalNumber;
    private int acceptedProposalNumber;
    private String acceptedValue;
    private String value;

    // Constructors, getters, and setters

    public PaxosMessage(Type type, int proposalNumber) {
        this.type = type;
        this.proposalNumber = proposalNumber;
    }

    public PaxosMessage(Type type, int proposalNumber, String value) {
        this.type = type;
        this.proposalNumber = proposalNumber;
        this.value = value;
    }

    public PaxosMessage(Type type, int proposalNumber, int acceptedProposalNumber, String acceptedValue) {
        this.type = type;
        this.proposalNumber = proposalNumber;
        this.acceptedProposalNumber = acceptedProposalNumber;
        this.acceptedValue = acceptedValue;
    }

    // Getters and Setters
    // ...
}
