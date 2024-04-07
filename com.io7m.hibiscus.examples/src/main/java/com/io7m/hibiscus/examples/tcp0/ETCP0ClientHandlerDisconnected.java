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

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionClosed;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.basic.HBClientHandlerAndMessage;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionFailed;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.hibiscus.basic.HBConnectionSucceeded;
import com.io7m.hibiscus.examples.http0.EHTTP0Exception;
import com.io7m.hibiscus.examples.http0.EHTTP0MessageType;

import java.io.IOException;
import java.net.Socket;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ETCP0ClientHandlerDisconnected
  extends ETCP0ClientHandlerAbstract
{
  private final HBConnectionClosed<ETCP0MessageType, ETCP0Exception> connection;
  private final int maximumReceiveQueueSize;
  private final Clock clock;

  ETCP0ClientHandlerDisconnected(
    final Clock inClock,
    final int inMaximumReceiveQueueSize)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.maximumReceiveQueueSize =
      inMaximumReceiveQueueSize;
    this.connection =
      new HBConnectionClosed<>(ETCP0Exception::new);
  }

  @Override
  public HBConnectionResultType<
    ETCP0MessageType,
    ETCP0ConnectionParameters,
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
          socket,
          socket.getInputStream(),
          socket.getOutputStream()
        );

      boolean keepTransport = false;

      try {
        transport.write(new ETCP0CommandLogin(
          UUID.randomUUID(),
          parameters.user(),
          parameters.password()
        ));

        final var responseOpt =
          transport.read(parameters.connectTimeout());

        if (responseOpt.isPresent()) {
          return switch (responseOpt.get()) {
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
                    new HBClientHandlerAndMessage<>(
                      new ETCP0ClientHandlerConnected(
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
      } catch (final ETCP0Exception e) {
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
  public HBConnectionType<ETCP0MessageType, ETCP0Exception> connection()
  {
    return this.connection;
  }

  @Override
  public void close()
    throws ETCP0Exception
  {
    this.connection.close();
  }
}
