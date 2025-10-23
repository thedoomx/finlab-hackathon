package org.hackathon.finlabvalidator.persistence.repository;

import org.hackathon.finlabvalidator.persistence.domain.IbanDto;
import org.hackathon.finlabvalidator.persistence.domain.IbanStatus;
import org.hackathon.finlabvalidator.persistence.domain.PaginatedResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class IbanRepository implements IIbanRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String schemaName;

    public IbanRepository(JdbcTemplate jdbcTemplate,
                          @Value("${spring.datasource.schema}") String schemaName) {
        this.jdbcTemplate = jdbcTemplate;
        this.schemaName = schemaName;
    }

    public Optional<IbanDto> findByIban(String iban) {
        String sql = String.format("SELECT id, iban, status FROM %s.iban WHERE iban = ?", schemaName);
        List<IbanDto> results = jdbcTemplate.query(sql, (rs, rowNum) ->
                        new IbanDto(
                                rs.getLong("id"),
                                rs.getString("iban"),
                                IbanStatus.valueOf(rs.getString("status"))
                        )
                , iban);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<IbanDto> findFirstIban() {
        String sql = String.format("SELECT id, iban, status FROM %s.iban ORDER BY id LIMIT 1", schemaName);
        return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new IbanDto(
                        rs.getLong("id"),
                        rs.getString("iban"),
                        IbanStatus.valueOf(rs.getString("status"))
                )
        ));
    }

    @Override
    public PaginatedResult<IbanDto> findAll(int page, int pageSize) {
        String countSql = String.format("SELECT COUNT(*) FROM %s.iban", schemaName);
        Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class);

        if (totalCount == null || totalCount == 0) {
            return new PaginatedResult<>(List.of(), 0, pageSize, page);
        }

        int offset = (page - 1) * pageSize;

        String dataSql = String.format(
                "SELECT id, iban, status FROM %s.iban ORDER BY id LIMIT ? OFFSET ?",
                schemaName
        );

        List<IbanDto> items = jdbcTemplate.query(dataSql, new Object[]{pageSize, offset}, (rs, rowNum) ->
                new IbanDto(
                        rs.getLong("id"),
                        rs.getString("iban"),
                        IbanStatus.valueOf(rs.getString("status"))
                )
        );

        return new PaginatedResult<>(items, totalCount, pageSize, page);
    }
}