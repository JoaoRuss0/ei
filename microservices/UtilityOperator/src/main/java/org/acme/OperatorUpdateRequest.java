package org.acme;

public record OperatorUpdateRequest(
    String location,
    String name,
    String iban
){}
