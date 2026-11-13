package com.interviewai.service;

import java.io.*;
import java.util.concurrent.*;

/** Owns the upstream request and body so timeout/disconnect can stop blocked reads. */
public final class StreamControl implements AutoCloseable {
  private boolean cancelled;
  private InputStream body;
  private CompletableFuture<?> request;

  public synchronized void check() {
    if (cancelled || Thread.currentThread().isInterrupted())
      throw new CancellationException("Stream closed");
  }

  public synchronized void request(CompletableFuture<?> value) {
    request = value;
    if (cancelled || Thread.currentThread().isInterrupted()) {
      value.cancel(true);
      throw new CancellationException("Stream closed");
    }
  }

  public synchronized void body(InputStream value) throws IOException {
    if (cancelled) {
      value.close();
      throw new CancellationException("Stream closed");
    }
    body = value;
  }

  public synchronized void close() {
    cancelled = true;
    if (request != null) request.cancel(true);
    if (body != null)
      try {
        body.close();
      } catch (IOException ignored) {
      }
  }
}
