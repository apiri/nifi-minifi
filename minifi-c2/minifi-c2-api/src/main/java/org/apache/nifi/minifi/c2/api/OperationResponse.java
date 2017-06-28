package org.apache.nifi.minifi.c2.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OperationResponse {

    private final Operation operation;
    private List<OperationRequest> requests;

    public OperationResponse(Operation operation) {
        this.operation = operation;
        this.requests = new ArrayList<>();
    }

    public Operation getOperation() {
        return operation;
    }

    public List<OperationRequest> getRequests() {
        return Collections.unmodifiableList(this.requests);
    }

    public boolean addRequest(OperationRequest operationRequest) {
        return requests.add(operationRequest);
    }
}
