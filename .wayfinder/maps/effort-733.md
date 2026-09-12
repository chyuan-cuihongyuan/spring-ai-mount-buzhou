# effort #733 — 提示词使用缺口读数

- 会话：G 会话 700 系第 34 轮 ｜ spec [733](../../../docs/spec/733-prompt-usage-gaps.md) ｜ 票 [T1066](../tickets/T1066-prompt-usage-gaps.md)/[T1067](../tickets/T1067-prompt-usage-gaps-verify.md) ｜ impl633
- 借鉴：—（401 注册表 × 使用统计的联合读数）

## 勘察（排重）
- PromptRegistry.names() 与 PromptUsageStats.snapshot() 各自存在——差集读数（零使用清理候选/孤儿统计漂移信号）无；grep -i unused 零命中。

## 决定
`PromptUsageGaps.analyze(declaredNames, rows)` 纯函数——unused（声明未用字典序）/orphans（stats 有而 registry 无——漂移信号）。

## 测试
零使用差集+孤儿/null fail-fast。
