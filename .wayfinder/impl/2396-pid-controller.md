# impl 2396 — W 会话 W44 PID Controller 比例积分微分控制（spec 9043 / W9087–W9088 / W44）

纵切片：PID Controller 比例积分微分控制——PidController（core/policy）：三增益+抗饱和反馈控制器。

- 验证：`mvn -pl buzhou-core test -Dtest='PidControllerTest'` 全绿。
