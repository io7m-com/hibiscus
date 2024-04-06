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


package com.io7m.hibiscus.tests.examples.http0;

import com.io7m.hibiscus.api.HBTransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class EHTTP0Transport
  implements HBTransportType<EHTTP0MessageType, EHTTP0Exception>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EHTTP0Transport.class);

  private final LinkedBlockingQueue<EHTTP0MessageType> inbox;
  private final HttpClient http;
  private final URI target;

  EHTTP0Transport(
    final HttpClient inHttp,
    final URI inTarget)
  {
    this.http =
      Objects.requireNonNull(inHttp, "http");
    this.target =
      Objects.requireNonNull(inTarget, "target");
    this.inbox =
      new LinkedBlockingQueue<>();
  }

  @Override
  public Optional<EHTTP0MessageType> read(
    final Duration timeout)
    throws EHTTP0Exception
  {
    Objects.requireNonNull(timeout, "timeout");

    try {
      if (this.isClosed()) {
        throw new EHTTP0Exception(new ClosedChannelException());
      }
      return Optional.ofNullable(
        this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS)
      );
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  @Override
  public void write(
    final EHTTP0MessageType message)
    throws EHTTP0Exception
  {
    final var data =
      EHTTP0Messages.toBytes(message);

    final var future =
      this.http.sendAsync(
        HttpRequest.newBuilder()
          .uri(this.target)
          .POST(HttpRequest.BodyPublishers.ofByteArray(data))
          .build(),
        HttpResponse.BodyHandlers.ofByteArray()
      );

    future.whenComplete((httpResponse, throwable) -> {
      LOG.debug("httpResponse: {}", httpResponse);
      LOG.debug("throwable:      ", throwable);

      if (httpResponse != null) {
        try {
          this.inbox.add(EHTTP0Messages.fromBytes(httpResponse.body()));
        } catch (final EHTTP0Exception e) {
          future.completeExceptionally(e);
        }
      }
    });
  }

  @Override
  public void close()
    throws EHTTP0Exception
  {
    this.http.close();
  }

  @Override
  public boolean isClosed()
  {
    return this.http.isTerminated();
  }
}
