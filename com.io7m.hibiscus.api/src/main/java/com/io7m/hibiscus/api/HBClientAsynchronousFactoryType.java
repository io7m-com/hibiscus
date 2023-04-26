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

package com.io7m.hibiscus.api;

/**
 * A factory of asynchronous clients.
 *
 * @param <X>  The type of exceptions that can be raised by clients
 * @param <G>  The type of client configuration values
 * @param <C>  The type of commands sent to the server by the client
 * @param <RS> The type of responses returned from the server
 * @param <RF> The type of failure responses returned from the server
 * @param <E>  The type of events received from the server
 * @param <CR> The type of credentials
 * @param <L>  The type of clients
 */

public interface HBClientAsynchronousFactoryType<
  X extends Exception,
  G extends HBConfigurationType,
  C extends HBCommandType,
  RS extends HBResponseType,
  RF extends HBResponseType,
  E extends HBEventType,
  CR extends HBCredentialsType,
  L extends HBClientAsynchronousType<X, C, RS, RF, E, CR>>
{
  /**
   * Open a new client.
   *
   * @param configuration The client configuration
   *
   * @return A new client
   */

  L openAsynchronousClient(G configuration);
}