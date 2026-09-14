package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.StreamTextFilter;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardExemptionRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流式 PII 类型级豁免测试（spec 1643 / T2437–T2438 / impl 1196）：
 * type:TYPE 豁免后流式窗口放行该类型原文、其余照脱（StreamTextFilter SPI
 * 无会话上下文——会话级豁免不适用流式面的诚实边界钉住）。
 */
class PiiStreamExemptionTest {

    private static final String TEXT = "联系 test@example.com 或 13800138000";

    @Test
    void typeExemptionReleasesOnlyThatTypeInStream() {
        GuardExemptionRegistry exemptions = new GuardExemptionRegistry();
        exemptions.grant("pii-redaction", "type:CN_PHONE",
                System.currentTimeMillis() + 60_000, "手机号误报");
        PiiStreamRedactionHook hook = new PiiStreamRedactionHook(null, null, 8, exemptions);
        StreamTextFilter filter = hook.replyStreamFilter();
        StringBuilder out = new StringBuilder();
        out.append(filter.filter("联系 test@example.com 或 138"));
        out.append(filter.filter("00138000 完毕"));
        out.append(filter.flush());
        String result = out.toString();
        assertThat(result).contains("[PII:EMAIL");        // EMAIL 照脱
        assertThat(result).contains("13800138000");       // CN_PHONE 豁免原文
    }

    @Test
    void noExemptionRedactsBothTypes() {
        PiiStreamRedactionHook hook = new PiiStreamRedactionHook(null, null, 8,
                new GuardExemptionRegistry());
        StreamTextFilter filter = hook.replyStreamFilter();
        StringBuilder out = new StringBuilder();
        out.append(filter.filter("联系 test@example.com 或 138"));
        out.append(filter.filter("00138000 完毕"));
        out.append(filter.flush());
        assertThat(out.toString()).contains("[PII:EMAIL").contains("[PII:CN_PHONE");
    }
}
