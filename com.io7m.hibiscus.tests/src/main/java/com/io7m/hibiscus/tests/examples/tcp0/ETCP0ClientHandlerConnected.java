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


package com.io7m.hibiscus.tests.examples.tcp0;

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionResultType;

import java.nio.channels.AlreadyConnectedException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class ETCP0ClientHandlerConnected
  extends ETCP0ClientHandlerAbstract
{
  private final HBConnection<ETCP0MessageType, ETCP0Exception> connection;

  ETCP0ClientHandlerConnected(
    final HBConnection<ETCP0MessageType, ETCP0Exception> inConnection)
  {
    this.connection =
      Objects.requireNonNull(inConnection, "connection");
  }

  @Override
  public HBConnectionResultType<
      ETCP0MessageType,
      ETCP0ConnectionParameters,
      ETCP0Exception>
  doConnect(
    final ETCP0ConnectionParameters parameters)
  {
    return new HBConnectionError<>(new AlreadyConnectedException());
  }

  @Override
  public boolean isConnected()
  {
    return !this.connection.isClosed();
  }

  @Override
  public boolean isClosed()
  {
    return this.connection.isClosed();
  }

  @Override
  public void close()
    throws ETCP0Exception
  {
    this.connection.close();
  }

  @Override
  public void doSend(
    final ETCP0MessageType message)
    throws ETCP0Exception
  {
    this.connection.send(message);
  }

  @Override
  public Optional<ETCP0MessageType> doReceive(
    final Duration timeout)
    throws ETCP0Exception
  {
    return this.connection.receive(timeout);
  }

  @Override
  public <R extends ETCP0MessageType> R doAsk(
    final ETCP0MessageType message)
    throws ETCP0Exception, InterruptedException
  {
    return this.connection.ask(message);
  }
}
