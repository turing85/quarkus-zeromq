package de.turing85.zeromq.service.one;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Singleton;

import de.turing85.quarkus.zeromq.ZClient;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor
public class PingSender {
  private final ZClient zClient;
  private final AtomicInteger counter = new AtomicInteger(0);

  @Scheduled(every = "1s")
  void sendPing() {
    final String message = "Ping from one to two - %d".formatted(counter.incrementAndGet());
    Log.infof("Sending ping message \"%s\"", message);
    if (zClient.send(message)) {
      Log.debugf("Message \"%s\" sent successfully", message);
    } else {
      Log.warnf("Unable to send ping message \"%s\"", message);
    }
  }
}
