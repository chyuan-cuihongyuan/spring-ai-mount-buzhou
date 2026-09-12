# 644 — JSON 行手工拼接收口（Jackson 统一）

> 来源：F 会话第 45 轮 = effort #600（spec 60 序列化纪律全仓收口）/ [T938](../../.wayfinder/tickets/T938-jsonl-escaping-shape.md) / [T939](../../.wayfinder/tickets/T939-jsonl-escaping-verify.md) / impl 497。

## 背景

spec 60 为观测导出立了「绝不手工拼接、Jackson 序列化保证一行一记录（字符串内换行天然转义）」纪律，但四处仍在手工拼 JSON：

- **PromptUsageJsonl**：`row.name()` 零转义——prompt 名含 `"` / `\` / 换行即产出畸形 JSONL 行（OLAP `read_json_auto` 装载失败）；
- **ExportBundle.manifestJson**：error 字段 `.replace("\"", "'")` 土法转义——丢信息（引号变撇号）且不处理换行（IO 异常消息常见多行 → manifest 畸形）；
- **FailureTurnSnapshots / WebhookDeadLetterJsonl**：自有 escape 只覆盖 `\` `"` `\n` `\r` 四字符——裸控制字符（`\t`、`\b`、`\f` 等 <0x20）在严格 JSON 解析器下非法。

## 目标

四处统一走 Jackson `writeValueAsString`（`LinkedHashMap` 插入序 = 字段序稳定，与 spec 60 行形态同构；静态共享 `ObjectMapper` 常量——ObservabilityJsonlExporter 同模式）。删除两份重复私有 `escape`。

## 非目标

不造公共序列化工具类（四处形态各异、各自一行 MAPPER 调用足够）；不改行字段序与正常值字节形态。

## 测试

四处对抗用例：字段注入 `"`、`\`、换行、`\t` → 行合法 JSON 且 Jackson 回读得原值；正常值既有用例零回归。

## 兼容性

正常值行字节形态不变（Jackson 对无特殊字符值的序列化与手工拼接一致）；仅特殊字符场景从「畸形」变「合法」。
