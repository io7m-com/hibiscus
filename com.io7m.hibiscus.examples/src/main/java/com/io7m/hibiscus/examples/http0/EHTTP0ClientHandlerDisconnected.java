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

import com.io7m.hibiscus.api.HBConnectionError;
import com.io7m.hibiscus.api.HBConnectionFailed;
import com.io7m.hibiscus.api.HBConnectionResultType;
import com.io7m.hibiscus.api.HBConnectionSucceeded;
import com.io7m.hibiscus.api.HBTransportClosed;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.hibiscus.api.HBClientHandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public final class EHTTP0ClientHandlerDisconnected
  extends EHTTP0ClientHandlerAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EHTTP0ClientHandlerDisconnected.class);

  private final HBTransportType<EHTTP0MessageType, EHTTP0Exception> closedTransport;

  EHTTP0ClientHandlerDisconnected()
  {
    this.closedTransport =
      new HBTransportClosed<>(EHTTP0Exception::new);
  }

  @Override
  public HBConnectionResultType<
    EHTTP0MessageType,
    EHTTP0ConnectionParameters,
    HBClientHandlerType<EHTTP0MessageType, EHTTP0ConnectionParameters, EHTTP0Exception>,
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
        final var response =
          transport.sendAndWait(
            new EHTTP0CommandLogin(
              UUID.randomUUID(),
              parameters.user(),
              parameters.password()
            ),
            parameters.connectTimeout()
          );

        LOG.debug("Response: {}", response);

        return switch (response) {
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
                  ok,
                  new EHTTP0ClientHandlerConnected(transport)
                );
              }
            };
          }
        };
      } catch (final EHTTP0Exception | TimeoutException e) {
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
  public HBTransportType<EHTTP0MessageType, EHTTP0Exception> transport()
  {
    return this.closedTransport;
  }

  @Override
  public boolean isClosed()
  {
    return this.closedTransport.isClosed();
  }

  @Override
  public void close()
    throws EHTTP0Exception
  {

  }
}
