package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 715 / T1030–T1031：格式保形掩码——四型形态逐字符、保长、fail-closed
 * 全星降级、通用边界、null fail-fast。
 */
class FormatPreservingMaskerTest {

    @Test
    void phoneMaskKeepsHeadAndTail() {
        assertThat(FormatPreservingMasker.maskPhone("13812345678")).isEqualTo("138****5678");
        assertThat(FormatPreservingMasker.maskPhone("13812345678")).hasSize(11);
        // 非法形状（12 位）→ 等长全星 fail-closed 不抛
        assertThat(FormatPreservingMasker.maskPhone("138123456789")).isEqualTo("************");
        // 非数字
        assertThat(FormatPreservingMasker.maskPhone("abcdefghijk")).isEqualTo("***********");
    }

    @Test
    void idCardMaskKeepsCheckDigitVisible() {
        assertThat(FormatPreservingMasker.maskIdCard("110101199003077758"))
                .isEqualTo("1101************58");
        assertThat(FormatPreservingMasker.maskIdCard("11010119900307775X"))
                .isEqualTo("1101************5X"); // 尾两位（含校验码）保留
        // 17 位（缺位）→ 全星
        assertThat(FormatPreservingMasker.maskIdCard("1101011990030777"))
                .isEqualTo("****************");
    }

    @Test
    void emailMaskKeepsFirstCharAndDomain() {
        assertThat(FormatPreservingMasker.maskEmail("alice@example.com"))
                .isEqualTo("a***@example.com");
        // 非法（无 @）→ 全星
        assertThat(FormatPreservingMasker.maskEmail("not-an-email")).isEqualTo("************");
    }

    @Test
    void ipMaskKeepsFirstTwoOctets() {
        assertThat(FormatPreservingMasker.maskIp("192.168.1.100")).isEqualTo("192.168.*.*");
        // 越界八位组 → 全星
        assertThat(FormatPreservingMasker.maskIp("192.168.1.999")).isEqualTo("*************");
        // 段数不对 → 全星
        assertThat(FormatPreservingMasker.maskIp("192.168.1")).isEqualTo("*********");
    }

    @Test
    void genericMaskHandlesBoundaries() {
        assertThat(FormatPreservingMasker.mask("abcdefgh", 2, 2)).isEqualTo("ab****gh");
        assertThat(FormatPreservingMasker.mask("abc", 5, 5)).isEqualTo("abc"); // keep ≥ 长度全保
        assertThat(FormatPreservingMasker.mask("", 1, 1)).isEmpty();
        assertThatThrownBy(() -> FormatPreservingMasker.mask("abc", -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FormatPreservingMasker.maskPhone(null))
                .isInstanceOf(NullPointerException.class);
    }
}
