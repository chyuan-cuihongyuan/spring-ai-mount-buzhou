# impl 2155 — S 会话 S5 Fencing Token 世代令牌护栏（spec 5004 / T6109–T6110 / S5）

纵切片：FencingTokenGuard（core/transaction）——单调发令 +
三态写守卫 + release 不重置 + 读数面。

- 验证：`mvn -pl buzhou-core test -Dtest='FencingTokenGuardTest'` 全绿。
