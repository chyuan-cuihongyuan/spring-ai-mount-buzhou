package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Set;

/**
 * 紧急停用热重载（spec 325 / T641，320 同法）：收到刷新事件重读
 * {@code buzhou.tool-kill-switch.tools} → 整体覆盖停用集——yml 是事实源，
 * 运行时应急改动在下一次刷新时被收编（诚实语义）。
 */
public final class ToolKillSwitchHotReload implements ApplicationListener<BuzhouConfigRefreshEvent> {

    private final ToolKillSwitchHook hook;
    private final Environment environment;

    public ToolKillSwitchHotReload(ToolKillSwitchHook hook, Environment environment) {
        if (hook == null || environment == null) {
            throw new IllegalArgumentException("hook/environment 必须非空");
        }
        this.hook = hook;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(BuzhouConfigRefreshEvent event) {
        List<String> tools = Binder.get(environment)
                .bind("buzhou.tool-kill-switch.tools",
                        Bindable.listOf(String.class))
                .orElse(List.of());
        hook.replaceDisabled(Set.copyOf(tools));
    }
}
