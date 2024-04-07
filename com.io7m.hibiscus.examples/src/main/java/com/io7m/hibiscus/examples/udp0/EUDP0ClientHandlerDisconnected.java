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

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionClosed;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.basic.HBClientHandlerAndMessage;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionFailed;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.hibiscus.basic.HBConnectionSucceeded;
import com.io7m.hibiscus.examples.tcp0.ETCP0Exception;
import com.io7m.hibiscus.examples.tcp0.ETCP0MessageType;

import java.io.IOException;
import java.net.DatagramSocket;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EUDP0ClientHandlerDisconnected
  extends EUDP0ClientHandlerAbstract
{
  private final HBConnectionClosed<EUDP0MessageType, EUDP0Exception> connection;
  private final Clock clock;
  private final int maximumReceiveQueueSize;

  EUDP0ClientHandlerDisconnected(
    final Clock inClock,
    final int inMaximumReceiveQueueSize)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.maximumReceiveQueueSize =
      inMaximumReceiveQueueSize;
    this.connection =
      new HBConnectionClosed<>(EUDP0Exception::new);
  }

  @Override
  public HBConnectionResultType<
    EUDP0MessageType,
    EUDP0ConnectionParameters,
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
        new EUDP0Transport(parameters.address(), socket);

      boolean keepTransport = false;

      try {
        transport.write(new EUDP0CommandLogin(
          UUID.randomUUID(),
          parameters.user(),
          parameters.password()
        ));

        final var responseOpt =
          transport.read(parameters.connectTimeout());

        if (responseOpt.isPresent()) {
          return switch (responseOpt.get()) {
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
                    new HBClientHandlerAndMessage<>(
                      new EUDP0ClientHandlerConnected(
                        new HBConnection<>(
                          this.clock,
                          transport,
                          this.maximumReceiveQueueSize
                        )
                      ),
                      ok
                    )
                  );
                }
              };
            }
          };
        }

        return new HBConnectionError<>(new TimeoutException());
      } catch (final EUDP0Exception e) {
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
  public HBConnectionType<EUDP0MessageType, EUDP0Exception> connection()
  {
    return this.connection;
  }

  @Override
  public void close()
    throws EUDP0Exception
  {
    this.connection.close();
  }
}
