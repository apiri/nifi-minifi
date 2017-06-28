package org.apache.nifi.minifi.c2.api;

import java.util.ArrayList;
import java.util.List;

public class OperationRequest {

    private final Operation operation;
    private final String name;
    private List<Object> content;

    public OperationRequest(Operation operation, String name) {
        this.operation = operation;
        this.name = name;
        this.content= new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public Operation getOperation() {
        return operation;
    }

    public List<Object> getContent() {
        return content;
    }

    public boolean addContent(Object o) {
        return content.add(o);
    }
}
