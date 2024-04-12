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
 * A factory of clients.
 *
 * @param <C> The type of configurations
 * @param <M> The type of messages
 * @param <P> The type of connection parameters
 * @param <T> The type of clients
 * @param <X> the type of exceptions
 */

public interface HBClientFactoryType<
  C extends HBConfigurationType,
  M extends HBMessageType,
  P extends HBConnectionParametersType,
  T extends HBClientType<M, P, X>,
  X extends Exception>
{
  /**
   * Create a new client.
   *
   * @param configuration The client configuration
   *
   * @return The new client
   *
   * @throws X On errors
   */

  T create(
    C configuration)
    throws X;
}
