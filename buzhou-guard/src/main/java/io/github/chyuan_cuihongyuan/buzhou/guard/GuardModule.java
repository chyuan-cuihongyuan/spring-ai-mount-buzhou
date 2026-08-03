package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.AuthTtl;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.ConfirmOption;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.Confirmation;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolConfig;
import io.github.chyuan_cuihongyuan.buzhou.guard.config.DangerousToolEntry;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.DangerousToolGuardHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.GuardAuthApi;

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

    private GuardModule(Builder builder) {
        DangerousToolConfig config = new DangerousToolConfig(
                builder.enabled, builder.authTtl, List.copyOf(builder.dangerousTools));
        this.authApi = new GuardAuthApi(builder.stores.sessionStateStore());
        List<BuzhouHook> h = new ArrayList<>();
        if (builder.enabled) {
            h.add(new DangerousToolGuardHook(config, builder.stores.sessionStateStore()));
        }
        this.hooks = List.copyOf(h);
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

    public static final class Builder {

        private final BuzhouStores stores;
        private boolean enabled = true;
        private AuthTtl authTtl = AuthTtl.ONCE;
        private final List<DangerousToolEntry> dangerousTools = new ArrayList<>();

        private Builder(BuzhouStores stores) {
            this.stores = stores;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
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
