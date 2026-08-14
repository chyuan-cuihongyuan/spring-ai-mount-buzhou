package io.github.chyuan_cuihongyuan.buzhou.guard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.AuditChain;
import io.github.chyuan_cuihongyuan.buzhou.guard.audit.SigningKeyRing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * guard 生命周期（impl-30 / spec 13 §core-1）：phase =
 * {@link BuzhouLifecyclePhases#GUARD}（core/memory/spill 之后、store 之前停）。
 *
 * <p>impl-39 / spec 13 §T64：审计链已随装配面接线（每条记录即时 append-only 落库，
 * 无挂起缓冲）；停机钩子做<b>终局完整性自检</b>——重放验证当前链，断链即 WARN
 * （审计不可用应在停机日志里可见，而非静默）。无审计链（audit 关闭）时保持占位行为。
 */
public class GuardModuleLifecycle implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(GuardModuleLifecycle.class);

    private final AtomicBoolean running = new AtomicBoolean();
    private final AuditChain auditChain;
    private final SigningKeyRing keyRing;

    public GuardModuleLifecycle() {
        this(null, null);
    }

    public GuardModuleLifecycle(AuditChain auditChain, SigningKeyRing keyRing) {
        this.auditChain = auditChain;
        this.keyRing = keyRing;
    }

    @Override
    public void start() {
        running.set(true);
    }

    @Override
    public void stop() {
        if (auditChain != null) {
            try {
                if (!auditChain.verify(keyRing)) {
                    LOG.warn("buzhou-guard 停机自检：审计链验证失败（记录数={}）——需人工重放校验",
                            auditChain.records().size());
                } else {
                    LOG.info("buzhou-guard 停机自检：审计链完整（记录数={}）",
                            auditChain.records().size());
                }
            } catch (RuntimeException e) {
                LOG.warn("buzhou-guard 停机自检异常（审计链状态未知）", e);
            }
        }
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return BuzhouLifecyclePhases.GUARD;
    }
}
