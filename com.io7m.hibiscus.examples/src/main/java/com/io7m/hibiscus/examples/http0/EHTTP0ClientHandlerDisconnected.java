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

  private final AtomicBoolean closed;

  EHTTP0ClientHandlerDisconnected()
  {
    this.closed = new AtomicBoolean(false);
  }

  @Override
  public HBConnectionResultType<
    EHTTP0MessageType,
    EHTTP0ConnectionParameters,
    EHTTP0Exception>
  doConnect(
    final EHTTP0ConnectionParameters parameters)
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
                        new HBConnection<>(transport)
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
  public boolean isConnected()
  {
    return false;
  }

  @Override
  public boolean isClosed()
  {
    return this.closed.get();
  }

  @Override
  public void close()
  {
    this.closed.set(true);
  }

  @Override
  public void doSend(
    final EHTTP0MessageType message)
    throws EHTTP0Exception
  {
    Objects.requireNonNull(message, "message");
    throw new EHTTP0Exception("Not connected!");
  }

  @Override
  public Optional<EHTTP0MessageType> doReceive(
    final Duration timeout)
    throws EHTTP0Exception
  {
    Objects.requireNonNull(timeout, "timeout");
    throw new EHTTP0Exception("Not connected!");
  }

  @Override
  public <R extends EHTTP0MessageType> R doAsk(
    final EHTTP0MessageType message)
    throws EHTTP0Exception
  {
    Objects.requireNonNull(message, "message");
    throw new EHTTP0Exception("Not connected!");
  }
}
