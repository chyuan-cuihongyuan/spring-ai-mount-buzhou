# 1632 · 输入侧 PII 豁免（spec 820 第三消费者）

> 来源：N 会话 R33（effort #1632 / T2415–T2416 / impl 1185）。

## Solution

`PiiInputRedactionHook` 构造器 +exemptions（null = 零行为；GuardModule 装配传
同 registry）：
- **会话级**：subject=sessionId（「该会话输入可信」——内部已合规通道，mechanism
  = `pii-input-redaction` 与输出侧 `pii-redaction` 分侧独立豁免）；
- **类型级**：subject=`type:TYPE`——生效集剔除（与输出侧同款）。

## 附：跨会话测试解卡

J 会话 DangerousToolStatsTest（spec 1069）在多会话包结构调整后脱锚：缺
import（类移 guard.hook 包）、GuardModule 全限定包路径错、yml envelope 形态
与当前 list 形态不符——三处就地修复（跨会话记档承接，R23 同模式）。

## Testing Decisions

- 回归：guard 全量 368 用例（含解卡后的 J 系 4 用例与输入/输出侧豁免全档）。

## Out of Scope

- 流式回复侧（PiiStreamRedactionHook）豁免（第四消费者——痛点出现再接）。
