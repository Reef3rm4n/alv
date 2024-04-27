package io.alv.core.handler.messages.output;


public sealed interface Output permits ErrorMessage, Event, Ack {
}
