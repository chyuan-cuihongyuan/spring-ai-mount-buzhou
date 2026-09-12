# 711 — fork 谱系游走环防护

> 来源：G 会话第 12 轮 = effort #711（借鉴静态分析 call-graph 环检测：visited set + 深度上限双闸）/ [T973](../../.wayfinder/tickets/T973-fork-lineage-guard-shape.md) / [T974](../../.wayfinder/tickets/T974-fork-lineage-guard-verify.md) / impl 514。
> 选题注记：原列主题「提示前缀缓存命中率 getter」缺口核查已被 spec 90 覆盖——ruled-out 顺延。

## 背景

fork 谱系是 state 键链（`buzhou.fork.source` 指向上游，spec 602/634）。正常 fork（运行时两条入口）只造新会话——谱系恒为树。但**导入/还原路径**（spec 6）接受外部构造的导出 JSON：环状 SOURCE 链（A→B→A）或超深链可经导入注入——消费方（面板/导出/审计）游走谱系时无防护即死循环。

## 目标

- `ForkLineageWalker`（core/session）：`static Lineage walk(SessionStateStore, String sessionId, int maxDepth)`——沿 `SessionForkKeys.SOURCE` 逐跳上溯：
  - visited 集合判环：重复访问 = `loopDetected=true` 立即终止（不 OOM）；
  - maxDepth 封顶（默认常量 64）：`depthCapped=true`；
  - 缺 SOURCE = 根（正常树零开销终止）。
- 返回 `Lineage{List<String> ancestors（最近→根）, boolean loopDetected, boolean depthCapped}`；只读不修（处置权留给导入方/面板）。

## 非目标

不做 fork 写入口的环预防（运行时入口天然无环）；不做导入端自动断环（处置策略留导入方裁决）。

## 测试

三层树有序游走；A↔B 环 loopDetected 终止；深度封顶；无源空谱系；键统一 SessionForkKeys。

## 兼容性

纯增量读面；零行为变化。
