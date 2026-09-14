# 1539 — F8/F11 判定收尾（design-incompleteness F 系全清）

> 来源：M 会话第 43 轮 = effort #1539（impl 1142）。

## 裁定

- F8：会话索引业务标签 = 编程面 only 定案（SessionIndexObserver.wiring() 公开构造可传 map；yml 装配入口不开——标签语义业务自定无默认可兜）；
- F11：Spill 双路径幂等 = 单路径 Hook 化定案（CopyOnWrite/Onload 双 Hook 覆盖进出两向，Hook 层是唯一落盘点——无双路径即无幂等问题；manager 终检形态不采用）。

## 兼容性

纯文档回写零行为变化；F 系（F1-F11）全档闭环。
