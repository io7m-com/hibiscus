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

import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBTransportClosed;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.hibiscus.api.HBClientHandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public final class ETCP0ClientHandlerDisconnected
  extends ETCP0ClientHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ETCP0ClientHandlerDisconnected.class);

  private final Clock clock;
  private final HBTransportType<ETCP0MessageType, ETCP0Exception> transportClosed;

  ETCP0ClientHandlerDisconnected(
    final Clock inClock)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.transportClosed =
      new HBTransportClosed<>(ETCP0Exception::new);
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
    Objects.requireNonNull(parameters, "credentials");

    try {
      final var socket = new Socket();
      socket.connect(parameters.address());

      final var transport =
        new ETCP0Transport(
          this.clock,
          socket,
          socket.getInputStream(),
          socket.getOutputStream()
        );

      boolean keepTransport = false;

      try {
        final var response =
          transport.sendAndWait(
            new ETCP0CommandLogin(
              UUID.randomUUID(),
              parameters.user(),
              parameters.password()
            ),
            parameters.connectTimeout()
          );

        LOG.debug("Response: {}", response);

        return switch (response) {
          case final ETCP0CommandType c -> {
            yield new HBConnectionFailed<>(c);
          }

          case final ETCP0ResponseType r -> {
            yield switch (r) {
              case final ETCP0ResponseFailure f -> {
                yield new HBConnectionFailed<>(f);
              }

              case final ETCP0ResponseOK ok -> {
                keepTransport = true;
                yield new HBConnectionSucceeded<>(
                  ok,
                  new ETCP0ClientHandlerConnected(this.clock, transport)
                );
              }
            };
          }
        };
      } catch (final ETCP0Exception | TimeoutException e) {
        return new HBConnectionError<>(e);
      } finally {
        if (!keepTransport) {
          try {
            transport.close();
          } catch (final ETCP0Exception e) {
            // Can't do anything about this.
          }
        }
      }
    } catch (final IOException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBTransportType<ETCP0MessageType, ETCP0Exception> transport()
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
    throws ETCP0Exception
  {

  }
}
