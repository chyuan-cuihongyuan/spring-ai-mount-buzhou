package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.Arrays;
import java.util.List;

/**
 * ETag 条件请求匹配（spec 2046 / T3193 / impl 1597）——HTTP RFC 7232
 * 思想：查询/导出端点的条件请求复用——If-None-Match 命中回 304（省
 * 全量载荷）、If-Match 未命中回 412（乐观并发防护——他人已改则拒）；
 * 弱 ETag（W/ 前缀）语义内容等价、强 ETag 字节等价。
 *
 * <p>纯函数零状态、确定性。
 */
public final class EntityTagMatcher {

    private static final String WEAK_PREFIX = "W/";

    private EntityTagMatcher() {
    }

    /** 强比较：全等且双方非弱（字节等价口径——If-Match 用）。 */
    public static boolean strongMatch(String etagA, String etagB) {
        if (etagA == null || etagB == null) {
            throw new IllegalArgumentException("etag 不能为 null");
        }
        return !isWeak(etagA) && !isWeak(etagB) && etagA.equals(etagB);
    }

    /** 弱比较：剥 W/ 前缀后全等（语义等价口径——If-None-Match 用）。 */
    public static boolean weakMatch(String etagA, String etagB) {
        if (etagA == null || etagB == null) {
            throw new IllegalArgumentException("etag 不能为 null");
        }
        return stripWeak(etagA).equals(stripWeak(etagB));
    }

    /**
     * If-None-Match 判定（命中 → 调用方回 304 Not Modified）：头为
     * "*"（任意现存即命中）或任一候选标签与当前**弱比较**相等；头
     * null/空 → false（无条件即无命中）。
     */
    public static boolean ifNoneMatchHit(String ifNoneMatchHeader, String currentEtag) {
        return anyMatch(ifNoneMatchHeader, currentEtag, true);
    }

    /**
     * If-Match 判定（未命中 → 调用方回 412 Precondition Failed）：头
     * 为 "*"（任意现存即满足）或任一候选与当前**强比较**相等；头
     * null/空 → false（RFC：If-Match 缺失不做前置检查——调用方区分）。
     */
    public static boolean ifMatchSatisfied(String ifMatchHeader, String currentEtag) {
        return anyMatch(ifMatchHeader, currentEtag, false);
    }

    private static boolean anyMatch(String header, String currentEtag, boolean weak) {
        if (currentEtag == null) {
            throw new IllegalArgumentException("currentEtag 不能为 null");
        }
        if (header == null || header.isBlank()) {
            return false; // 无条件头
        }
        if ("*".equals(header.trim())) {
            return true; // 通配——任意现存资源即命中
        }
        List<String> candidates = splitHeader(header);
        for (String candidate : candidates) {
            boolean matched = weak ? weakMatch(candidate, currentEtag) : strongMatch(candidate, currentEtag);
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitHeader(String header) {
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static boolean isWeak(String etag) {
        return etag.startsWith(WEAK_PREFIX);
    }

    private static String stripWeak(String etag) {
        return isWeak(etag) ? etag.substring(WEAK_PREFIX.length()) : etag;
    }
}
