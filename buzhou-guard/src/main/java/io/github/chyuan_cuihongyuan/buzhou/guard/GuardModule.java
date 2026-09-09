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
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyEngine;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyGateHook;

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
        // spec 86 §A / T329：PII 脱敏先于 spotlight（order 70 < 80——先脱敏原文再包裹）
        if (builder.piiRedaction) {
            h.add(builder.piiTypes == null
                    ? (builder.customPiiRules == null
                            ? new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiRedactionHook()
                            : new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiRedactionHook(
                                    null, builder.customPiiRules))
                    : (builder.customPiiRules == null
                            ? new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiRedactionHook(
                                    builder.piiTypes)
                            : new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiRedactionHook(
                                    builder.piiTypes, builder.customPiiRules)));
        }
        // spec 106 §A / T389：用户输入脱敏（beforeTurn replaceInput——与输出侧正交）
        if (builder.piiInputRedaction) {
            h.add(builder.piiTypes == null
                    ? (builder.customPiiRules == null
                            ? new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiInputRedactionHook()
                            : new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiInputRedactionHook(
                                    null, builder.customPiiRules))
                    : (builder.customPiiRules == null
                            ? new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiInputRedactionHook(
                                    builder.piiTypes)
                            : new io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiInputRedactionHook(
                                    builder.piiTypes, builder.customPiiRules)));
        }
        // spec 400 / T692：密钥扫描（三缝 MASK——输入/出站参数/工具结果；默认关）
        if (builder.secretScanning) {
            h.add(builder.secretTypes == null
                    ? new io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretScanHook()
                    : new io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretScanHook(
                            builder.secretTypes));
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
        // impl-40 / spec 13 §T64：策略门（热加载引擎由装配/业务侧注入；默认拒语义见 PolicyGateHook）
        if (builder.policyEngine != null) {
            h.add(new PolicyGateHook(builder.policyEngine));
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
        // spec 86 §A / T329：工具输出 PII 脱敏（默认关；null 类型集 = 全类型）
        private boolean piiRedaction = false;
        // spec 106 §A / T389：用户输入 PII 脱敏（默认关；与输出侧正交，types 复用）
        private boolean piiInputRedaction = false;
        private java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiType> piiTypes = null;
        // spec 129 / T475：自定义 PII 规则（yml/程序面；null = 无叠加）
        private io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules customPiiRules;
        // spec 400 / T692：密钥扫描（默认关；null 类型集 = 全 7 型）
        private boolean secretScanning = false;
        private java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretType> secretTypes = null;
        // impl-40 / spec 13 §T64：授权策略门引擎（null = 不挂策略门）
        private PolicyEngine policyEngine;

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

        /** 开启工具输出 PII 脱敏（全类型；spec 86 / T329，Presidio 借鉴）。 */
        public Builder piiRedaction() {
            return piiRedaction(null);
        }

        /** 开启 PII 脱敏并指定类型子集（null/空 = 全类型）。 */
        public Builder piiRedaction(
                java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiType> types) {
            this.piiRedaction = true;
            this.piiTypes = types;
            return this;
        }

        /** 开启用户输入 PII 脱敏（类型集沿用当前 piiTypes；未设 = 全类型；spec 106 / T389）。 */
        public Builder piiInputRedaction() {
            this.piiInputRedaction = true;
            return this;
        }

        /** spec 129 / T475：自定义 PII 规则（输出/输入两侧共用叠加；null = 清除）。 */
        public Builder customPiiRules(
                io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules rules) {
            this.customPiiRules = rules;
            return this;
        }

        /** 开启密钥扫描（全 7 型；spec 400 / T692，gitleaks 借鉴——三缝 MASK）。 */
        public Builder secretScanning() {
            return secretScanning(null);
        }

        /** 开启密钥扫描并指定类型子集（null/空 = 全类型）。 */
        public Builder secretScanning(
                java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretType> types) {
            this.secretScanning = true;
            this.secretTypes = types;
            return this;
        }

        /** 挂授权策略门（热加载 {@link io.github.chyuan_cuihongyuan.buzhou.guard.policy.PolicyRefresher}
         * 或静态 {@link io.github.chyuan_cuihongyuan.buzhou.guard.policy.EmbeddedPolicyEngine}）。 */
        public Builder policyEngine(PolicyEngine engine) {
            this.policyEngine = engine;
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
            // spec 86 §A / T329：pii.enabled（默认 false）+ pii.types（List/CSV，可选）
            Object piiVal = ymlConfig.get("pii");
            if (piiVal instanceof Map<?, ?> piiMap) {
                Object piiEnabled = piiMap.get("enabled");
                if (piiEnabled instanceof Boolean b3) {
                    this.piiRedaction = b3;
                }
                // spec 106 §A / T389：输入侧独立开关（types 与输出侧共用）
                Object inputVal = piiMap.get("input-redaction");
                if (inputVal instanceof Boolean b4) {
                    this.piiInputRedaction = b4;
                }
                Object typesVal = piiMap.get("types");
                java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiType> types =
                        new java.util.LinkedHashSet<>();
                if (typesVal instanceof List<?> typeList) {
                    for (Object t : typeList) {
                        parsePiiType(String.valueOf(t), types);
                    }
                } else if (typesVal instanceof String csv) {
                    for (String t : csv.split(",")) {
                        parsePiiType(t.trim(), types);
                    }
                }
                if (!types.isEmpty()) {
                    this.piiTypes = types;
                }
                // spec 129 / T475：custom-rules 声明式规则（List<{name,pattern}> 或 name→pattern map）
                Object rulesVal = piiMap.get("custom-rules");
                if (rulesVal != null) {
                    this.customPiiRules = parseCustomPiiRules(rulesVal);
                }
            }
            // spec 400 / T692：secrets.enabled（默认 false）+ secrets.types（List/CSV，可选）
            Object secretsVal = ymlConfig.get("secrets");
            if (secretsVal instanceof Map<?, ?> secretsMap) {
                Object secretsEnabled = secretsMap.get("enabled");
                if (secretsEnabled instanceof Boolean b5) {
                    this.secretScanning = b5;
                }
                Object secretTypesVal = secretsMap.get("types");
                java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretType> stypes =
                        new java.util.LinkedHashSet<>();
                if (secretTypesVal instanceof List<?> typeList) {
                    for (Object t : typeList) {
                        parseSecretType(String.valueOf(t), stypes);
                    }
                } else if (secretTypesVal instanceof String csv) {
                    for (String t : csv.split(",")) {
                        parseSecretType(t.trim(), stypes);
                    }
                }
                if (!stypes.isEmpty()) {
                    this.secretTypes = stypes;
                }
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

        private static void parsePiiType(String raw,
                java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiType> into) {
            try {
                into.add(io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiType.valueOf(
                        raw.trim().toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // 未知类型忽略（有界枚举纪律——fail-soft，装配日志面另议）
            }
        }

        /** spec 400 / T692：密钥类型解析（fail-soft 与 PII types 同口径）。 */
        private static void parseSecretType(String raw,
                java.util.Set<io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretType> into) {
            try {
                into.add(io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretType.valueOf(
                        raw.trim().toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // 未知类型忽略（有界枚举纪律——fail-soft，与 parsePiiType 同口径）
            }
        }

        /**
         * spec 129 / T475：解析 custom-rules（List of {name, pattern} 或 name→pattern
         * Map）——非法名/正则装配期 fail-fast（Rule 构造器与 Pattern.compile 原样上抛）。
         */
        private static io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules
        parseCustomPiiRules(Object rulesVal) {
            List<io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules.Rule> rules =
                    new ArrayList<>();
            if (rulesVal instanceof List<?> ruleList) {
                for (Object item : ruleList) {
                    if (item instanceof Map<?, ?> ruleMap) {
                        String name = stringOf(ruleMap.get("name"));
                        String pattern = stringOf(ruleMap.get("pattern"));
                        if (name != null && pattern != null) {
                            rules.add(io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules.Rule
                                    .of(name, pattern));
                        }
                    }
                }
            } else if (rulesVal instanceof Map<?, ?> nameToPattern) {
                for (Map.Entry<?, ?> entry : nameToPattern.entrySet()) {
                    String name = stringOf(entry.getKey());
                    String pattern = stringOf(entry.getValue());
                    if (name != null && pattern != null) {
                        rules.add(io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules.Rule
                                .of(name, pattern));
                    }
                }
            }
            return new io.github.chyuan_cuihongyuan.buzhou.guard.pii.CustomPiiRules(rules);
        }

        @SuppressWarnings("unchecked")
        private void parseToolEntry(Map<String, Object> toolMap) {            String name = stringOf(toolMap.get("name"));
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
