package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 337 / impl-360：行李面回归——put/view 快照不可变/空键拒绝/值长
 * 封顶/键数封顶/被拒计数不静默。
 */
class ToolBaggageTest {

    @Test
    void putAndRoundtrip_seedMap() {
        ToolBaggage baggage = new ToolBaggage(Map.of("tenant", "acme", "env", "prod"));
        assertThat(baggage.view()).containsEntry("tenant", "acme").containsEntry("env", "prod");
        assertThat(baggage.isEmpty()).isFalse();
        assertThat(baggage.size()).isEqualTo(2);
        baggage.put("tenant", "beta"); // 改值
        assertThat(baggage.view()).containsEntry("tenant", "beta");
        baggage.remove("env");
        assertThat(baggage.view()).doesNotContainKey("env");
        baggage.clear();
        assertThat(baggage.isEmpty()).isTrue();
    }

    @Test
    void viewIsImmutableSnapshot() {
        ToolBaggage baggage = new ToolBaggage();
        baggage.put("k", "v");
        Map<String, String> snapshot = baggage.view();
        assertThatThrownBy(() -> snapshot.put("k2", "v2"))
                .isInstanceOf(UnsupportedOperationException.class);
        baggage.put("k2", "v2"); // 活面继续长——快照不变
        assertThat(snapshot).doesNotContainKey("k2");
        assertThat(baggage.view()).containsKey("k2");
    }

    @Test
    void blankKeyRejected_nullValueFoldsEmpty() {
        ToolBaggage baggage = new ToolBaggage();
        assertThatThrownBy(() -> baggage.put(" ", "v"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> baggage.put(null, "v"))
                .isInstanceOf(IllegalArgumentException.class);
        baggage.put("k", null); // null 值折空串（诚实边界）
        assertThat(baggage.view()).containsEntry("k", "");
        assertThat(baggage.rejectedPuts()).isEqualTo(2);
    }

    @Test
    void valueLengthCapped() {
        ToolBaggage baggage = new ToolBaggage();
        String tooLong = "x".repeat(ToolBaggage.MAX_VALUE_CHARS + 1);
        assertThatThrownBy(() -> baggage.put("k", tooLong))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("超长");
        baggage.put("k", "x".repeat(ToolBaggage.MAX_VALUE_CHARS)); // 恰好上限过
        assertThat(baggage.view()).containsKey("k");
    }

    @Test
    void entryCountCapped_overwriteStillAllowed() {
        ToolBaggage baggage = new ToolBaggage();
        for (int i = 0; i < ToolBaggage.MAX_ENTRIES; i++) {
            baggage.put("k-" + i, "v");
        }
        assertThatThrownBy(() -> baggage.put("extra", "v"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("上限");
        baggage.put("k-0", "updated"); // 已有键改值不受限
        assertThat(baggage.view()).containsEntry("k-0", "updated");
        baggage.remove("k-1");
        baggage.put("extra", "v"); // 腾位后可放
        assertThat(baggage.view()).containsKey("extra");
    }

    @Test
    void emptyByDefault_zeroOverheadSemantics() {
        assertThat(new ToolBaggage().isEmpty()).isTrue();
        assertThat(new ToolBaggage().view()).isEmpty();
    }
}
