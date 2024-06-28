package io.alv.core.gateway;

import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class VertxHttpGateway extends AbstractVerticle {


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    // todo load http/websocket/grpc routes from MessageHandlers
    // todo handle authorization
    // todo handle validation
    // todo handle broadcast subscriptions

  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    super.stop(stopPromise);
  }
}
