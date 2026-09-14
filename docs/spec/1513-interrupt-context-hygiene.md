# 1513 — 中断与异常上下文卫生（design-incompleteness 五-4 部分 / 五-8 闭环）

> 来源：M 会话第 15 轮 = effort #1513（impl 1116）。

## 背景

- 五-4：`DefaultMcpClientRegistry.shutdown` 排空等待 `catch (Exception ignored)` 吞 InterruptedException 且不恢复中断位（JCIP 违规——取消信号丢失）。
- 五-8：`DiskSpillStore` 9 处 `"spill 磁盘 IO 失败"` 裸 message 无上下文（排障盲区）。

## 目标

- shutdown 收窄 catch：`TimeoutException | ExecutionException`（强杀兜底语义不变）；InterruptedException 独立分支恢复中断位并停止等待后续条目；
- DiskSpillStore 9 处 message 补操作名 + 可用标识（sessionDir/rootDir/uri/metaPath/sessionId）。

## 兼容性

异常 message 文本变化（排障质量提升）；shutdown 中断时提前停止等待（强杀兜底不受影响）。
