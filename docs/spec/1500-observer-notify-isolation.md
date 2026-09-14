# 1500 — SessionObserver 通知面异常隔离收口

> 来源：M 会话第 1 轮 = effort #1500（impl 1103，开张轮兼首个实质主题）。Guava EventBus SubscriberExceptionHandler 思想；impl-30 / spec 13 §core-1 的 onClose/deliverEvent 隔离先例在 observer 其余回调面的补全。

## 背景

`DefaultAgentSession` 的 `SessionObserver` 通知点中，仅 `close()` 的 onClose（失败收集 + suppressed 聚合）与 `deliverEvent`（hook 链 + 逐 listener）有异常隔离；其余 12 处（onOpen/onTurnStart×2/onTurnEnd×3/onTurnError×5/onCancel）为裸 `forEach`。单个观察者抛 RuntimeException 会：

1. 中断 `forEach`，其余观察者丢失该通知（观测面静默残缺）；
2. 向上传播破坏轮次（非流式轮失败 / 流式订阅 error）；
3. onOpen 在构造器尾部——炸掉整个会话构造，且已 track 的 leakHandle 永不 close（半初始化泄漏）。

## 目标

- 新增私有 `notifyObservers(Consumer<SessionObserver>)` 隔离派发：逐个 try/catch RuntimeException → ERROR 日志（带 sessionId 与异常栈），不中断其余观察者、不向上传播；
- 12 处裸通知点统一改走隔离派发；
- onClose 维持既有「失败收集 + suppressed 聚合上抛」语义不变（终结路径需可观测失败）。

## 兼容性

行为修复型变化：观察者异常从「炸掉通知链与主流程」改为「ERROR 日志 + 继续分发」，与既有隔离决策（impl-30）同向。健康观察者零感知；无新增公共 API。
