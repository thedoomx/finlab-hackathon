package org.hackathon.finlabvalidator.api.controllers;

import org.hackathon.finlabvalidator.application.IAccountService;
import org.hackathon.finlabvalidator.persistence.domain.IbanStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("${app.api.base-path}/accounts/")
public class AccountController {

    private final IAccountService service;

    public AccountController(IAccountService service) {
        this.service = service;
    }

    @GetMapping("{iban}")
    public Mono<ResponseEntity<IbanStatus>> validate(@PathVariable String iban) {
        return service.validate(iban)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
