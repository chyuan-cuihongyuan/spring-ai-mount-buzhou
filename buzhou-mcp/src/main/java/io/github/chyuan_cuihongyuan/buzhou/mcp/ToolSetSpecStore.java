package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;

import java.util.List;

/**
 * ToolSetSpec 持久层存取 seam（spec 04：DB 数据源复用 05 配置通道的 KV 载体，
 * {@code ToolSetSpec} 序列化为 JSON 存值——序列化细节归存储实现，本模块只面对对象）。
 *
 * <p>存储实现归属持久化侧（jdbc/redis 模块按需实现）；本模块提供内存默认实现
 * {@link InMemoryToolSetSpecStore} 供测试与本地运行。
 */
public interface ToolSetSpecStore {

    /** 全量清单快照。 */
    List<ToolSetSpec> loadAll();
}
