package de.turing85.quarkus.zeromq;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import lombok.extern.slf4j.Slf4j;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

@Slf4j
public final class ZServer {
  private final ZMQ.Socket serverSocket;
  private final int port;
  private final ExecutorService executor;

  private volatile boolean running;
  private volatile boolean stopRequested;

  public ZServer(final ZContext zContext, final int port, final ExecutorService executor) {
    serverSocket = zContext.createSocket(SocketType.SERVER);
    serverSocket.setReceiveTimeOut((int) Duration.ofSeconds(1).toMillis());
    this.port = port;
    this.executor = executor;
    running = false;
    stopRequested = false;
  }

  public synchronized ZServer start() {
    if (!running) {
      log.info("Starting ZServer on port {}", port);
      serverSocket.bind("tcp://*:%d".formatted(port));
      running = true;
      executor.execute(() -> {
        synchronized (this) {
          while (!stopRequested) {
            final byte[] rawMessage = serverSocket.recv();
            if (rawMessage != null) {
              log.info("Received message \"{}\"", new String(rawMessage, ZMQ.CHARSET));
            }
            try {
              this.wait(Duration.ofMillis(50).toMillis());
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          log.info("Stopping ZServer on port {}", port);
          running = false;
        }
      });
    }
    return this;
  }

  public void stop() {
    if (stopRequested || !running) {
      return;
    }
    synchronized (this) {
      if (stopRequested) {
        return;
      }
      log.debug("Setting stop signal for ZServer on port {}", port);
      stopRequested = true;
      while (running) {
        try {
          this.wait(Duration.ofMillis(50).toMillis());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
    serverSocket.close();
  }
}

