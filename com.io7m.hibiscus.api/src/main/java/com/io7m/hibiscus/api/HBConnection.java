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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

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
  private final LinkedBlockingQueue<M> received;
  private final HBTransportType<M, X> transport;
  private final Clock clock;

  /**
   * An implementation of the {@link HBConnectionType} interface.
   *
   * @param inClock           The clock used for timeouts
   * @param inTransport       The underlying transport
   * @param receiveQueueBound The maximum number of messages that can be held
   *                          in the receive queue
   */

  public HBConnection(
    final Clock inClock,
    final HBTransportType<M, X> inTransport,
    final int receiveQueueBound)
  {
    this.clock =
      Objects.requireNonNull(inClock, "clock");
    this.received =
      new LinkedBlockingQueue<>(receiveQueueBound);
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
    final M message,
    final Duration timeout)
    throws
    X,
    InterruptedException,
    TimeoutException,
    HBConnectionReceiveQueueOverflowException
  {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(timeout, "timeout");

    this.transport.write(message);

    final var timeLater =
      Instant.now(this.clock).plus(timeout);

    while (true) {
      final var timeNow = Instant.now(this.clock);
      if (timeNow.isAfter(timeLater)) {
        throw new TimeoutException(
          "No response received in %s".formatted(timeout));
      }

      final var responseOpt = this.transport.read(timeout);
      if (responseOpt.isPresent()) {
        final var response = responseOpt.get();
        if (response.isResponseFor(message)) {
          return (R) response;
        }

        try {
          this.received.add(response);
        } catch (final IllegalStateException e) {
          throw new HBConnectionReceiveQueueOverflowException(
            this.received.size()
          );
        }
      }
    }
  }

  @Override
  public Optional<M> receive(
    final Duration timeout)
    throws X, InterruptedException
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
