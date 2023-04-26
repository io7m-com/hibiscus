/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.hibiscus.tests;

import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.basic.HClientHandlerType;
import com.io7m.hibiscus.basic.HClientNewHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Objects;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public final class HExampleConnected
  implements HClientHandlerType<
  HExampleException,
  HExampleCommandType,
  HExampleResponseType,
  HExampleResponseError,
  HExampleEvent,
  HExampleCredentials>
{
  private final HttpClient httpClient;

  public HExampleConnected(
    final HttpClient inHttpClient)
  {
    this.httpClient =
      Objects.requireNonNull(inHttpClient, "inHttpClient");
  }

  @Override
  public boolean onIsConnected()
  {
    return false;
  }

  @Override
  public List<HExampleEvent> onPollEvents()
  {
    return List.of();
  }

  @Override
  public HBResultType<HClientNewHandler<HExampleException, HExampleCommandType, HExampleResponseType, HExampleResponseError, HExampleEvent, HExampleCredentials>, HExampleResponseError>
  onExecuteLogin(
    final HExampleCredentials credentials)
    throws InterruptedException
  {
    return new HExampleDisconnected(this.httpClient)
      .onExecuteLogin(credentials);
  }

  @Override
  public <C1 extends HExampleCommandType, RS1 extends HExampleResponseType> HBResultType<RS1, HExampleResponseError>
  onExecuteCommand(
    final C1 command)
    throws InterruptedException
  {
    if (command instanceof HExampleEcho echo) {
      final var request =
        HttpRequest.newBuilder()
          .uri(URI.create("http://localhost:20000/echo"))
          .POST(ofString(echo.message()))
          .build();

      final HttpResponse<String> response;
      try {
        response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (final IOException e) {
        return new HBResultFailure<>(new HExampleResponseError(e.getMessage()));
      }

      return new HBResultSuccess<>(
        (RS1) new HExampleResponseEcho(response.body())
      );
    }

    return new HBResultFailure<>(
      new HExampleResponseError("Unrecognized command.")
    );
  }

  @Override
  public void onDisconnect()
  {

  }
}
