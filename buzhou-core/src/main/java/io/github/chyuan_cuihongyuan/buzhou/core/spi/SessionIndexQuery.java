package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 会话索引查询（spec 30 / T109 / impl-84）：按 lastActiveAt 倒序，过滤项 null = 不过滤。
 *
 * @param appId      精确匹配（null = 全部）
 * @param agentName  精确匹配（null = 全部）
 * @param status     {@link SessionInfo} 状态常量（null = 全部）
 * @param tagKey     标签键（配 tagValue 精确匹配；单独指定无效——需成对）
 * @param tagValue   标签值
 * @param offset     分页偏移
 * @param limit      页大小（≤200）
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
        int limit) {

    public SessionIndexQuery {
        if (limit < 1) {
            limit = 20;
        }
        limit = Math.min(limit, 200);
        offset = Math.max(offset, 0);
        if ((tagKey == null) != (tagValue == null)) {
            throw new IllegalArgumentException("tagKey/tagValue 必须成对指定（sessionId 检索键=值）");
        }
    }

    /** 全量查询（最近活跃优先，默认页）。 */
    public static SessionIndexQuery defaults() {
        return new SessionIndexQuery(null, null, null, null, null, 0, 20);
    }
}
