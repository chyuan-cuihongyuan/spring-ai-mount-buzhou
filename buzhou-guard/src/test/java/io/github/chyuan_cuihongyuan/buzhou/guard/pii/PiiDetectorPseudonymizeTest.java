package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PII 格式保持假名化测试（spec 713 / T977–T978 / impl 516）：形状保持
 * （长度/字符类/分隔/大小写）、确定性、原文不留痕、非 PII 原样、既有面零回归。
 */
class PiiDetectorPseudonymizeTest {

    private final PiiDetector detector = new PiiDetector();

    @Test
    void phoneShapePreserved() {
        String text = "联系 13812345678 或 0755-1234567";
        String out = detector.pseudonymize(text, EnumSet.of(PiiType.CN_PHONE));

        assertThat(out).hasSameSizeAs(text);
        assertThat(out).matches("联系 \\d{11} 或 0755-1234567"); // 手机段变 11 数字；固话/分隔原样
        assertThat(out).doesNotContain("13812345678"); // 原文不留痕
    }

    @Test
    void emailShapePreservedWithCaseAndSeparators() {
        String text = "Mail: John.Doe@Example.Com!";
        String out = detector.pseudonymize(text, EnumSet.of(PiiType.EMAIL));

        assertThat(out).hasSameSizeAs(text);
        assertThat(out).startsWith("Mail: "); // 前缀原样
        assertThat(out).endsWith("!");
        String local = out.substring(6, out.indexOf('@', 6));
        String domain = out.substring(out.indexOf('@', 6) + 1, out.length() - 1);
        assertThat(local).matches("[A-Za-z]+\\.[A-Za-z]+"); // 点分隔与大小写形态保持
        assertThat(domain).matches("[A-Za-z]+\\.[A-Za-z]{2,}");
        assertThat(out).doesNotContain("John").doesNotContain("Example");
    }

    @Test
    void residentIdDigitsStayDigits() {
        String text = "ID:11010519491231002X";
        String out = detector.pseudonymize(text, EnumSet.of(PiiType.CN_RESIDENT_ID));

        assertThat(out).hasSameSizeAs(text);
        assertThat(out).startsWith("ID:");
        assertThat(out.substring(3)).matches("\\d{17}[0-9A-Za-z]"); // 17 数字 + 末位字母类保持
        // 注：校验位特例（X）不保留——特例保留会泄漏「哪位是校验位」（spec 713 诚实边界）
        assertThat(out).doesNotContain("11010519491231002X");
    }

    @Test
    void deterministicAcrossCallsAndInstances() {
        String text = "card 6222020200112233445";
        Set<PiiType> types = EnumSet.of(PiiType.BANK_CARD);

        String once = detector.pseudonymize(text, types);
        String twice = detector.pseudonymize(text, types);
        String freshInstance = new PiiDetector().pseudonymize(text, types);

        assertThat(twice).isEqualTo(once);
        assertThat(freshInstance).isEqualTo(once); // (seed,type,text) 播种——进程内外一致
    }

    @Test
    void nonPiiTextUntouched() {
        String text = "nothing sensitive here 12345678";
        assertThat(detector.pseudonymize(text, EnumSet.allOf(PiiType.class)))
                .isSameAs(text);
    }

    @Test
    void redactSemanticsUnchanged() {
        // 既有 redact 全占位符面零回归（与 pseudonymize 互不干扰）
        String text = "call 13812345678";
        assertThat(detector.redact(text, EnumSet.of(PiiType.CN_PHONE)))
                .isEqualTo("call [PII:CN_PHONE]");
    }
}
