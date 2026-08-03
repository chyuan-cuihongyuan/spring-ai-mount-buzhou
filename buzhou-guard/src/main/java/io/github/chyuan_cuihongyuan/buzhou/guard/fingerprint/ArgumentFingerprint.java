package io.github.chyuan_cuihongyuan.buzhou.guard.fingerprint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * 参数指纹（spec 07 授权粒度）：工具名 + 危险参数规范化 JSON 的 SHA-256 前 16 hex。
 *
 * <p>规范化 JSON：键按字典序排列（TreeMap），保证同语义参数产生稳定指纹。
 * auth state key = {@code auth.{toolName}.{fingerprint}}。
 */
public final class ArgumentFingerprint {

    /** auth state key 前缀（对应 state {@code auth.*} 命名空间）。 */
    public static final String AUTH_KEY_PREFIX = "auth.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ArgumentFingerprint() {
    }

    /** 计算工具名 + 参数的指纹哈希（SHA-256 前 16 hex）。 */
    public static String fingerprint(String toolName, Map<String, Object> arguments) {
        String canonical = canonicalJson(arguments);
        String material = toolName + "|" + canonical;
        return sha256Hex16(material);
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
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object sortKeys(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            map.forEach((k, v) -> sorted.put(String.valueOf(k), sortKeys(v)));
            return sorted;
        }
        return value;
    }
}
