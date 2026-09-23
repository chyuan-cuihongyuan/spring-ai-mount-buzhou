package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/**
 * Cache-Control 指令裁决（spec 4048 / T6097 / impl 2149）——
 * RFC 9111 §5.2 指令语义：解析（逗号分隔、大小写不敏感、值
 * 容忍引号；未知指令按 RFC 语义**忽略但记录**）+ 优先级裁决
 * {@code judge}：
 * <ol>
 *   <li>no-store → NOT_CACHEABLE（最高——存都不许）；</li>
 *   <li>no-cache → MUST_REVALIDATE（可存但每用必验）；</li>
 *   <li>新鲜度上限 = shared ? (s-maxage 优先于 max-age) :
 *       max-age；age &lt; 上限 → FRESH，否则 STALE。</li>
 * </ol>
 * 自造 TTL 语义（指令被无视）与裸字符串匹配（大小写/引号
 * 各行其是）的病解；嵌套 {@link Directives} 不另立面。
 */
public final class CacheControlDirectives {

    private static final String DIRECTIVE_NO_STORE = "no-store";
    private static final String DIRECTIVE_NO_CACHE = "no-cache";
    private static final String DIRECTIVE_MAX_AGE = "max-age";
    private static final String DIRECTIVE_S_MAXAGE = "s-maxage";
    private static final String DIRECTIVE_IMMUTABLE = "immutable";

    private CacheControlDirectives() {
    }

    /** 裁决四态。 */
    public enum Verdict {
        NOT_CACHEABLE, MUST_REVALIDATE, FRESH, STALE
    }

    /**
     * 解析后的指令集（不可变）。
     *
     * @param noStore no-store 存在
     * @param noCache no-cache 存在
     * @param immutable immutable 存在
     * @param maxAge max-age 秒数（缺省空）
     * @param sMaxAge s-maxage 秒数（缺省空）
     * @param unknown 被忽略的未知指令（RFC 语义：忽略但记录）
     */
    public record Directives(boolean noStore, boolean noCache, boolean immutable,
            OptionalLong maxAge, OptionalLong sMaxAge, List<String> unknown) {
    }

    /** 解析 Cache-Control 头（null 按空处理；负 delta-seconds fail-fast）。 */
    public static Directives parse(String header) {
        boolean noStore = false;
        boolean noCache = false;
        boolean immutable = false;
        OptionalLong maxAge = OptionalLong.empty();
        OptionalLong sMaxAge = OptionalLong.empty();
        List<String> unknown = new ArrayList<>();
        if (header == null || header.isBlank()) {
            return new Directives(false, false, false, maxAge, sMaxAge, List.of());
        }
        for (String raw : header.split(",")) {
            String directive = raw.strip();
            if (directive.isEmpty()) {
                continue;
            }
            int equals = directive.indexOf('=');
            String name = (equals < 0 ? directive : directive.substring(0, equals))
                    .strip().toLowerCase();
            String value = equals < 0 ? null
                    : directive.substring(equals + 1).strip().replace("\"", "");
            switch (name) {
                case DIRECTIVE_NO_STORE -> noStore = true;
                case DIRECTIVE_NO_CACHE -> noCache = true;
                case DIRECTIVE_IMMUTABLE -> immutable = true;
                case DIRECTIVE_MAX_AGE -> maxAge = OptionalLong.of(deltaSeconds(value, header));
                case DIRECTIVE_S_MAXAGE -> sMaxAge = OptionalLong.of(deltaSeconds(value, header));
                default -> unknown.add(name);
            }
        }
        return new Directives(noStore, noCache, immutable, maxAge, sMaxAge, List.copyOf(unknown));
    }

    /**
     * 新鲜度裁决（优先级：no-store > no-cache > 新鲜度上限）。
     *
     * @param directives 响应指令
     * @param ageSeconds 响应年龄（秒）
     * @param sharedCache 是否共享缓存（共享时 s-maxage 优先于 max-age）
     */
    public static Verdict judge(Directives directives, long ageSeconds, boolean sharedCache) {
        if (directives.noStore()) {
            return Verdict.NOT_CACHEABLE;
        }
        if (directives.noCache()) {
            return Verdict.MUST_REVALIDATE;
        }
        OptionalLong limit = sharedCache && directives.sMaxAge().isPresent()
                ? directives.sMaxAge()
                : directives.maxAge();
        if (limit.isEmpty() || ageSeconds < limit.getAsLong()) {
            return Verdict.FRESH;   // 无上限 = 启发式可存（本件判 FRESH）
        }
        return Verdict.STALE;
    }

    /** delta-seconds（非负整数；负值/非法 fail-fast）。 */
    private static long deltaSeconds(String value, String header) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("delta-seconds 缺值：" + header);
        }
        long seconds;
        try {
            seconds = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("delta-seconds 非法 '" + value + "'：" + header);
        }
        if (seconds < 0) {
            throw new IllegalArgumentException("delta-seconds 需非负：" + header);
        }
        return seconds;
    }
}
