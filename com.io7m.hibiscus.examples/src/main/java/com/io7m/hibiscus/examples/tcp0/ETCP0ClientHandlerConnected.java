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


package com.io7m.hibiscus.examples.tcp0;

import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.hibiscus.api.HBClientHandlerType;

import java.time.Clock;
import java.util.Objects;

public final class ETCP0ClientHandlerConnected
  extends ETCP0ClientHandlerAbstract
{
  private final HBTransportType<ETCP0MessageType, ETCP0Exception> transport;
  private final Clock clock;

  ETCP0ClientHandlerConnected(
    final Clock inClock,
    final HBTransportType<ETCP0MessageType, ETCP0Exception> inConnection)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.transport =
      Objects.requireNonNull(inConnection, "transport");
  }

  @Override
  public HBConnectionResultType<
    ETCP0MessageType,
    ETCP0ConnectionParameters,
    HBClientHandlerType<ETCP0MessageType, ETCP0ConnectionParameters, ETCP0Exception>,
    ETCP0Exception>
  doConnect(
    final ETCP0ConnectionParameters parameters)
    throws InterruptedException
  {
    return new ETCP0ClientHandlerDisconnected(this.clock)
      .doConnect(parameters);
  }

  @Override
  public HBTransportType<ETCP0MessageType, ETCP0Exception> transport()
  {
    return this.transport;
  }

  @Override
  public boolean isClosed()
  {
    return this.transport.isClosed();
  }

  @Override
  public void close()
    throws ETCP0Exception
  {
    this.transport.close();
  }
}
