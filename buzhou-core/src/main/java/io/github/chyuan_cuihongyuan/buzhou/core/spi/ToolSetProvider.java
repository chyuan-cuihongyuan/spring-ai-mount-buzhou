package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.List;

/**
 * MCP server 清单供给 SPI（spec 04）。
 *
 * <p>内置两实现：{@code PropertiesToolSetProvider}（读 {@code buzhou.mcp.servers.*}，静态）与
 * {@code DbToolSetProvider}（读持久层，后台改配即推送），均在 {@code buzhou-mcp} 模块。
 * 配置中心适配（Nacos/Apollo）= 各自实现本接口，留作 community-extension，内核零依赖。
 */
public interface ToolSetProvider {

    /** 当前全量清单（快照语义）。 */
    List<ToolSetSpec> currentToolSets();

    /** 注册变更监听；配置源推送时回调。 */
    void addChangeListener(Runnable onChange);
}
