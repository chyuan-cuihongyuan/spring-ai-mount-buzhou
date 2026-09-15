package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * 评测集内容指纹（L 会话 1700 系 R5 = effort #1704 / spec 1704 /
 * 票 T2609 + T2610 / impl 1304）——DVC / HuggingFace Datasets 的数据集内容
 * 指纹思想：评测集「改名不改内容」或「悄悄改题」都该被同一个指纹显形——
 * 跨 run 的分数对比必须先确认对比的是同一份数据。
 *
 * <p>纯函数零状态：规范形 = 逐项以 \n 连接（UTF-8）→ SHA-256 →
 * {@code sha256-<64 hex>}。序敏感（ORDERED）与序不敏感（UNORDERED——字典序
 * 排序后摘要）两种口径。
 *
 * @since 1.0.0
 */
public final class EvalSetFingerprint {

    private EvalSetFingerprint() {
    }

    /** 序口径闭集。 */
    public enum OrderSensitivity { ORDERED, UNORDERED }

    /** 内容指纹：{@code sha256-<64 hex>}；null/空表得空表指纹（稳定常量）。 */
    public static String of(List<String> items, OrderSensitivity sensitivity) {
        List<String> data = new ArrayList<>(items == null ? List.of() : items);
        if (sensitivity == OrderSensitivity.UNORDERED) {
            data.sort(String::compareTo);
        }
        String canonical = String.join("\n", data);
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用（JVM 规范必备算法）", e);
        }
        StringBuilder hex = new StringBuilder("sha256-");
        for (byte b : digest.digest(canonical.getBytes(StandardCharsets.UTF_8))) {
            hex.append(Character.forDigit((b >> 4) & 0xf, 16));
            hex.append(Character.forDigit(b & 0xf, 16));
        }
        return hex.toString();
    }

    /** 序敏感指纹（默认口径）。 */
    public static String of(List<String> items) {
        return of(items, OrderSensitivity.ORDERED);
    }
}
