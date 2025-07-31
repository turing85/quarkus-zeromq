package de.turing85.quarkus.zeromq;

import java.time.Duration;

import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Slf4j
public final class ZClient {
  public static final int MAX_TRIES = 100;

  private final ZMQ.Socket clientSocket;
  private final int port;

  private volatile boolean running;
  private volatile boolean stopRequested;

  public ZClient(final ZContext zContext, final int port) {
    clientSocket = zContext.createSocket(SocketType.CLIENT);
    this.port = port;
    running = false;
    stopRequested = false;
  }

  public ZClient connect() {
    if (running) {
      return this;
    }
    synchronized (this) {
      if (running) {
        return this;
      }
      for (int numTry = 0; numTry < MAX_TRIES; ++numTry) {
        log.debug("Trying to connect to server at port {} (try: {})", port, numTry);
        if (clientSocket.connect("tcp://localhost:" + port)) {
          if (log.isDebugEnabled()) {
            log.debug("Connected to server at port {} on try {}", port, numTry);
          } else {
            log.info("Connected to server at port {}", port);
          }
          running = true;
          return this;
        }

        while (!stopRequested) {
          try {
            this.wait(Duration.ofSeconds(1).toMillis());
            break;
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }
    final String message = "Failed to connect to server at port %d after %d tries, stopping"
        .formatted(port, MAX_TRIES);
    log.info(message);
    throw new IllegalStateException(message);
  }

  public void disconnect() {
    if (stopRequested || !running) {
      return;
    }
    synchronized (this) {
      if (stopRequested) {
        return;
      }
      log.debug("Disconnecting from server at port {}", port);
      stopRequested = true;
      while (!running) {
        try {
          this.wait(Duration.ofMillis(50).toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      running = false;
    }
    clientSocket.close();
  }

  public boolean send(final String message) {
    if (!running || stopRequested) {
      log.warn("Connection to server at port {} is closed", port);
      return false;
    }
    return clientSocket.send(message.getBytes(ZMQ.CHARSET));
  }
}
