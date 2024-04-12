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
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * The type of RPC client handlers.
 *
 * @param <M> The type of messages
 * @param <P> The type of connection parameters
 * @param <X> the type of exceptions
 */

public interface HBClientHandlerType<
  M extends HBMessageType,
  P extends HBConnectionParametersType,
  X extends Exception>
  extends HBClientCloseableType<X>, HBIOOperationsType<M, X>
{
  /**
   * @return A function to convert arbitrary exceptions to {@code X}
   */

  Function<Throwable, X> exceptionTransformer();

  @Override
  default HBReadType<M> receive(
    final Duration timeout)
    throws X, InterruptedException
  {
    return this.transport()
      .receive(timeout);
  }

  @Override
  default void send(
    final M message)
    throws X, InterruptedException
  {
    this.transport()
      .send(message);
  }

  @Override
  default void sendAndForget(
    final M message)
    throws X, InterruptedException
  {
    this.transport()
      .sendAndForget(message);
  }

  @Override
  default M sendAndWait(
    final M message,
    final Duration timeout)
    throws X, InterruptedException, TimeoutException
  {
    return this.transport()
      .sendAndWait(message, timeout);
  }

  /**
   * Create a new connection to the server.
   *
   * @param parameters The connection parameters
   *
   * @return The connection result
   */

  HBConnectionResultType<M, P, HBClientHandlerType<M, P, X>, X> doConnect(
    P parameters)
    throws InterruptedException;

  /**
   * @return The underlying transport
   */

  HBTransportType<M, X> transport();
}
