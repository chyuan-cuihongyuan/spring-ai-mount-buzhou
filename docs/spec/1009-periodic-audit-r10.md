# 1009 — J 系周期预检（R10）

> 来源：J 会话第 10 轮 = effort #1009（[T1469](../../.wayfinder/tickets/T1469-periodic-audit-shape.md) / [T1470](../../.wayfinder/tickets/T1470-periodic-audit-verify.md) / impl 762）。每 10 轮全仓验证纪律的首轮执行。

## 范围

隔离 worktree（origin/main 快照、reactor 联编口径）全仓 `mvn -B -ntp clean verify` + 双文档门复跑 + 台账对账；发现主仓破损就地修复（并行会话在制半成品的主仓红收口，归属注记于提交信息）。

## 发现与处置

1. **guard `ToolDenialLog.topDenials` 序被 Map.copyOf 打散**（主仓红）：`LinkedHashMap` 有序填充后被 `Map.copyOf`（ImmutableCollections 哈希布局）重排——改 `Collections.unmodifiableMap` 包装。修复已验证（隔离树单测 + 全链路复跑绿）。
2. **I 会话 910–915 六 spec 缺 README 行**：补行（标题级描述，归属其提交）。
3. **`SessionExportDiff` 缺 API 快照行**：补行（core.session 段字典序位）。
4. 台账对账：specs 1000–1008 / 票 T1451–T1468 / impl 753–761 全档无缺位。

## 经验入档

- 单模块跑 starter 门的两个假红陷阱再证：①依赖走 ~/.m2 陈旧 jar（快照扫描集不完整）；②`-rf` 续跑后下游模块同样拿旧包。**门只在 reactor 联编口径下有意义**。
- 并行会话竞争下，spec 入库与 README 行/快照行分离提交是主仓红的主因——各系周期轮应默认承担对方红检收口。
