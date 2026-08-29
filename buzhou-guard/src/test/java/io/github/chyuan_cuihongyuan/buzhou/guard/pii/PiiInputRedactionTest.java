package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 106 §B / T390：用户输入 PII 脱敏红队——beforeTurn replaceInput 占位符化；
 * 幂等（占位符不再处理）；类型子集；无命中零改写。spec 86 fog 收口（输入/输出
 * 两侧正交开关）。
 */
class PiiInputRedactionTest {

    @Test
    void beforeTurnReplacesPiiInInput() {
        PiiInputRedactionHook hook = new PiiInputRedactionHook();
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        DefaultTurnContext ctx = new DefaultTurnContext(env,
                "帮我查下 13812345678 这个号，邮箱 a@b.co");

        hook.beforeTurn(ctx);

        assertThat(ctx.input())
                .isEqualTo("帮我查下 [PII:CN_PHONE] 这个号，邮箱 [PII:EMAIL]");
    }

    @Test
    void idempotentAndTypeSubsetAndNoHitZeroChange() {
        PiiInputRedactionHook hook = new PiiInputRedactionHook();
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());

        // 幂等：已脱敏输入不再处理
        DefaultTurnContext once = new DefaultTurnContext(env, "号 [PII:CN_PHONE] 查询");
        hook.beforeTurn(once);
        assertThat(once.input()).isEqualTo("号 [PII:CN_PHONE] 查询");

        // 类型子集：只脱邮箱
        PiiInputRedactionHook emailOnly = new PiiInputRedactionHook(EnumSet.of(PiiType.EMAIL));
        DefaultTurnContext mixed = new DefaultTurnContext(env, "13812345678 和 a@b.co");
        emailOnly.beforeTurn(mixed);
        assertThat(mixed.input()).isEqualTo("13812345678 和 [PII:EMAIL]");

        // 无命中零改写（引用等——不改写即零成本）
        DefaultTurnContext clean = new DefaultTurnContext(env, "正常提问");
        String before = clean.input();
        hook.beforeTurn(clean);
        assertThat(clean.input()).isSameAs(before);
    }
}
