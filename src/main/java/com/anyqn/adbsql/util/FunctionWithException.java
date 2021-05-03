package com.anyqn.adbsql.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface FunctionWithException<Target, Response, ExObj extends Exception> {
    static <Target, Response> Consumer<Target> handlerBuilder(
            final FunctionWithException<Target, Response, Exception> handlingConsumer) {
        return obj -> {
            try {
                handlingConsumer.accept(obj);
            } catch (final Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    Response accept(Target target) throws ExObj;
}