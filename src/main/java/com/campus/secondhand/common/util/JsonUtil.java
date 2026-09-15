package com.campus.secondhand.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 统一的 JSON 序列化工具,用于构造写入 JSON 列的字符串(如 admin_operation_logs.operation_detail)。
 * 禁止手工拼接 JSON 字符串:用户输入中的引号/反斜杠/控制字符会生成非法 JSON,
 * 在 MySQL JSON 列上直接报错并导致所在事务整体回滚。
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(Map<String, Object> payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize operation detail to JSON", ex);
        }
    }
}
