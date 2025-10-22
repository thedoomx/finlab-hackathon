package org.hackathon.finlabvalidator.application;

import org.hackathon.finlabvalidator.persistence.domain.IbanStatus;
import reactor.core.publisher.Mono;

public interface IAccountService {
    Mono<IbanStatus> validate(String iban);
}
