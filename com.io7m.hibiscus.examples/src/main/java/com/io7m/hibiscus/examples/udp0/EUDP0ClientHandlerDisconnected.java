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
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBTransportClosed;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.hibiscus.api.HBClientHandlerType;
import com.io7m.hibiscus.examples.http0.EHTTP0ClientHandlerDisconnected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public final class EUDP0ClientHandlerDisconnected
  extends EUDP0ClientHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EHTTP0ClientHandlerDisconnected.class);

  private final HBTransportClosed<EUDP0MessageType, EUDP0Exception> transportClosed;
  private final Clock clock;

  EUDP0ClientHandlerDisconnected(
    final Clock inClock)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.transportClosed =
      new HBTransportClosed<>(EUDP0Exception::new);
  }

  @Override
  public HBConnectionResultType<
    EUDP0MessageType,
    EUDP0ConnectionParameters,
    HBClientHandlerType<EUDP0MessageType, EUDP0ConnectionParameters, EUDP0Exception>,
    EUDP0Exception>
  doConnect(
    final EUDP0ConnectionParameters parameters)
    throws InterruptedException
  {
    Objects.requireNonNull(parameters, "credentials");

    try {
      final var socket =
        new DatagramSocket();
      final var transport =
        new EUDP0Transport(this.clock, parameters.address(), socket);

      boolean keepTransport = false;

      try {
        final var response =
          transport.sendAndWait(
            new EUDP0CommandLogin(
              UUID.randomUUID(),
              parameters.user(),
              parameters.password()
            ),
            parameters.connectTimeout()
          );

        LOG.debug("Response: {}", response);

        return switch (response) {
          case final EUDP0CommandType c -> {
            yield new HBConnectionFailed<>(c);
          }

          case final EUDP0ResponseType r -> {
            yield switch (r) {
              case final EUDP0ResponseFailure f -> {
                yield new HBConnectionFailed<>(f);
              }

              case final EUDP0ResponseOK ok -> {
                keepTransport = true;
                yield new HBConnectionSucceeded<>(
                  ok,
                  new EUDP0ClientHandlerConnected(transport)
                );
              }
            };
          }
        };
      } catch (final EUDP0Exception | TimeoutException e) {
        return new HBConnectionError<>(e);
      } finally {
        if (!keepTransport) {
          try {
            transport.close();
          } catch (final EUDP0Exception e) {
            // Can't do anything about this.
          }
        }
      }
    } catch (final IOException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBTransportType<EUDP0MessageType, EUDP0Exception> transport()
  {
    return this.transportClosed;
  }

  @Override
  public boolean isClosed()
  {
    return this.transportClosed.isClosed();
  }

  @Override
  public void close()
    throws EUDP0Exception
  {
    this.transportClosed.close();
  }
}
