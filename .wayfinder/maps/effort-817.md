# effort #817 — SLO 多窗燃烧率联合判定

- 会话：H 会话 800 系第 18 轮 ｜ spec [817](../../../docs/spec/817-slo-multiwindow-burn.md) ｜ 票 [T1135](../tickets/T1135-slo-multiwindow-burn.md)/[T1136](../tickets/T1136-slo-multiwindow-burn-verify.md) ｜ impl570
- 借鉴：Google SRE Workbook multi-window multi-burn-rate 告警（SloTH/pyrra 实现思想）

## 勘察（排重）

- ErrorBudget（149）：单窗 burnRate+breaching——「毛刺 vs 真事故」双窗确认缺位。
- LatencySloMonitor：延迟 SLO 非错误预算窗。
- grep -i `multiwindow|multi.?window|resonance`：无命中。

## 决定

`SloMultiWindowBurn`（core.health，纯函数判定脑）：evaluate(fastBurn, slowBurn, fastSamples, slowSamples, fastThreshold, slowThreshold, minSamples)——双窗同时超阈且样本足 → incident；快窗独热=毛刺/慢窗独热=慢性渗漏/不足不判，reason 人话说明。ErrorBudget 零变更（组合优于改动——调用方各配窗长取 burnRate 喂入）；边界 ≥ 语义；阈值/minSamples fail-fast。

## 测试

双窗共振+边界相等判（14.4/6.0 Workbook 经典档）/快窗独热毛刺/慢窗独热渗漏/样本不足不判（快缺/慢缺）/双冷安静/三参 fail-fast——6 例全绿。

## 诚实边界

判定脑不持时间窗（ErrorBudget 组合——窗长归调用方配置）；不触发通知（incident 布尔+reason，通知归 AlertGate 族）；burn 允许 0（无错误合法）。
