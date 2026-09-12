# 640 — fork 谱系键公共常量收口

> 来源：F 会话第 41 轮 = effort #600（spec 602/634 的键契约收口）/ [T930](../../.wayfinder/tickets/T930-fork-lineage-keys-shape.md) / [T931](../../.wayfinder/tickets/T931-fork-lineage-keys-verify.md) / impl 493。借鉴：gRPC `Metadata.Key` / k8s apimachinery 常量类收口惯例。

## 背景

fork 谱系三个 state 键字符串（`buzhou.fork.source` / `buzhou.fork.turn` / producer `buzhou.core.fork`）此前在写入口（AgentRuntime fork/forkFromTurn）与读入口（会话面板 forkedActive 段、导出携带、测试）各持一份字面量复制。字符串复制会漂移——写 A 读 B 时谱系静默断裂（forkedActive 永远 0、导出谱系丢失），且编译期不可见。

## 目标

`core.session` 公共常量类 `SessionForkKeys`：

- `SOURCE = "buzhou.fork.source"`（谱系源会话 id——fork 与 forkFromTurn 都写）
- `TURN = "buzhou.fork.turn"`（回放起点轮次——仅 forkFromTurn 写）
- `PRODUCER = "buzhou.core.fork"`（state producer 标识）

写读两侧一律引用常量，main 代码零字面量残留。放公共 API 包：键名是 state 面对外契约（导出/导入跨环境携带，外部读面需同键名）。

## 非目标

不改键值（wire 契约钉死——既有存量数据与新数据同键无缝）；不新增第四键。

## 测试

常量一致性 3 用例（钉常量与字面量同值）+ 既有 SessionForkLineageTest 字面量断言保留（真实 fork 双向钉住——常量类改值即红）+ 全模块零回归。

## 兼容性

纯收口零行为变化；public 常量类为纯增量。
