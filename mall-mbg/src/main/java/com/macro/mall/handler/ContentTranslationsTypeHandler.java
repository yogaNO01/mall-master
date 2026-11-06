package com.macro.mall.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** Explicitly mapped on content columns so other JSON/specification fields stay unchanged. */
public class ContentTranslationsTypeHandler extends BaseTypeHandler<Map<String, Map<String, String>>> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, Map<String, String>>> TYPE = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement statement, int index,
            Map<String, Map<String, String>> value, JdbcType jdbcType) throws SQLException {
        try {
            statement.setString(index, JSON.writeValueAsString(value));
        } catch (IOException exception) {
            throw new SQLException("Invalid content translations", exception);
        }
    }

    @Override
    public Map<String, Map<String, String>> getNullableResult(ResultSet result, String column) throws SQLException {
        return read(result.getString(column));
    }

    @Override
    public Map<String, Map<String, String>> getNullableResult(ResultSet result, int column) throws SQLException {
        return read(result.getString(column));
    }

    @Override
    public Map<String, Map<String, String>> getNullableResult(CallableStatement statement, int column) throws SQLException {
        return read(statement.getString(column));
    }

    private Map<String, Map<String, String>> read(String value) throws SQLException {
        if (value == null || value.isBlank()) return null;
        try {
            return JSON.readValue(value, TYPE);
        } catch (IOException exception) {
            throw new SQLException("Invalid stored content translations", exception);
        }
    }
}
