package com.damdamdeo.pulse.extension.it.query.todo;

import com.damdamdeo.pulse.extension.core.TodoId;
import com.damdamdeo.pulse.extension.core.query.TodoProjection;
import com.damdamdeo.pulse.extension.core.query.TodoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
@Transactional
public class TodoJdbcProjectionRepository implements TodoRepository {

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void store(final TodoProjection todoProjection) {
        Objects.requireNonNull(todoProjection);
        // language=sql
        final String sql = """
                INSERT INTO todo_projection (todo_id, projection)
                VALUES (?, CAST(? AS JSONB))
                ON CONFLICT (todo_id) DO UPDATE SET projection = EXCLUDED.projection
                """;
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, todoProjection.id().linkTo().id());
            statement.setString(2, objectMapper.writeValueAsString(todoProjection));
            statement.executeUpdate();
        } catch (final SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to store Todo projection", exception);
        }
    }

    @Override
    public TodoProjection getByTodoId(final TodoId todoId) {
        Objects.requireNonNull(todoId);
        // language=sql
        final String sql = "SELECT projection FROM todo_projection WHERE todo_id = ?";
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, todoId.id());
            try (final ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? readProjection(resultSet) : null;
            }
        } catch (final SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to retrieve Todo projection", exception);
        }
    }

    @Override
    public Collection<TodoProjection> findAll() {
        // language=sql
        final String sql = "SELECT projection FROM todo_projection ORDER BY todo_id";
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql);
             final ResultSet resultSet = statement.executeQuery()) {
            final List<TodoProjection> projections = new ArrayList<>();
            while (resultSet.next()) {
                projections.add(readProjection(resultSet));
            }
            return List.copyOf(projections);
        } catch (final SQLException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to list Todo projections", exception);
        }
    }

    private TodoProjection readProjection(final ResultSet resultSet) throws SQLException, JsonProcessingException {
        return objectMapper.readValue(resultSet.getString("projection"), TodoProjection.class);
    }

}
