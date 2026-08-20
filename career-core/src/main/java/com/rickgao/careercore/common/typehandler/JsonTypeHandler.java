package com.rickgao.careercore.common.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.util.JsonUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用 JSON 列 TypeHandler。
 * 由 MyBatis 在解析 resultMap 属性时通过 "Class 构造器" 注入目标类型(MyBatis 3.5+ 支持),
 * 也可配合具体类型子类使用。
 */
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private static final ObjectMapper MAPPER = JsonUtil.getMapper();

    private final Class<T> type;

    public JsonTypeHandler() {
        this.type = null;
    }

    public JsonTypeHandler(Class<T> type) {
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 序列化失败", e);
        }
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    @SuppressWarnings("unchecked")
    private T parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            if (type == null) {
                return (T) MAPPER.readValue(content, Object.class);
            }
            return MAPPER.readValue(content, type);
        } catch (Exception e) {
            throw new RuntimeException("JSON 列解析失败: " + content, e);
        }
    }
}
