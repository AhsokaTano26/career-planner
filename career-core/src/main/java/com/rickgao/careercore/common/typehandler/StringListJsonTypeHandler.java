package com.rickgao.careercore.common.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rickgao.careercore.common.util.JsonUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 字符串数组(JSON 数组)列 TypeHandler,用于 List&lt;String&gt; 类型。
 */
public class StringListJsonTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper MAPPER = JsonUtil.getMapper();
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("JSON 序列化失败", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<String> parse(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(content, LIST_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("JSON 数组解析失败: " + content, e);
        }
    }
}
