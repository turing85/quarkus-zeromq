package de.turing85.zeromq.service.one.config;

import java.util.concurrent.ExecutorService;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import de.turing85.quarkus.zeromq.ZClient;
import de.turing85.quarkus.zeromq.ZServer;
import io.quarkus.arc.Unremovable;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.Startup;
import org.zeromq.ZContext;

public class ZProducer {
  @Produces
  @Singleton
  public ZContext zContext() {
    return new ZContext();
  }

  @Produces
  @Singleton
  @Unremovable
  @Startup
  public ZServer zSErver(final ZContext zContext,
      @SuppressWarnings("CdiInjectionPointsInspection") final ExecutorService executor) {
    return new ZServer(zContext, 1111, executor).start();
  }

  @Produces
  @Singleton
  @Unremovable
  @Startup
  public ZClient zClient(ZContext zContext) {
    return new ZClient(zContext, 2222).connect();
  }

  public void shutdown(@Observes final ShutdownEvent event, final ZClient zClient,
      final ZServer zServer, final ZContext zContext) {
    Log.info("Disconnecting zClient");
    zClient.disconnect();
    Log.info("Stopping zServer");
    zServer.stop();
    Log.info("Closing zContext");
    zContext.close();
  }
}
