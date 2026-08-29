package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 多 sink webhook 扇出（spec 151 / T507，Kafka 多消费者组借鉴）：N 个
 * {@link WebhookEventForwarder} sink（各自 url/secret/include-types/outbox/退避/
 * 死信<b>完全独立</b>），onEvent 广播全部——各 sink 按自己的类型过滤入队。
 *
 * <p>每 sink 独立 stateStore（outbox 隔离——一 sink 积压不挤占另一 sink）；
 * 不用 fanout = 单 forwarder 原样（零变化）。零新投递逻辑：签名/幂等/退避/死信
 * 全部复用 forwarder 既有实现。
 */
public final class WebhookFanout implements SessionEventListener, AutoCloseable {

    private final List<WebhookEventForwarder> sinks;

    public WebhookFanout(Collection<WebhookEventForwarder> sinks) {
        this.sinks = List.copyOf(sinks == null ? List.of() : sinks);
    }

    /** 扇出构造：sink + 其类型订阅（null/空 = 全投递）。 */
    public static WebhookFanout of(Collection<WebhookEventForwarder> sinks,
                                   Collection<String> includeTypes) {
        if (includeTypes != null && !includeTypes.isEmpty()) {
            sinks.forEach(s -> s.setIncludeTypes(includeTypes));
        }
        return new WebhookFanout(sinks);
    }

    @Override
    public void onEvent(SessionEvent event) {
        List<WebhookEventForwarder> snapshot = new ArrayList<>(sinks);
        for (WebhookEventForwarder sink : snapshot) {
            sink.onEvent(event); // 各 sink 自行过滤/入队/投递（异常互不传染——forwarder 入队面不抛）
        }
    }

    /** sink 数（观测/测试）。 */
    public int sinkCount() {
        return sinks.size();
    }

    /** 逐 sink 关闭（各自排空语义保持）。 */
    @Override
    public void close() {
        sinks.forEach(WebhookEventForwarder::close);
    }
}
