package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Redis 键命名空间碰撞审计（spec 705 / T1010，fsck 思想 + E R12 污染教训制度化）：
 * 对键布局做结构性对抗模拟——保留段/冒号后缀注入会话与 span 形状，产出碰撞
 * 证据（Finding）+ 保留段读数 + sessionId 摄入守卫谓词。
 *
 * <p>布局风险是<b>确定性的</b>（结构使然与数据无关）——结果可缓存、可入启动
 * 体检。本面只出证据与守卫：改键形状是破坏性迁移语义，归大版本。
 */
public final class RedisKeyLayoutAudit {

    /** obs 命名空间的保留二级段（与 spans/events/event/spev 形状冲突的 sid 值域）。 */
    public static final Set<String> RESERVED_SEGMENTS =
            Set.of("sessions", "spev", "event", "snap", "spans", "events");

    private RedisKeyLayoutAudit() {
    }

    /** 单条布局发现（kind ∈ RESERVED_SEGMENT / SPAN_INDEX_CLASH / COLON_SUFFIX_TRICK）。 */
    public record Finding(String kind, String key, String conflictWith, String hint) {
    }

    /**
     * 结构性碰撞审计（默认对抗样本；定制 prefix 形状不变——风险与 prefix 无关）。
     */
    public static List<Finding> audit() {
        return audit("buzhou:");
    }

    /**
     * 结构性碰撞审计：三族已知碰撞形状逐一实例化比对。
     *
     * <ul>
     *   <li>RESERVED_SEGMENT——sid 注入保留段后与全局索引键同域（obs:sessions 族的近邻混淆面）；</li>
     *   <li>SPAN_INDEX_CLASH——sid="spev"/"event" + 对端值="spans" 时跨形状<b>完全同串</b>；</li>
     *   <li>COLON_SUFFIX_TRICK——sid 含冒号时租约键与 fencing 计数器键完全同串（HASH vs INCR 类型冲突）。</li>
     * </ul>
     */
    public static List<Finding> audit(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        RedisKeys keys = new RedisKeys(prefix);
        List<Finding> findings = new ArrayList<>();

        // ① sid 保留段：spansOfSession(保留段) 与专用索引键同前缀域——枚举即证据
        for (String reserved : RESERVED_SEGMENTS) {
            findings.add(new Finding("RESERVED_SEGMENT", keys.spansOfSession(reserved),
                    prefix + "obs:" + reserved,
                    "sessionId 不应使用保留段「" + reserved + "」——obs 命名空间二级段冲突面"));
        }

        // ② 跨形状完全同串：sid="spev" + spanId="spans" vs eventsOfSpan("spans")
        String viaSessionSpev = keys.spansOfSession("spev");
        String viaSpanIndex = keys.eventsOfSpan("spans");
        if (viaSessionSpev.equals(viaSpanIndex)) {
            findings.add(new Finding("SPAN_INDEX_CLASH", viaSessionSpev, viaSpanIndex,
                    "sessionId=\"spev\" 时会话 span 索引与 spanId=\"spans\" 的事件索引同串（ZSET 串写）"));
        }
        String viaSessionEvent = keys.spansOfSession("event");
        String viaEventBody = keys.event("spans");
        if (viaSessionEvent.equals(viaEventBody)) {
            findings.add(new Finding("SPAN_INDEX_CLASH", viaSessionEvent, viaEventBody,
                    "sessionId=\"event\" 时会话 span 索引与 eventId=\"spans\" 的事件正文键同串"));
        }

        // ③ 冒号后缀歧义：lease:<a:seq>（租约 HASH）vs lease:<a>:seq（fencing INCR）
        String leaseOfTrickySid = keys.lease("a:seq");
        String fencingOfPlainSid = keys.leaseFencingSeq("a");
        if (leaseOfTrickySid.equals(fencingOfPlainSid)) {
            findings.add(new Finding("COLON_SUFFIX_TRICK", leaseOfTrickySid, fencingOfPlainSid,
                    "sessionId 含冒号时租约键与 fencing 计数器键同串（HASH/INCR 类型冲突+fencing 污染）"
                            + "——摄入前用 isSafeSessionId 守卫"));
        }
        return List.copyOf(findings);
    }

    /** 保留段读数（只读视图）。 */
    public static Set<String> reservedSegments() {
        return RESERVED_SEGMENTS;
    }

    /**
     * sessionId 摄入守卫（从严口径供宿主选用）：非空、无冒号（后缀歧义根因）、
     * 不在保留段、无 glob 元字符（SCAN 转义已兜底但谓词从严）。null → false。
     */
    public static boolean isSafeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        if (sessionId.indexOf(':') >= 0) {
            return false;
        }
        if (RESERVED_SEGMENTS.contains(sessionId)) {
            return false;
        }
        for (int i = 0; i < sessionId.length(); i++) {
            char ch = sessionId.charAt(i);
            if (ch == '*' || ch == '?' || ch == '[' || ch == ']' || ch == '\\') {
                return false;
            }
        }
        return true;
    }
}
