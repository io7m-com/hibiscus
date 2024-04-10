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


package com.io7m.hibiscus.examples.udp0;

import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.hibiscus.api.HBClientHandlerType;

import java.nio.channels.AlreadyConnectedException;
import java.util.Objects;

public final class EUDP0ClientHandlerConnected
  extends EUDP0ClientHandlerAbstract
{
  private final HBTransportType<EUDP0MessageType, EUDP0Exception> transport;

  EUDP0ClientHandlerConnected(
    final HBTransportType<EUDP0MessageType, EUDP0Exception> inConnection)
  {
    this.transport =
      Objects.requireNonNull(inConnection, "transport");
  }

  @Override
  public HBConnectionResultType<
    EUDP0MessageType,
    EUDP0ConnectionParameters,
    HBClientHandlerType<EUDP0MessageType, EUDP0ConnectionParameters, EUDP0Exception>,
    EUDP0Exception>
  doConnect(
    final EUDP0ConnectionParameters parameters)
  {
    return new HBConnectionError<>(new AlreadyConnectedException());
  }

  @Override
  public HBTransportType<EUDP0MessageType, EUDP0Exception> transport()
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
    throws EUDP0Exception
  {
    this.transport.close();
  }
}
