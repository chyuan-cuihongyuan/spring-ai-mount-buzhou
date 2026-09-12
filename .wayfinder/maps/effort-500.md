# Wayfinder Map — Buzhou 流式回复 PII 脱敏（effort #500，E 会话第 1 轮）

> E 会话第 1 轮（500 系首轮）。勘察：guard 已有输入缝（PiiInputRedactionHook
> order 60，spec 106）与工具输出缝（PiiRedactionHook order 70，spec 86）——
> **模型回复出站缝**（流式 chunk 级 + 非流式整段）空白：模型自产/复读的 PII
> 直达订阅者与观测累积面。流式 chunk 化使 PII 可跨 chunk 边界断裂
> （"138"|"0013" 两 chunk），逐 chunk 独立扫描必漏。

## Destination

core 出站过滤 SPI：`core.hook.StreamTextFilter`（每轮新建有状态过滤器，
`filter(chunk)` 逐 chunk 改写 + `flush()` 收口排空）+ `BuzhouHook.
replyStreamFilter()` 默认方法（返回 null = 不参与）+ `HookChain.
newReplyFilters()`（hook 序收集）+ DefaultAgentSession 两缝接线（流式：
map 逐 chunk 改写 + concatWith 收口 flush chunk——flush 文本也进
replyAccumulator 观测面；非流式 chat：filter+flush 整段改写）。
guard 端 `PiiStreamRedactionHook`（order 75，PII 族同列）：滑动窗口缓冲
（默认 128 字符 = RFC 5321 本地段 64 + 常见域名；固定型上限 19（银行卡）
全覆盖）——可定界前缀只发安全前缀（emitLen = len−window+1），跨界实体留
窗中待完整再脱敏；flush 全量排空。yml `buzhou.guard.pii.reply-redaction`
（默认 false，与 106 输入侧开关同族）+ `reply-window`（可选）。
计数复用 `buzhou.guard.pii.redactions` + PiiHitStats Side.OUTPUT（回复即
出站面，不新开 side 枚举）。

## Notes

- 号段：spec 500 / T751–T752 / impl-403。
- 借鉴源：Microsoft Presidio 流式匿名化思想（占位符化）+ Cloudflare 流式
  WAF / Pravega 流式正则的「回看窗口」跨界匹配思想。
- 诚实边界：①窗口（128）放不下的超长跨界实体（如超长邮箱本地段）不保；
  ②会话历史内的模型自产 PII 不回溯清洗——历史随下一轮再注入上下文，但
  出站缝每轮都拦，用户面恒净；③filter 有状态单轮单用非线程安全（同轮
  串行回调契约）。
- 零默认行为：未挂 filter 钩子时零包装零开销（newReplyFilters 空表短路）。

## Out of scope

- 会话历史回溯清洗（记忆治理域）；NER 型检测（86 诚实边界同注记）；
  流式工具调用 chunk 的脱敏（工具结果已有 afterTool 缝）。

## Tickets

- [x] [T751 core 回复流出站过滤 SPI + 两缝接线](../tickets/T751-reply-stream-filter-spi.md)
- [x] [T752 PiiStreamRedactionHook 窗口缓冲](../tickets/T752-pii-stream-redaction-hook.md)
