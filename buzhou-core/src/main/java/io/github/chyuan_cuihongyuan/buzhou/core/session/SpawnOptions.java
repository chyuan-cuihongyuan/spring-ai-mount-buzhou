package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ResumeStrategy;

import java.util.List;

/**
 * 会话拉起选项。
 *
 * @param steal          会话已被其他实例持有时是否抢租（崩溃恢复场景：旧实例已死，新实例 steal 接管）
 * @param listeners      会话事件监听器
 * @param resumeStrategy 恢复语义档位的会话级覆盖（{@code null} = 不覆盖，走运行时配置
 *                       {@code buzhou.recovery.resume-strategy}；spec「崩溃中轮次恢复 · 改动面」）
 */
public record SpawnOptions(boolean steal, List<SessionEventListener> listeners, ResumeStrategy resumeStrategy) {

    public SpawnOptions {
        listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    /** 兼容旧签名：{@code resumeStrategy} 置 {@code null}（不覆盖运行时配置档位）。 */
    public SpawnOptions(boolean steal, List<SessionEventListener> listeners) {
        this(steal, listeners, null);
    }

    public static SpawnOptions defaults() {
        return new SpawnOptions(false, List.of());
    }

    public static SpawnOptions withSteal() {
        return new SpawnOptions(true, List.of());
    }

    public SpawnOptions withListeners(SessionEventListener... toAdd) {
        return new SpawnOptions(steal, List.of(toAdd), resumeStrategy);
    }

    /** 派生一个覆盖恢复语义档位的副本（steal / listeners 保持不变）。 */
    public SpawnOptions withResumeStrategy(ResumeStrategy strategy) {
        return new SpawnOptions(steal, listeners, strategy);
    }
}
