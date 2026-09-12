# effort #738 — 导出体积去向审计

- 会话：G 会话 700 系第 39 轮 ｜ spec [738](../../../docs/spec/738-session-export-size-audit.md) ｜ 票 [T1076](../tickets/T1076-session-export-size.md)/[T1077](../tickets/T1077-session-export-size-verify.md) ｜ impl638
- 借鉴：存储成本归因思想

## 勘察（排重）

- SessionExport 四段导出体积无归因面——「导出为什么大」要肉眼拆 JSON；grep -i sizeAudit 零命中。

## 决定

`SessionExportSizeAudit.analyze(SessionExport)` 纯函数：按段字符归因 Segment(segment,chars,share)——chars 降序+占比守恒；summary 存在性小头标记；扩展段按名分行。

## 测试

四段归因+占比守恒/空导出零/null fail-fast。

## 诚实边界

字符估算口径（精确序列化大小可 toJson 交叉验证）；自动瘦身动作不做。
