# 609 — 评估项级超时预算

> 借鉴：[pytest-timeout](https://github.com/pytest-dev/pytest-timeout)——挂死测试不拖死套件。
> 来源：F 会话第 10 轮 = effort #600 / [T868](../../.wayfinder/tickets/T868-eval-item-timeout-shape.md) / [T869](../../.wayfinder/tickets/T869-eval-item-timeout-verify.md) / impl 462。

## 背景

评估 run（spec 52）逐项执行/并行执行（spec 68），但单项无时限——provider 停滞或工具死锁时整跑永不完成，CI 门禁挂死。

## 目标

`setPerItemTimeout(Duration)`（默认 null 不设）：超时项中断收敛 error、其余项照跑、run 必完成；指标 `buzhou.eval.item.timeouts`。

## 非目标

- 不做整跑墙钟上限（run 级归并行度×项预算自然有界）。
- 不改三态语义（error 计入分母的既有口径不变）。

## 设计

- 项跑独立虚拟线程 `future.get(timeout)`；超时 `shutdownNow()` 传播中断（可中断阻塞即刻中止，与模型超时兜底同取舍）。
- 串行/并行路径统一包装；detail 携带预算值。

## 测试

3 用例（挂死收敛/并行生效/预算校验）。

## 兼容性

默认不设 = 完全直通（零行为变化）。
