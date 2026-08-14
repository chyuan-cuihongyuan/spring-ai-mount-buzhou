package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读快照端点 {@code /actuator/buzhou}（impl-41 / spec 13 §T66）：聚合容器内全部
 * {@link BuzhouHealth} 机制贡献——运维一屏看全 buzhou 各机制状态与有界详情
 * （不写操作、无敏感字段：审计密钥/会话内容绝不出现）。
 */
@Endpoint(id = "buzhou")
public final class BuzhouHealthEndpoint {

    private final List<BuzhouHealth> contributors;

    public BuzhouHealthEndpoint(List<BuzhouHealth> contributors) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
    }

    @ReadOperation
    public Map<String, Object> buzhouSnapshot() {
        Map<String, Object> mechanisms = new LinkedHashMap<>();
        for (BuzhouHealth contributor : contributors) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", contributor.status().name());
            entry.put("details", safeDetails(contributor));
            mechanisms.put(contributor.mechanism(), entry);
        }
        return Map.of("mechanisms", mechanisms);
    }

    private Map<String, Object> safeDetails(BuzhouHealth contributor) {
        try {
            return contributor.details();
        } catch (RuntimeException e) {
            return Map.of("detailsError", String.valueOf(e.getMessage()));
        }
    }
}
