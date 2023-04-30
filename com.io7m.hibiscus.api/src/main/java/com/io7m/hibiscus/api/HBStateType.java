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

import java.util.Objects;

/**
 * The client state.
 *
 * @param <C>  The type of commands
 * @param <R>  The type of responses
 * @param <RF> The type of error responses
 * @param <CR> The type of credentials
 */

public sealed interface HBStateType<
  C extends HBCommandType,
  R extends HBResponseType,
  RF extends R,
  CR extends HBCredentialsType>
{
  /**
   * @return {@code true} if this state implies the client is either closing or
   * has closed
   */

  boolean isClosingOrClosed();

  /**
   * The client is connected.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStateConnected<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "CONNECTED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client is disconnected.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStateDisconnected<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "DISCONNECTED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client is executing a command.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   * @param command The command
   */

  record HBStateExecutingCommand<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>(
    C command)
    implements HBStateType<C, R, RF, CR>
  {
    /**
     * The client is executing a command.
     */

    public HBStateExecutingCommand
    {
      Objects.requireNonNull(command, "command");
    }

    @Override
    public String toString()
    {
      return "EXECUTING_COMMAND";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client successfully executed a command.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   * @param command  The command
   * @param response The response
   */

  record HBStateExecutingCommandSucceeded<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>(
    C command,
    R response)
    implements HBStateType<C, R, RF, CR>
  {
    /**
     * The client successfully executed a command.
     */

    public HBStateExecutingCommandSucceeded
    {
      Objects.requireNonNull(command, "command");
      Objects.requireNonNull(response, "response");
    }

    @Override
    public String toString()
    {
      return "EXECUTING_COMMAND_SUCCEEDED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client failed to execute a command.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   * @param command  The command
   * @param response The response
   */

  record HBStateExecutingCommandFailed<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>(
    C command,
    RF response)
    implements HBStateType<C, R, RF, CR>
  {
    /**
     * The client failed to execute a command.
     */

    public HBStateExecutingCommandFailed
    {
      Objects.requireNonNull(command, "command");
      Objects.requireNonNull(response, "response");
    }

    @Override
    public String toString()
    {
      return "EXECUTING_COMMAND_FAILED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client is authenticating with the server.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   * @param credentials The credentials
   */

  record HBStateExecutingLogin<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>(
    CR credentials)
    implements HBStateType<C, R, RF, CR>
  {
    /**
     * The client is authenticating with the server.
     */

    public HBStateExecutingLogin
    {
      Objects.requireNonNull(credentials, "credentials");
    }

    @Override
    public String toString()
    {
      return "EXECUTING_LOGIN";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client successfully authenticated with the server.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   * @param response The response
   */

  record HBStateExecutingLoginSucceeded<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>(
    R response)
    implements HBStateType<C, R, RF, CR>
  {
    /**
     * The client successfully authenticated with the server.
     */

    public HBStateExecutingLoginSucceeded
    {
      Objects.requireNonNull(response, "response");
    }

    @Override
    public String toString()
    {
      return "EXECUTING_LOGIN_SUCCEEDED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client failed to authenticate with the server.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   * @param response The response
   */

  record HBStateExecutingLoginFailed<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>(
    RF response)
    implements HBStateType<C, R, RF, CR>
  {
    /**
     * The client failed to authenticate with the server.
     */

    public HBStateExecutingLoginFailed
    {
      Objects.requireNonNull(response, "response");
    }

    @Override
    public String toString()
    {
      return "EXECUTING_LOGIN_FAILED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client is polling the server for events.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStatePollingEvents<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "POLLING_EVENTS";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client successfully polled the server for events.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStatePollingEventsSucceeded<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "POLLING_EVENTS_SUCCEEDED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client failed to poll the server for events.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStatePollingEventsFailed<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "POLLING_EVENTS_FAILED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return false;
    }
  }

  /**
   * The client has been instructed to close.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStateClosing<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "CLOSING";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return true;
    }
  }

  /**
   * The client has been closed.
   *
   * @param <C>  The type of commands
   * @param <R>  The type of responses
   * @param <RF> The type of error responses
   * @param <CR> The type of credentials
   */

  record HBStateClosed<
    C extends HBCommandType,
    R extends HBResponseType,
    RF extends R,
    CR extends HBCredentialsType>()
    implements HBStateType<C, R, RF, CR>
  {
    @Override
    public String toString()
    {
      return "CLOSED";
    }

    @Override
    public boolean isClosingOrClosed()
    {
      return true;
    }
  }
}
