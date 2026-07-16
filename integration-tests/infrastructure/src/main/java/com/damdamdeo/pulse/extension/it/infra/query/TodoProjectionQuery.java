package com.damdamdeo.pulse.extension.it.infra.query;

import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProvider;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierProviderException;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepository;
import com.damdamdeo.pulse.extension.core.connectionidentifier.ConnectionIdentifierRepositoryException;
import com.damdamdeo.pulse.extension.core.event.Identifiable;
import com.damdamdeo.pulse.extension.core.event.OwnedBy;
import com.damdamdeo.pulse.extension.core.query.*;
import com.damdamdeo.pulse.extension.it.domain.ListTodos;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class TodoProjectionQuery implements Query<ListTodos, TodoProjection> {

    @Inject
    ProjectionFromEventStore<TodoProjection> todoProjectionProjectionFromEventStore;

    @Inject
    ConnectionIdentifierProvider connectionIdentifierProvider;

    @Inject
    ConnectionIdentifierRepository connectionIdentifierRepository;

    private static final MultipleResultProjectionQuery<ListTodos> MULTIPLE_RESULT_PROJECTION_QUERY = (passphrase, ownedBy, input) -> {
        Objects.requireNonNull(passphrase);
        Objects.requireNonNull(ownedBy);
        // language=sql
        return """
                WITH decrypted AS (
                  SELECT
                    aggregate_root_id,
                    aggregate_root_type,
                    public.pgp_sym_decrypt(aggregate_root_payload, '%1$s')::jsonb AS decrypted_aggregate_root_payload,
                    belongs_to,
                    owned_by
                  FROM aggregate_root
                  WHERE owned_by = '%2$s'
                )
                SELECT jsonb_build_object(
                  'todoId', d.decrypted_aggregate_root_payload -> 'id',
                  'description', d.decrypted_aggregate_root_payload ->> 'description',
                  'status', d.decrypted_aggregate_root_payload ->> 'status',
                  'important', d.decrypted_aggregate_root_payload ->> 'important',
                  'checklist', COALESCE(
                    jsonb_agg(
                      jsonb_build_object(
                        'todoChecklistId', i.decrypted_aggregate_root_payload -> 'id',
                        'description', i.decrypted_aggregate_root_payload ->> 'description'
                      )
                    ), '[]'::jsonb
                  )
                ) AS response
                FROM decrypted d
                LEFT JOIN decrypted i
                  ON i.belongs_to = d.aggregate_root_id
                 AND i.aggregate_root_type = 'TodoChecklist'
                WHERE d.aggregate_root_type = 'Todo'
                  AND d.owned_by = '%2$s'
                GROUP BY d.aggregate_root_id, d.aggregate_root_type, d.decrypted_aggregate_root_payload, d.belongs_to
                ORDER BY d.aggregate_root_id ASC;
                """.formatted(new String(passphrase.passphrase()), ownedBy.id());
    };

    @Override
    public Result<TodoProjection> execute(final ListTodos input) throws QueryException {
        Objects.requireNonNull(input);
        try {
            Optional<Identifiable> identifiable = connectionIdentifierRepository.find(connectionIdentifierProvider.provide());
            Validate.validState(identifiable.isPresent());
            return todoProjectionProjectionFromEventStore.findAllBy(OwnedBy.from(identifiable.get()), input, MULTIPLE_RESULT_PROJECTION_QUERY);
        } catch (final ConnectionIdentifierProviderException | ConnectionIdentifierRepositoryException exception) {
            throw new QueryException(exception);
        }
    }

    @Override
    public List<Audience> audiences() {
        return List.of(Audience.EVERYONE);
    }
}
