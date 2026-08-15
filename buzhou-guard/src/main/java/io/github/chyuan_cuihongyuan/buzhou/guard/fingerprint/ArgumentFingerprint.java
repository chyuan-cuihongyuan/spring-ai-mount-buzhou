package io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * 参数指纹（spec 07 授权粒度）：危险参数规范化 JSON 的 SHA-256 前 16 hex。
 *
 * <p>授权粒度 = 工具名 + 参数指纹，体现在 auth state key = {@code auth.{toolName}.{fingerprint}}；
 * 指纹本身只对参数哈希（spec 公式 {@code SHA-256(canonicalJson(arguments))} 前 16 hex）。
 *
 * <p>规范化 JSON：键按字典序递归排列（含 List 元素内的 Map），保证同语义参数产生稳定指纹。
 */
public final class ArgumentFingerprint {

    /** auth state key 前缀（对应 state {@code auth.*} 命名空间）。 */
    public static final String AUTH_KEY_PREFIX = "auth.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ArgumentFingerprint() {
    }

    /** 计算参数的指纹哈希（SHA-256 前 16 hex；spec 公式，工具名不入哈希材质）。 */
    public static String fingerprint(Map<String, Object> arguments) {
        return sha256Hex16(canonicalJson(arguments));
    }

    /** 构造 auth state key。 */
    public static String authKey(String toolName, String fingerprint) {
        return AUTH_KEY_PREFIX + toolName + "." + fingerprint;
    }

    /** 规范化 JSON：TreeMap 排序键后序列化。 */
    @SuppressWarnings("unchecked")
    public static String canonicalJson(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }
        try {
            Object sorted = sortKeys(arguments);
            return MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            // 退化：用 toString 保证仍有指纹（不期望发生）
            return String.valueOf(arguments);
        }
    }

    private static String sha256Hex16(String material) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) { // 前 8 字节 = 16 hex 字符
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.CONFIG_INVALID,
                    "运行环境缺少 SHA-256（JVM 安全提供者异常）", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object sortKeys(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), sortKeys(v)));
            return sorted;
        }
        if (value instanceof Iterable<?> list) {
            java.util.List<Object> sortedList = new java.util.ArrayList<>();
            list.forEach(item -> sortedList.add(sortKeys(item)));
            return sortedList;
        }
        return value;
    }
}
