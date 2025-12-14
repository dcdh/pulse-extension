package com.damdamdeo.pulse.extension.consumer.runtime.notification;

public record NotifyEvent(String eventName, Class<?> type, Object data) {
}
