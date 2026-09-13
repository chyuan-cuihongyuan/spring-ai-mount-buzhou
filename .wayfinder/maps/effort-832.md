# effort #832 — 技能加载延迟读数

- 会话：H 会话 800 系第 33 轮 ｜ spec [832](../../../docs/spec/832-skill-load-latency.md) ｜ 票 [T1165](../tickets/T1165-skill-load-latency.md)/[T1166](../tickets/T1166-skill-load-latency-verify.md) ｜ impl585
- 借鉴：LangSmith 延迟分析扩散（839 使用计数的延迟姊妹面）

## 勘察（排重）

- SkillUsageStats：加载计数+top/unused——无耗时维。
- ToolTimingAggregator：工具域——技能域缺位。
- grep -i `skill.*latency|load.*ms`：无命中。

## 决定

`SkillLoadLatency`（skills，纯读数）：per-skill 环 32 样本+nearest-rank P50/P95+max 累计；技能封顶 1024 超限并入 __overflow__ 桶（SkillUsageStats 同款口径——口径一致优先）；record 脏入参忽略；slowest() 按 P95 降序。喂点=LoadSkillTool 装配侧。

## 测试

分位环账（100 次加载环存 69..100：P50=84 P95=99 max=100）/环挤老 max 累计不丢/溢出桶入账+原技能 null/slowest P95 降序/脏入参三形态——5 例绿（环容量语义首跑账误修正：loads=环容量非累计）。

## 诚实边界

loads=近窗样本数非累计（累计语义归 SkillUsageStats——分工声明）；max 为全历史峰值（环挤出不丢 max——口径显式）；喂点手动。
