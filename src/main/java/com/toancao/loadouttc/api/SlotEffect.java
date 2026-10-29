package com.toancao.loadouttc.api;

/** A custom slot animation. Register it with {@link LoadoutTcApi#registerEffect}. */
@FunctionalInterface
public interface SlotEffect {
    EffectFrame apply(EffectContext context);
}
