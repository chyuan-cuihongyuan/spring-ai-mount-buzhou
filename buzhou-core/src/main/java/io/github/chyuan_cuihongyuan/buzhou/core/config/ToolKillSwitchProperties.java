package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 工具紧急停用装配属性（spec 325 / T641，前缀
 * {@code buzhou.tool-kill-switch}）。装配恒在（空集直通零变化——事故按钮
 * 预先在场）；yml 列表 = 启动预停用，且是每次刷新事件的事实源。
 *
 * @param tools 启动即停用的工具清单（空/未配 = 无停用）
 */
@ConfigurationProperties(prefix = "buzhou.tool-kill-switch")
public record ToolKillSwitchProperties(List<String> tools) {
}
