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

import com.io7m.hibiscus.api.HBReadNothing;
import com.io7m.hibiscus.api.HBReadType;
import com.io7m.hibiscus.api.HBReadResponse;
import com.io7m.hibiscus.api.HBTransportType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class EHTTP0Transport
  implements HBTransportType<EHTTP0MessageType, EHTTP0Exception>
{
  private final LinkedBlockingQueue<MessageAndResponse> inbox;
  private final HttpClient http;
  private final URI target;

  private record MessageAndResponse(
    EHTTP0MessageType message,
    EHTTP0MessageType response)
  {

  }

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
  public HBReadType<EHTTP0MessageType> receive(
    final Duration timeout)
    throws EHTTP0Exception, InterruptedException
  {
    Objects.requireNonNull(timeout, "timeout");

    if (this.isClosed()) {
      throw new EHTTP0Exception(new ClosedChannelException());
    }

    final var r =
      this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);

    if (r == null) {
      return new HBReadNothing<>();
    }
    return new HBReadResponse<>(r.message(), r.response());
  }

  @Override
  public void send(
    final EHTTP0MessageType message)
    throws EHTTP0Exception, InterruptedException
  {
    final var data =
      EHTTP0Messages.toBytes(message);

    final HttpResponse<byte[]> httpResponse;
    try {
      httpResponse = this.http.send(
        HttpRequest.newBuilder()
          .uri(this.target)
          .POST(HttpRequest.BodyPublishers.ofByteArray(data))
          .build(),
        HttpResponse.BodyHandlers.ofByteArray()
      );
    } catch (final IOException e) {
      throw new EHTTP0Exception(e);
    }

    this.inbox.add(
      new MessageAndResponse(
        message,
        EHTTP0Messages.fromBytes(httpResponse.body())
      )
    );
  }

  @Override
  public void sendAndForget(
    final EHTTP0MessageType message)
    throws EHTTP0Exception, InterruptedException
  {
    final var data =
      EHTTP0Messages.toBytes(message);

    try {
      this.http.send(
        HttpRequest.newBuilder()
          .uri(this.target)
          .POST(HttpRequest.BodyPublishers.ofByteArray(data))
          .build(),
        HttpResponse.BodyHandlers.discarding()
      );
    } catch (final IOException e) {
      throw new EHTTP0Exception(e);
    }
  }

  @Override
  public EHTTP0MessageType sendAndWait(
    final EHTTP0MessageType message,
    final Duration timeout)
    throws EHTTP0Exception, InterruptedException, TimeoutException
  {
    final var data =
      EHTTP0Messages.toBytes(message);

    final HttpResponse<byte[]> httpResponse;
    try {
      httpResponse = this.http.send(
        HttpRequest.newBuilder()
          .uri(this.target)
          .POST(HttpRequest.BodyPublishers.ofByteArray(data))
          .timeout(timeout)
          .build(),
        HttpResponse.BodyHandlers.ofByteArray()
      );
    } catch (final IOException e) {
      throw new EHTTP0Exception(e);
    }

    return EHTTP0Messages.fromBytes(httpResponse.body());
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
