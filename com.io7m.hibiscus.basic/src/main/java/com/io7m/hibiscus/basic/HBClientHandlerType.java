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

package com.io7m.hibiscus.basic;

import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;
import com.io7m.hibiscus.api.HBResultType;
import org.osgi.annotation.versioning.ProviderType;

import java.util.List;

/**
 * A client handler.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <R>  The type of responses from the server
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

@ProviderType
public interface HBClientHandlerType<
  X extends Exception,
  C extends HBCommandType,
  R extends HBResponseType,
  RS extends R,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
{
  /**
   * @return {@code true} if this handler is currently connected
   */

  boolean onIsConnected();

  /**
   * Poll the server for events.
   *
   * @return The list of events, if any
   *
   * @throws InterruptedException On interruption
   */

  List<E> onPollEvents()
    throws InterruptedException;

  /**
   * Log in synchronously.
   *
   * @param credentials The credentials
   *
   * @return The result
   *
   * @throws InterruptedException On interruption
   */

  HBResultType<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF>
  onExecuteLogin(CR credentials)
    throws InterruptedException;

  /**
   * Execute the given command synchronously.
   *
   * @param command The command
   *
   * @return The result
   *
   * @throws InterruptedException On interruption
   */

  HBResultType<RS, RF> onExecuteCommand(C command)
    throws InterruptedException;

  /**
   * Disconnect the client.
   *
   * @throws InterruptedException On interruption
   */

  void onDisconnect()
    throws InterruptedException;
}
