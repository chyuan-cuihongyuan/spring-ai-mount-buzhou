package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import java.util.List;

/**
 * 策略规则来源 SPI（impl-40 / spec 13 §T64）：classpath / file /（部署侧可扩展 K8s
 * ConfigMap watch 等）。<b>etag = 内容 sha256</b>——HTTP 条件拉取语义（304 = 未变化返回 null）。
 */
public interface PolicySource {

    /**
     * 加载规则集。
     *
     * @param ifNoneMatch 调用方已持有的 etag（null = 首次/强制拉取）
     * @return 内容未变化时返回 <b>null</b>（304 语义）；否则返回新快照（含新 etag）
     * @throws IllegalStateException 来源不可读（由刷新器兜住沿用旧快照）
     */
    Snapshot load(String ifNoneMatch);

    /** 来源描述（日志/观测用）。 */
    String description();

    record Snapshot(String etag, List<PolicyDecision.Rule> rules) {
        public Snapshot {
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }
}
