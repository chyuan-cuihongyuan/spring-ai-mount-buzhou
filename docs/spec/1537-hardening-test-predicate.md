# 1537 — RunCommandHardeningTest 进程匹配谓词收窄（周期预检发现）

> 来源：M 会话第 41 轮 = effort #1537（impl 1140）。

## 背景

interruptKillsProcessTree 的存活侧证谓词用裸 `"sleep 30"` 子串匹配全机进程——多会话共享机器上，并行会话轮询 shell 的命令行（`for ...; sleep 30; done`）同样命中，测试稳定误红（R41 worktree 实证 pgrep 命中无关进程）。

## 目标

谓词锚定 marker 唯一路径（`"sleep 30 && touch " + marker`——测试自构造命令行的确定性锚）+ destroyForcibly 垂死窗口轮询（≤2s）。

## 兼容性

测试自愈零产品变化。
