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
 * The client state.
 */

public enum HBState
{
  /**
   * The client is authenticating with the server.
   */

  CLIENT_EXECUTING_LOGIN,

  /**
   * The client failed to authenticate with the server.
   */

  CLIENT_EXECUTING_LOGIN_FAILED,

  /**
   * The client successfully authenticated with the server.
   */

  CLIENT_EXECUTING_LOGIN_SUCCEEDED,

  /**
   * The client is connected.
   */

  CLIENT_CONNECTED,

  /**
   * The client is disconnected.
   */

  CLIENT_DISCONNECTED,

  /**
   * The client is executing a command.
   */

  CLIENT_EXECUTING_COMMAND,

  /**
   * The client failed to execute a command.
   */

  CLIENT_EXECUTING_COMMAND_FAILED,

  /**
   * The client successfully executed a command.
   */

  CLIENT_EXECUTING_COMMAND_SUCCEEDED,

  /**
   * The client is polling the server for events.
   */

  CLIENT_POLLING_EVENTS,

  /**
   * The client failed to poll the server for events.
   */

  CLIENT_POLLING_EVENTS_FAILED,

  /**
   * The client successfully polled the server for events.
   */

  CLIENT_POLLING_EVENTS_SUCCEEDED,

  /**
   * The client has been instructed to close.
   */

  CLIENT_CLOSING,

  /**
   * The client has been closed.
   */

  CLIENT_CLOSED
}
