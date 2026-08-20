package com.rickgao.careercore.common.idempotency;

/**
 * 幂等执行回调。
 */
@FunctionalInterface
public interface IdempotentSupplier<T> {

    T get();
}
