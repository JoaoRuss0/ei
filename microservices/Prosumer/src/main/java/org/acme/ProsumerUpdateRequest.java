package org.acme;

public record ProsumerUpdateRequest(
    String name,
    Long FiscalNumber,
    String location
){}
