package org.hackathon.finlabvalidator.persistence.domain;

public record IbanDto(
        long id,
        String iban,
        IbanStatus status
){}
