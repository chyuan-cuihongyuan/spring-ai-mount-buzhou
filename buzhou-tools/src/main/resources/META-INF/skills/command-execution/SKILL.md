---
name: command-execution
description: run_command 深度指引：沙箱边界、命令黑名单、超时与输出治理
allowed-tools: run_command, read_range
---

# 命令执行指引

## 沙箱边界

- `workdir` 限沙箱 root（默认应用工作目录）内，越界即拒；默认 workdir = 沙箱 root。
- 命令内引用的文件路径同样应限沙箱内——沙箱约束的是执行边界，不审命令内容里的每个路径。

## 黑名单

命中黑名单的命令直接拒绝执行（`rm -rf /`、`mkfs*`、`dd` 块设备写、`shutdown`/`reboot`/`halt`、
fork 炸弹模式等）。被拒时换等价的安全命令（如定向删除沙箱内具体目录）。

## 超时

- 默认 60s；`timeoutSeconds` 可调但有上限（默认 600s）。
- 超时进程被强杀，返回「超时」文本——长任务拆步执行，不要单命令跑满上限。

## 输出治理

- stdout/stderr 合并返回；超阈值自动走 Spill 落盘，返回预览 + `read_range` 回读指针。
- 预期大输出（构建日志等）主动重定向到文件，再 `read_range` 按需取段，比整读更省上下文。
