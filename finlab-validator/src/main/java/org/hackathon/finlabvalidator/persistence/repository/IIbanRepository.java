package org.hackathon.finlabvalidator.persistence.repository;

import org.hackathon.finlabvalidator.persistence.domain.IbanDto;
import org.hackathon.finlabvalidator.persistence.domain.PaginatedResult;

import java.util.Optional;

public interface IIbanRepository {
    Optional<IbanDto> findByIban(String iban);
    Optional<IbanDto> findFirstIban();
    PaginatedResult<IbanDto> findAll(int page, int pageSize);
}
