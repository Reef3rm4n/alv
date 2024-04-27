package io.alv.core.cluster;

import io.alv.core.gateway.VertxHttpGateway;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.mutiny.core.Vertx;

import java.time.Duration;

public class GatewayApp {

  private Vertx vertx;


  public void start() {
    vertx = Vertx.vertx();
    vertx.deployVerticle(VertxHttpGateway::new, new DeploymentOptions().setInstances(CpuCoreSensor.availableProcessors() * 2))
      .await().atMost(Duration.ofSeconds(10));
  }

  public void stop() {
    vertx.close().await().atMost(Duration.ofSeconds(10));
  }

}
