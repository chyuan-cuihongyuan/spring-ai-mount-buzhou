# 1103 — SessionObserver 通知面异常隔离（M 系 R1）

**What to build:** DefaultAgentSession 新增 notifyObservers 隔离派发，12 处观察者裸 forEach 通知点统一收口。

**Blocked by:** T2251 / T2252（同轮 shape+verify）。

**Status:** done

- [x] notifyObservers(Consumer<SessionObserver>) 私有隔离派发（ERROR 日志带 sessionId，不中断不传播）
- [x] onOpen/onTurnStart×2/onTurnEnd×3/onTurnError×5/onCancel 共 12 处替换
- [x] onClose 既有失败收集语义不动
- [x] ObserverNotifyIsolationTest 四通道（open/start/error/cancel）+ 既有 GuardBlockObserverClosureTest 零回归

## Done

验证：core 定向测试绿（见票 T2252 验收口径）。commit 见本轮 fix 提交。
