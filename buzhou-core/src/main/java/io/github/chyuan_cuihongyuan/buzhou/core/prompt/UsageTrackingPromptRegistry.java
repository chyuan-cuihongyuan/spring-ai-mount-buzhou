package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 使用记账提示词注册表装饰器（spec 424 / T739，Langfuse prompt analytics
 * 借鉴）：三个 resolve 形态（latest/标签/钉版）命中即 {@link PromptUsageStats}
 * 记账——哪个提示词哪个版本被用到多少次可答，晋级/退役由使用数据说话。
 * resolve miss / publish / label 不记账（不是使用）；其余方法纯透传。
 */
public final class UsageTrackingPromptRegistry implements PromptRegistry {

    private final PromptRegistry delegate;
    private final PromptUsageStats stats;

    public UsageTrackingPromptRegistry(PromptRegistry delegate, PromptUsageStats stats) {
        if (delegate == null || stats == null) {
            throw new IllegalArgumentException("delegate/stats 必须非空");
        }
        this.delegate = delegate;
        this.stats = stats;
    }

    /** 绑定的统计 holder（宿主快照/导出用）。 */
    public PromptUsageStats stats() {
        return stats;
    }

    @Override
    public PromptVersion publish(String name, String body, String note) {
        return delegate.publish(name, body, note);
    }

    @Override
    public void label(String name, String label, int version) {
        delegate.label(name, label, version);
    }

    @Override
    public Optional<PromptVersion> resolve(String name) {
        return delegate.resolve(name).map(v -> {
            stats.record(v.name(), v.version());
            return v;
        });
    }

    @Override
    public Optional<PromptVersion> resolve(String name, String label) {
        return delegate.resolve(name, label).map(v -> {
            stats.record(v.name(), v.version());
            return v;
        });
    }

    @Override
    public Optional<PromptVersion> resolveVersion(String name, int version) {
        return delegate.resolveVersion(name, version).map(v -> {
            stats.record(v.name(), v.version());
            return v;
        });
    }

    @Override
    public Map<String, Integer> labels(String name) {
        return delegate.labels(name);
    }

    @Override
    public List<PromptVersion> versions(String name) {
        return delegate.versions(name);
    }

    @Override
    public Set<String> names() {
        return delegate.names();
    }
}
