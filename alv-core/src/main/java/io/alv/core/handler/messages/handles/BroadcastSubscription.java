package io.alv.core.handler.messages.handles;

import io.alv.core.handler.messages.output.Event;

public interface BroadcastSubscription {
  void onEvent(Event event);

}
