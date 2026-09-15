package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1833 / T2868：续读令牌——编解码回路、三态裁决、畸形 fail-fast。 */
class ResumeTokenCodecTest {

    /** 编解码回路：同 token 同指纹同偏移。 */
    @Test
    void encodeDecodeRoundTrip() {
        ResumeTokenCodec.ResumeToken token =
                new ResumeTokenCodec.ResumeToken("sha256-abc123", 42L);
        assertThat(ResumeTokenCodec.decode(ResumeTokenCodec.encode(token))).isEqualTo(token);
        // 指纹内含分隔符也按最后分隔符切（指纹侧容忍）
        ResumeTokenCodec.ResumeToken embedded =
                new ResumeTokenCodec.ResumeToken("a@b", 7L);
        assertThat(ResumeTokenCodec.decode(ResumeTokenCodec.encode(embedded))).isEqualTo(embedded);
    }

    /** 三态裁决：指纹先行（换代优先于越界），越界含边界（== max 为读到尾）。 */
    @Test
    void shouldJudgeThreeVerdicts() {
        ResumeTokenCodec.ResumeToken token =
                new ResumeTokenCodec.ResumeToken("fp-1", 100L);
        assertThat(ResumeTokenCodec.check(token, "fp-1", 150L))
                .isEqualTo(ResumeTokenCodec.Verdict.VALID);
        assertThat(ResumeTokenCodec.check(token, "fp-1", 100L))
                .isEqualTo(ResumeTokenCodec.Verdict.VALID);
        assertThat(ResumeTokenCodec.check(token, "fp-1", 99L))
                .isEqualTo(ResumeTokenCodec.Verdict.OUT_OF_RANGE);
        // 换代优先于越界：指纹不符即 STALE（哪怕也越界）
        assertThat(ResumeTokenCodec.check(token, "fp-2", 99L))
                .isEqualTo(ResumeTokenCodec.Verdict.STALE_DATA);
    }

    /** 畸形令牌解码 fail-fast：null、无分隔符、偏移非数字、空偏移。 */
    @Test
    void malformedTokenFailsFast() {
        assertThatThrownBy(() -> ResumeTokenCodec.decode(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("令牌不能为 null");
        assertThatThrownBy(() -> ResumeTokenCodec.decode("noseparator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺指纹或偏移");
        assertThatThrownBy(() -> ResumeTokenCodec.decode("fp@notanumber"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("偏移非数字");
        assertThatThrownBy(() -> ResumeTokenCodec.decode("fp@"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 畸形入参 fail-fast：负偏移令牌、空白指纹、负水位。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new ResumeTokenCodec.ResumeToken("fp", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("偏移 ≥ 0");
        assertThatThrownBy(() -> new ResumeTokenCodec.ResumeToken(" ", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResumeTokenCodec.check(
                new ResumeTokenCodec.ResumeToken("fp", 0), "", 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResumeTokenCodec.check(
                new ResumeTokenCodec.ResumeToken("fp", 0), "cur", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
