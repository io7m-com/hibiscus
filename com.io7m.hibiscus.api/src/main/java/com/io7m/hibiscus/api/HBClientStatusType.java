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

import java.util.concurrent.Flow;

/**
 * Status information for a client.
 *
 * @param <C>  The type of commands sent by the client
 * @param <R>  The type of responses returned from the server
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public interface HBClientStatusType<
  C extends HBCommandType,
  R extends HBResponseType,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
{
  /**
   * @return {@code true} if the client is connected
   */

  boolean isConnected();

  /**
   * @return A stream of events received from the server
   */

  Flow.Publisher<E> events();

  /**
   * @return A stream of state updates for the client
   */

  Flow.Publisher<HBStateType<C, R, RF, CR>> state();

  /**
   * @return The value of {@link #state()} right now
   */

  HBStateType<C, R, RF, CR> stateNow();
}
