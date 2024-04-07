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

import java.nio.channels.NotYetConnectedException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * An always-closed connection.
 *
 * @param <M> The type of messages
 * @param <X> The type of exceptions
 */

public final class HBConnectionClosed<
  M extends HBMessageType,
  X extends Exception>
  implements HBConnectionType<M, X>
{
  private final Function<Exception, X> exceptions;

  /**
   * An always-closed connection.
   *
   * @param inExceptions A function to construct exceptions of type {@code X}
   */

  public HBConnectionClosed(
    final Function<Exception, X> inExceptions)
  {
    this.exceptions =
      Objects.requireNonNull(inExceptions, "exceptions");
  }

  @Override
  public boolean isClosed()
  {
    return true;
  }

  @Override
  public void close()
    throws X
  {

  }

  @Override
  public void send(
    final M message)
    throws X, InterruptedException
  {
    throw this.exceptions.apply(new NotYetConnectedException());
  }

  @Override
  public <R extends M> R ask(
    final M message,
    final Duration timeout)
    throws X
  {
    throw this.exceptions.apply(new NotYetConnectedException());
  }

  @Override
  public Optional<M> receive(
    final Duration timeout)
    throws X, InterruptedException
  {
    throw this.exceptions.apply(new NotYetConnectedException());
  }
}
