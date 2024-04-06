/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.hibiscus.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * An implementation of the {@link HBConnectionType} interface.
 *
 * @param <M> The type of messages
 * @param <X> The type of exceptions
 */

public final class HBConnection<
  M extends HBMessageType,
  X extends Exception>
  implements HBConnectionType<M, X>
{
  private static final Duration ASK_POLL_TIMEOUT =
    Duration.ofMillis(10L);

  private final LinkedBlockingQueue<M> received;
  private final HBTransportType<M, X> transport;

  /**
   * An implementation of the {@link HBConnectionType} interface.
   *
   * @param inTransport The underlying transport
   */

  public HBConnection(
    final HBTransportType<M, X> inTransport)
  {
    this.received =
      new LinkedBlockingQueue<>();
    this.transport =
      Objects.requireNonNull(inTransport, "transport");
  }

  @Override
  public void send(
    final M message)
    throws X
  {
    Objects.requireNonNull(message, "message");
    this.transport.write(message);
  }

  @Override
  public <R extends M> R ask(
    final M message)
    throws X, InterruptedException
  {
    Objects.requireNonNull(message, "message");

    this.transport.write(message);
    while (!Thread.interrupted()) {
      final var responseOpt = this.transport.read(ASK_POLL_TIMEOUT);
      if (responseOpt.isPresent()) {
        final var response = responseOpt.get();
        if (response.isResponseFor(message)) {
          return (R) response;
        }
        this.received.add(response);
      }
    }
    throw new InterruptedException();
  }

  @Override
  public Optional<M> receive(
    final Duration timeout)
    throws X
  {
    try {
      final var m = this.received.poll(timeout.toNanos(), NANOSECONDS);
      if (m != null) {
        return Optional.of(m);
      }
    } catch (final InterruptedException e) {
      return this.transport.read(timeout);
    }
    return this.transport.read(timeout);
  }

  @Override
  public boolean isClosed()
  {
    return this.transport.isClosed();
  }

  @Override
  public void close()
    throws X
  {
    this.transport.close();
  }
}
