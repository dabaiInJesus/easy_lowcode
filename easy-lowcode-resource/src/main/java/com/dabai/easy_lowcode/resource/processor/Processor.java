package com.dabai.easy_lowcode.resource.processor;

public interface Processor<T> {
    String getType();
    T process(T input, ProcessorContext context);
    int getOrder();
}
