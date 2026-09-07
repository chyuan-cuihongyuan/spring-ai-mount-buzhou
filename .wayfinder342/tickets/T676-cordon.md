---
Type: task
Status: closed
---
## Question

MaintenanceCordon（yml 窗+运行时按钮+虚拟线程轮询+过期窗 no-op）+
装配（bean 恒在）+ 收口。

## Resolution

done（2026-09-04）：impl-365；cordon 七用例（入窗抬/出窗落/计数一次/
按钮即时/过期 no-op/与冻结并存 maintenance 落回仍 HIGH/view）+装配三
用例（恒在/yml 绑定/from≥until 红）绿；快照 regenerate +2 型
（MaintenanceCordon+属性）；README 纵深 IV 加行、覆盖门绿。
