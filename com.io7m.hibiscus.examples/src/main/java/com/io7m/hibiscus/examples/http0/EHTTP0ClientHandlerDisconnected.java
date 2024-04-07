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


package com.io7m.hibiscus.examples.http0;

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionClosed;
import com.io7m.hibiscus.api.HBConnectionType;
import com.io7m.hibiscus.api.HBMessageType;
import com.io7m.hibiscus.basic.HBClientHandlerAndMessage;
import com.io7m.hibiscus.basic.HBConnectionError;
import com.io7m.hibiscus.basic.HBConnectionFailed;
import com.io7m.hibiscus.basic.HBConnectionResultType;
import com.io7m.hibiscus.basic.HBConnectionSucceeded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EHTTP0ClientHandlerDisconnected
  extends EHTTP0ClientHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EHTTP0ClientHandlerDisconnected.class);

  private final HBConnectionClosed<EHTTP0MessageType, EHTTP0Exception> connection;
  private final int maximumReceiveQueueSize;
  private final Clock clock;

  EHTTP0ClientHandlerDisconnected(
    final Clock inClock,
    final int inMaximumReceiveQueueSize)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.maximumReceiveQueueSize =
      inMaximumReceiveQueueSize;
    this.connection =
      new HBConnectionClosed<>(EHTTP0Exception::new);
  }

  @Override
  public HBConnectionResultType<
    EHTTP0MessageType,
    EHTTP0ConnectionParameters,
    EHTTP0Exception>
  doConnect(
    final EHTTP0ConnectionParameters parameters)
    throws InterruptedException
  {
    Objects.requireNonNull(parameters, "credentials");

    try {
      final var http =
        HttpClient.newBuilder()
          .executor(Executors.newVirtualThreadPerTaskExecutor())
          .build();

      final var uri =
        new URI(
          "http",
          null,
          parameters.address().getHostName(),
          parameters.address().getPort(),
          "/",
          null,
          null
        );

      final var transport =
        new EHTTP0Transport(http, uri);

      boolean keepTransport = false;

      try {
        transport.write(new EHTTP0CommandLogin(
          UUID.randomUUID(),
          parameters.user(),
          parameters.password()
        ));

        final var responseOpt =
          transport.read(parameters.connectTimeout());

        LOG.debug("Response: {}", responseOpt);

        if (responseOpt.isPresent()) {
          return switch (responseOpt.get()) {
            case final EHTTP0CommandType c -> {
              yield new HBConnectionFailed<>(c);
            }

            case final EHTTP0ResponseType r -> {
              yield switch (r) {
                case final EHTTP0ResponseFailure f -> {
                  yield new HBConnectionFailed<>(f);
                }

                case final EHTTP0ResponseOK ok -> {
                  keepTransport = true;
                  yield new HBConnectionSucceeded<>(
                    new HBClientHandlerAndMessage<>(
                      new EHTTP0ClientHandlerConnected(
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
      } catch (final EHTTP0Exception e) {
        return new HBConnectionError<>(e);
      } finally {
        if (!keepTransport) {
          try {
            transport.close();
          } catch (final EHTTP0Exception e) {
            // Can't do anything about this.
          }
        }
      }
    } catch (final URISyntaxException e) {
      return new HBConnectionError<>(e);
    }
  }

  @Override
  public HBConnectionType<EHTTP0MessageType, EHTTP0Exception> connection()
  {
    return this.connection;
  }

  @Override
  public void close()
    throws EHTTP0Exception
  {
    this.connection.close();
  }
}
