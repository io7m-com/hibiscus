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

import com.io7m.hibiscus.api.HBClientSynchronousType;
import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;
import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.api.HBState;
import com.io7m.junreachable.UnreachableCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.io7m.hibiscus.api.HBState.CLIENT_CLOSED;
import static com.io7m.hibiscus.api.HBState.CLIENT_CONNECTED;
import static com.io7m.hibiscus.api.HBState.CLIENT_DISCONNECTED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_COMMAND;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_COMMAND_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_COMMAND_SUCCEEDED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_LOGIN;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_LOGIN_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_EXECUTING_LOGIN_SUCCEEDED;
import static com.io7m.hibiscus.api.HBState.CLIENT_POLLING_EVENTS;
import static com.io7m.hibiscus.api.HBState.CLIENT_POLLING_EVENTS_FAILED;
import static com.io7m.hibiscus.api.HBState.CLIENT_POLLING_EVENTS_SUCCEEDED;

/**
 * The basic synchronous client.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public final class HClientSynchronous<
  X extends Exception,
  C extends HBCommandType,
  RS extends HBResponseType,
  RF extends HBResponseType,
  E extends HBEventType,
  CR extends HBCredentialsType>
  implements HBClientSynchronousType<X, C, RS, RF, E, CR>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(HClientSynchronous.class);

  private final SubmissionPublisher<E> events;
  private final SubmissionPublisher<HBState> state;
  private final AtomicReference<HBState> stateNow;
  private final AtomicBoolean closed;
  private final HClientHandlerType<X, C, RS, RF, E, CR> disconnectedHandler;
  private HClientHandlerType<X, C, RS, RF, E, CR> handler;

  /**
   * Construct a client.
   *
   * @param inDisconnectedHandler The handler that will be (re)used in the
   *                              disconnected state.
   */

  public HClientSynchronous(
    final HClientHandlerType<X, C, RS, RF, E, CR> inDisconnectedHandler)
  {
    this.disconnectedHandler =
      Objects.requireNonNull(inDisconnectedHandler, "disconnectedHandler");
    this.events =
      new SubmissionPublisher<>();
    this.state =
      new SubmissionPublisher<>();
    this.stateNow =
      new AtomicReference<>(HBState.CLIENT_DISCONNECTED);
    this.closed =
      new AtomicBoolean(false);
    this.handler =
      this.disconnectedHandler;
  }

  @Override
  public boolean isConnected()
  {
    return this.handler.onIsConnected();
  }

  @Override
  public Flow.Publisher<E> events()
  {
    return this.events;
  }

  @Override
  public Flow.Publisher<HBState> state()
  {
    return this.state;
  }

  @Override
  public HBState stateNow()
  {
    return this.stateNow.get();
  }

  @Override
  public void pollEvents()
    throws InterruptedException
  {
    this.checkNotClosed();

    this.publishState(CLIENT_POLLING_EVENTS);

    try {
      this.handler.onPollEvents().forEach(this.events::submit);
      LOG.debug("polling events succeeded");
      this.publishState(CLIENT_POLLING_EVENTS_SUCCEEDED);
    } catch (final Exception e) {
      this.publishState(CLIENT_POLLING_EVENTS_FAILED);
      LOG.debug("polling events failed");
      throw e;
    }
  }

  @Override
  public <RS1 extends RS> HBResultType<RS1, RF> login(
    final CR credentials)
    throws InterruptedException
  {
    Objects.requireNonNull(credentials, "credentials");
    this.checkNotClosed();

    this.disconnect();
    this.publishState(CLIENT_EXECUTING_LOGIN);

    final HBResultType<HClientNewHandler<X, C, RS, RF, E, CR>, RF> result;
    try {
      result = this.handler.onExecuteLogin(credentials);
    } catch (final Exception e) {
      this.publishState(CLIENT_EXECUTING_LOGIN_FAILED);
      LOG.debug("login failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result instanceof final HBResultSuccess<HClientNewHandler<X, C, RS, RF, E, CR>, RF> success) {
      this.handler = success.result().newHandler();
      this.publishState(CLIENT_EXECUTING_LOGIN_SUCCEEDED);
      this.publishState(CLIENT_CONNECTED);
      LOG.debug("login succeeded");
      return success.map(h -> (RS1) h.loginResponse());
    }
    if (result instanceof final HBResultFailure<HClientNewHandler<X, C, RS, RF, E, CR>, RF> failure) {
      this.publishState(CLIENT_EXECUTING_LOGIN_FAILED);
      LOG.debug("login failed");
      return failure.cast();
    }
    throw new UnreachableCodeException();
  }

  private void checkNotClosed()
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Client is closed!");
    }
  }

  private void publishState(
    final HBState newState)
  {
    this.stateNow.set(newState);
    this.state.submit(newState);
  }

  @Override
  public <C1 extends C, RS1 extends RS> HBResultType<RS1, RF> execute(
    final C1 command)
    throws InterruptedException
  {
    Objects.requireNonNull(command, "command");
    this.checkNotClosed();

    this.publishState(CLIENT_EXECUTING_COMMAND);

    final HBResultType<RS, RF> result;
    try {
      result = this.handler.onExecuteCommand(command);
    } catch (final Exception e) {
      this.publishState(CLIENT_EXECUTING_COMMAND_FAILED);
      LOG.debug("command failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result.isSuccess()) {
      this.publishState(CLIENT_EXECUTING_COMMAND_SUCCEEDED);
      LOG.debug("command succeeded");
    } else {
      this.publishState(CLIENT_EXECUTING_COMMAND_FAILED);
      LOG.debug("command failed");
    }

    return (HBResultType<RS1, RF>) result;
  }

  @Override
  public void disconnect()
    throws InterruptedException
  {
    this.checkNotClosed();

    if (this.handler.onIsConnected()) {
      try {
        this.handler.onDisconnect();
      } finally {
        this.handler = this.disconnectedHandler;
        this.publishState(CLIENT_DISCONNECTED);
      }
    }
  }

  @Override
  public void close()
  {
    if (this.closed.compareAndSet(false, true)) {
      this.publishState(CLIENT_CLOSED);

      try {
        this.state.close();
      } finally {
        this.events.close();
      }
    }
  }
}
