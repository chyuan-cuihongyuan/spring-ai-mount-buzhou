package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.AttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.DefaultFactStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.ConfirmOption;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.Confirmation;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.fact.FactAttachmentRenderer;
import io.github.chyuan_cuihongyuan.buzhou.guard.fact.FactCollectorHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.fact.FactDefinition;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.DangerousToolGuardHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;
import io.github.chyuan_cuihongyuan.buzhou.guard.inject.CanaryGuardHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.inject.SpotlightHook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HITL 危险守卫模块入口（spec 07）。经 {@link #configure} 返回 {@link RuntimeConfig}，由
 * {@code RuntimeConfig.merge} 与其他机制模块组合，挂进 {@code HarnessAssembler} 的装配链。
 *
 * <p>用法：
 * <pre>{@code
 * RuntimeConfig config = RuntimeConfig.merge(
 *     GuardModule.builder(stores).dangerousTool("run_command", "confirm_run_command", "即将执行命令", confirmOptions).build().configure(),
 *     memory.configure(...));
 * }</pre>
 */
public final class GuardModule {

    private final List<BuzhouHook> hooks;
    private final GuardAuthApi authApi;
    private final AttachmentRenderer attachmentRenderer;
    private final FactStore factStore;

    private GuardModule(Builder builder) {
        DangerousToolConfig config = new DangerousToolConfig(
                builder.enabled, builder.authTtl, List.copyOf(builder.dangerousTools));
        this.authApi = new GuardAuthApi(builder.stores.sessionStateStore(), builder.authTtl,
                builder.stores.observabilityStore());
        this.factStore = new DefaultFactStore(builder.stores.sessionStateStore());
        List<BuzhouHook> h = new ArrayList<>();
        if (builder.enabled) {
            h.add(new DangerousToolGuardHook(config, builder.stores.sessionStateStore()));
        }
        if (builder.canaryGuard) {
            h.add(builder.canaryToken == null
                    ? new CanaryGuardHook()
                    : new CanaryGuardHook(builder.canaryToken, builder.canarySimilarityThreshold));
        }
        if (builder.spotlighting) {
            h.add(new SpotlightHook());
        }
        // impl-21 / T49：FIDES 最小 taint（读侧打标 + 写门校验；默认关，按机制开关）
        if (builder.taintTracking) {
            h.add(new io.github.chyuan_cuihongyuan.buzhou.guard.taint.TaintTrackingHook(
                    builder.stores.sessionStateStore()));
            h.add(new io.github.chyuan_cuihongyuan.buzhou.guard.taint.TaintWriteGateHook(
                    config, builder.stores.sessionStateStore()));
        }
        if (!builder.factDefinitions.isEmpty()) {
            h.add(new FactCollectorHook(builder.factDefinitions, factStore));
        }
        this.hooks = List.copyOf(h);
        this.attachmentRenderer = builder.factDefinitions.isEmpty() ? null
                : new FactAttachmentRenderer(factStore, builder.factDefinitions);
    }

    public static Builder builder(BuzhouStores stores) {
        return new Builder(stores);
    }

    /** 从 yml map（前缀 buzhou.guard）解析配置。 */
    public static GuardModule fromYml(BuzhouStores stores, Map<String, Object> ymlConfig) {
        return builder(stores).fromYml(ymlConfig).build();
    }

    public RuntimeConfig configure() {
        return new RuntimeConfig(hooks, Set.of(), Set.of(), null, List.of());
    }

    /** 授权写回 API（业务侧 REST 调用）。 */
    public GuardAuthApi authApi() {
        return authApi;
    }

    /** 事实 Attachment 渲染器（供 memory 注入视图构建方注入事实块）；无采集器时返回 null。 */
    public AttachmentRenderer attachmentRenderer() {
        return attachmentRenderer;
    }

    /** 事实存取门面（调试/查询用）。 */
    public FactStore factStore() {
        return factStore;
    }

    public static final class Builder {

        private final BuzhouStores stores;
        private boolean enabled = true;
        private AuthTtl authTtl = AuthTtl.ONCE;
        private final List<DangerousToolEntry> dangerousTools = new ArrayList<>();
        private final List<FactDefinition> factDefinitions = new ArrayList<>();
        // T18 读侧注入防御（默认关闭、按机制开关；见 docs/spec/11 guard）
        private boolean spotlighting = false;
        private boolean canaryGuard = false;
        private String canaryToken = null;
        private double canarySimilarityThreshold = 0.6;
        // impl-21 / T49：FIDES 最小 taint 信息流控制（默认关）
        private boolean taintTracking = false;

        private Builder(BuzhouStores stores) {
            this.stores = stores;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** 开启读侧 Spotlighting（随机分隔符 + 交织标记包裹外部输出）。 */
        public Builder spotlighting() {
            this.spotlighting = true;
            return this;
        }

        /** 开启 canary 泄漏检测 + 自硬化拒识。 */
        public Builder canaryGuard() {
            this.canaryGuard = true;
            return this;
        }

        /** 一键开启读侧注入防御（spotlighting + canary）。 */
        public Builder injectionDefense() {
            return spotlighting().canaryGuard();
        }

        /** 开启 FIDES 最小 taint 信息流控制（读侧打标 + 写门：untrusted 上下文写侧调用转 HITL）。 */
        public Builder taintTracking() {
            this.taintTracking = true;
            return this;
        }

        /** 固定密语（默认随机；测试/诊断用）。 */
        public Builder canaryToken(String token) {
            this.canaryToken = token;
            return this;
        }

        /** 变体拒识相似度阈值（字符 n-gram Jaccard，默认 0.6）。 */
        public Builder canarySimilarityThreshold(double threshold) {
            this.canarySimilarityThreshold = threshold;
            return this;
        }

        public Builder authTtl(AuthTtl authTtl) {
            this.authTtl = authTtl;
            return this;
        }

        public Builder dangerousTool(DangerousToolEntry entry) {
            this.dangerousTools.add(entry);
            return this;
        }

        /** 注册事实采集器（FactCollector 三要素脚手架）。 */
        public Builder factDefinition(FactDefinition definition) {
            if (definition != null) {
                this.factDefinitions.add(definition);
            }
            return this;
        }

        /** 便捷添加：名称 + requiredState + hint + 默认 approve/reject 双选项。 */
        public Builder dangerousTool(String name, String requiredState, String hint) {
            return dangerousTool(name, requiredState, hint, defaultConfirmation(name));
        }

        /** 便捷添加：名称 + requiredState + hint + 自定义选项。 */
        public Builder dangerousTool(String name, String requiredState, String hint,
                                     List<ConfirmOption> options) {
            this.dangerousTools.add(new DangerousToolEntry(name, requiredState, hint,
                    new Confirmation("请确认：" + name, options)));
            return this;
        }

        /** 默认 approve/reject 双选项。 */
        public static List<ConfirmOption> defaultConfirmation(String toolName) {
            return List.of(
                    new ConfirmOption("approve", "允许执行", "approve"),
                    new ConfirmOption("reject", "拒绝", "reject"));
        }

        @SuppressWarnings("unchecked")
        public Builder fromYml(Map<String, Object> ymlConfig) {
            if (ymlConfig == null || ymlConfig.isEmpty()) {
                return this;
            }
            Object enabledVal = ymlConfig.get("enabled");
            if (enabledVal instanceof Boolean b) {
                this.enabled = b;
            }
            Object ttlVal = ymlConfig.get("auth-ttl");
            if (ttlVal instanceof String s) {
                this.authTtl = AuthTtl.parse(s);
            }
            Object spotVal = ymlConfig.get("spotlighting");
            if (spotVal instanceof Boolean b) {
                this.spotlighting = b;
            }
            Object canaryVal = ymlConfig.get("canary-guard");
            if (canaryVal instanceof Boolean b2) {
                this.canaryGuard = b2;
            }
            Object tokenVal = ymlConfig.get("canary-token");
            if (tokenVal instanceof String s2 && !s2.isBlank()) {
                this.canaryToken = s2;
            }
            Object toolsVal = ymlConfig.get("dangerous-tools");
            if (toolsVal instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> toolMap) {
                        parseToolEntry((Map<String, Object>) toolMap);
                    }
                }
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        private void parseToolEntry(Map<String, Object> toolMap) {
            String name = stringOf(toolMap.get("name"));
            if (name == null || name.isBlank()) {
                return;
            }
            String requiredState = stringOf(toolMap.get("required-state"));
            String hint = stringOf(toolMap.get("hint"));
            List<ConfirmOption> options = new ArrayList<>();
            Object confirmVal = toolMap.get("confirmation");
            if (confirmVal instanceof Map<?, ?> confirmMap) {
                Object optionsVal = confirmMap.get("options");
                if (optionsVal instanceof List<?> optList) {
                    for (Object opt : optList) {
                        if (opt instanceof Map<?, ?> optMap) {
                            options.add(parseOption((Map<String, Object>) optMap));
                        }
                    }
                }
            }
            if (options.isEmpty()) {
                options = defaultConfirmation(name);
            }
            String title = confirmVal instanceof Map<?, ?> cm ? stringOf(cm.get("title")) : null;
            this.dangerousTools.add(new DangerousToolEntry(name,
                    requiredState == null ? "" : requiredState,
                    hint == null ? "" : hint,
                    new Confirmation(title == null ? "请确认：" + name : title, options)));
        }

        @SuppressWarnings("unchecked")
        private ConfirmOption parseOption(Map<String, Object> optMap) {
            String id = stringOf(optMap.get("id"));
            String label = stringOf(optMap.get("label"));
            String value = stringOf(optMap.get("value"));
            if (value == null) {
                value = id;
            }
            boolean hasInput = optMap.get("has-input") instanceof Boolean b && b
                    || optMap.get("hasInput") instanceof Boolean b2 && b2;
            String placeholder = stringOf(optMap.get("input-placeholder"));
            String inputType = stringOf(optMap.get("input-type"));
            return new ConfirmOption(id == null ? value : id, label, value, hasInput,
                    placeholder, inputType == null ? "text" : inputType);
        }

        private static String stringOf(Object o) {
            return o == null ? null : String.valueOf(o);
        }

        public GuardModule build() {
            return new GuardModule(this);
        }
    }
}
