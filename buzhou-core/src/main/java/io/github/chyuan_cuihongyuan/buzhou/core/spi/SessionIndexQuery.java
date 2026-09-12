package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 会话索引查询（spec 30 / T109 / impl-84）：按 lastActiveAt 倒序，过滤项 null = 不过滤。
 *
 * @param appId      精确匹配（null = 全部）
 * @param agentName  精确匹配（null = 全部）
 * @param status     {@link SessionInfo} 状态常量（null = 非 DELETED——spec 33 §B 口径，三实现一致排除已删）
 * @param tagKey     标签键（配 tagValue 精确匹配；单独指定无效——需成对）
 * @param tagValue   标签值
 * @param offset     分页偏移
 * @param limit      页大小（≤200）
 * @param cursor     keyset 游标（spec 631 / T912：上一页末行的 encodeCursor——翻页稳定
 *                   不受行活跃度变化跳行/重行；null = 从头；与 offset 可叠加）
 *
 * @since 1.0.0
 */
public record SessionIndexQuery(
        String appId,
        String agentName,
        String status,
        String tagKey,
        String tagValue,
        int offset,
        int limit,
        String cursor) {

    /** 规范序：(lastActiveAt DESC, sessionId ASC)——三实现统一（含平局字典序）。 */
    public static final java.util.Comparator<SessionInfo> CANONICAL_ORDER =
            java.util.Comparator.comparingLong(SessionInfo::lastActiveAtEpochMs).reversed()
                    .thenComparing(SessionInfo::sessionId);

    /** 游标定位（上一页末行的序键）。 */
    public record Cursor(long lastActiveAtEpochMs, String sessionId) {
    }

    public SessionIndexQuery {
        if (limit < 1) {
            limit = 20;
        }
        limit = Math.min(limit, 200);
        offset = Math.max(offset, 0);
        if ((tagKey == null) != (tagValue == null)) {
            throw new IllegalArgumentException("tagKey/tagValue 必须成对指定（sessionId 检索键=值）");
        }
        if (cursor != null) {
            decodeCursor(cursor); // 格式错 fail-fast（不透明但自校验）
        }
    }

    /** 七参兼容构造（无游标——既有调用零变化）。 */
    public SessionIndexQuery(String appId, String agentName, String status,
            String tagKey, String tagValue, int offset, int limit) {
        this(appId, agentName, status, tagKey, tagValue, offset, limit, null);
    }

    /** 全量查询（最近活跃优先，默认页）。 */
    public static SessionIndexQuery defaults() {
        return new SessionIndexQuery(null, null, null, null, null, 0, 20, null);
    }

    /** 编码游标（上一页末行 → 下一页 query.cursor）。 */
    public static String encodeCursor(SessionInfo lastRowOfPreviousPage) {
        if (lastRowOfPreviousPage == null) {
            return null;
        }
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                (lastRowOfPreviousPage.lastActiveAtEpochMs() + ":" + lastRowOfPreviousPage.sessionId())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** 解码游标（格式错 IllegalArgumentException——不透明但自校验）。 */
    public static Cursor decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            throw new IllegalArgumentException("cursor 非空（不传请用 null）");
        }
        String decoded;
        try {
            decoded = new String(java.util.Base64.getUrlDecoder().decode(cursor),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("cursor 非法（Base64 解码失败）：" + cursor, e);
        }
        int sep = decoded.indexOf(':');
        if (sep <= 0 || sep == decoded.length() - 1) {
            throw new IllegalArgumentException("cursor 非法（格式 ms:sessionId）：" + decoded);
        }
        try {
            return new Cursor(Long.parseUnsignedLong(decoded.substring(0, sep)),
                    decoded.substring(sep + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cursor 非法（毫秒段非数字）：" + decoded, e);
        }
    }

    /** 行是否在游标之后（规范序）——实现侧游标过滤共用。 */
    public static boolean afterCursor(SessionInfo info, Cursor cursor) {
        if (cursor == null) {
            return true;
        }
        if (info.lastActiveAtEpochMs() != cursor.lastActiveAtEpochMs()) {
            return info.lastActiveAtEpochMs() < cursor.lastActiveAtEpochMs();
        }
        return info.sessionId().compareTo(cursor.sessionId()) > 0;
    }
}
