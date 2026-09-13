# effort #743 — 导出脱敏命中计数（SessionExportSanitizer 深化）

- 会话：G 会话 700 系第 44 轮 ｜ spec [743](../../../docs/spec/743-sanitizer-hit-counts.md) ｜ 票 [T1086 沿用族](../tickets/T1086-sweep-retained.md)编号重排：票 [T1088](../tickets/T1088-sanitizer-hit-counts.md)/[T1089](../tickets/T1089-sanitizer-hit-counts-verify.md) ｜ impl644
- 借鉴：—（脱敏治理证据面）

## 勘察（排重）

- SessionExportSanitizer 脱敏但不计数——「导出时动了多少刀、什么类型最多」无证据；PiiHitStats 是输入/流式路径的命中统计（非导出路径）。

## 决定

Sanitizer 加 hitCounts（type 名与 custom:规则名 → 次数，ConcurrentHashMap 累计跨 sanitize 调用）+hitCounts()/totalHits() 读数；CustomPiiRules 加 rules() 只读访问器（规则匹配归因用）。PiiType 命中经 detector.scan 过滤 enabledTypes；自定义规则命中经 pattern matcher 计数（有替换差异才扫——省 CPU）。

## 测试

PHONE/EMAIL/TICKET_NO(custom) 各计 1+totalHits=3。
