package com.anyqn.adbjdbc.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerWithException<Target, ExObj extends Exception> {
    void accept(Target target) throws ExObj;

    static <Target> Consumer<Target> handlerBuilder(ConsumerWithException<Target, Exception> handlingConsumer) {
        return obj -> {
            try {
                handlingConsumer.accept(obj);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }
}