package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 会话索引健康面（spec 39 §C / T140 / impl-113）：装配态 + 首页采样行数
 * （list 无精确 count——首页 size 为近似水位）。索引未装配时本类不应注册
 * （auto-config 条件装配）；恒 UP（索引是查询优化面，故障不构成核心职能失效）。
 *
 * @since 1.0.0
 */
public final class SessionIndexHealth implements BuzhouHealth {

    private final SessionIndexStore index;

    public SessionIndexHealth(SessionIndexStore index) {
        this.index = index;
    }

    @Override
    public String mechanism() {
        return "session-index";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("wired", true);
        try {
            int sampled = index.list(new SessionIndexQuery(
                    null, null, null, null, null, 0, 1)).size();
            details.put("hasRows", sampled > 0); // 采样探测（免全量 count）
        } catch (RuntimeException e) {
            details.put("probeError", String.valueOf(e.getMessage())); // 查询面故障可见但不 DOWN
        }
        return details;
    }
}
