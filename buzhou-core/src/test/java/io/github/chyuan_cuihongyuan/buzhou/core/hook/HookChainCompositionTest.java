package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HookChainCompositionTest {

    static class NamedHook implements BuzhouHook {
        private final int order;
        private final String name;

        NamedHook(int order, String name) {
            this.order = order;
            this.name = name;
        }

        @Override
        public int order() {
            return order;
        }

        @Override
        public String name() {
            return name;
        }
    }

    @Test
    void resolvedOrderIsOrderByAscendingThenNameStable() {
        HookChain chain = HookChain.of(List.of(
                new NamedHook(2, "zeta"), new NamedHook(1, "alpha"), new NamedHook(2, "beta")));

        assertThat(chain.composition().resolvedHookNames())
                .containsExactly("alpha", "beta", "zeta");
    }

    @Test
    void disabledHooksAreExcludedFromResolvedOrder() {
        HookChain chain = new HookChain(
                List.of(new NamedHook(1, "alpha"), new NamedHook(2, "beta")),
                Set.of("beta"));

        ChainComposition composition = chain.composition();
        assertThat(composition.resolvedHookNames()).containsExactly("alpha");
        assertThat(composition.ghostDisabledNames()).isEmpty();
    }

    @Test
    void typoDisabledNameSurfacesAsGhost() {
        HookChain chain = new HookChain(
                List.of(new NamedHook(1, "alpha")),
                Set.of("nope"));

        ChainComposition composition = chain.composition();
        assertThat(composition.resolvedHookNames()).containsExactly("alpha");
        assertThat(composition.ghostDisabledNames()).containsExactly("nope");
    }

    @Test
    void realDisableDoesNotJoinGhostSet() {
        HookChain chain = new HookChain(
                List.of(new NamedHook(1, "alpha"), new NamedHook(2, "beta")),
                Set.of("beta", "typo_name"));

        ChainComposition composition = chain.composition();
        assertThat(composition.resolvedHookNames()).containsExactly("alpha");
        assertThat(composition.ghostDisabledNames()).containsExactly("typo_name");
    }

    @Test
    void compositionSnapshotIsImmutable() {
        HookChain chain = new HookChain(
                List.of(new NamedHook(1, "alpha")),
                Set.of("typo_name"));

        ChainComposition composition = chain.composition();
        assertThatThrownBy(() -> composition.resolvedHookNames().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> composition.ghostDisabledNames().add("y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void emptyChainAndEmptyDisablesGiveEmptyComposition() {
        HookChain chain = HookChain.of(List.of());

        ChainComposition composition = chain.composition();
        assertThat(composition.resolvedHookNames()).isEmpty();
        assertThat(composition.ghostDisabledNames()).isEmpty();
    }
}
