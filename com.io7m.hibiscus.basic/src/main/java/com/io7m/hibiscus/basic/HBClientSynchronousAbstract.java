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
 * @param <R>  The type of responses from the server
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public abstract class HBClientSynchronousAbstract<
  X extends Exception,
  C extends HBCommandType,
  R extends HBResponseType,
  RS extends R,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
  implements HBClientSynchronousType<X, C, R, RS, RF, E, CR>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(HBClientSynchronousAbstract.class);

  private final SubmissionPublisher<E> events;
  private final SubmissionPublisher<HBState> state;
  private final AtomicReference<HBState> stateNow;
  private final AtomicBoolean closedExternal;
  private final AtomicBoolean closedInternal;
  private final HBClientHandlerType<X, C, R, RS, RF, E, CR> disconnectedHandler;
  private final HBDirectExecutor executor;
  private HBClientHandlerType<X, C, R, RS, RF, E, CR> handler;

  /**
   * Construct a client.
   *
   * @param inDisconnectedHandler The handler that will be (re)used in the
   *                              disconnected state.
   */

  protected HBClientSynchronousAbstract(
    final HBClientHandlerType<X, C, R, RS, RF, E, CR> inDisconnectedHandler)
  {
    this.disconnectedHandler =
      Objects.requireNonNull(inDisconnectedHandler, "disconnectedHandler");

    this.executor =
      new HBDirectExecutor();
    this.events =
      new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize());
    this.state =
      new SubmissionPublisher<>(this.executor, Flow.defaultBufferSize());
    this.stateNow =
      new AtomicReference<>(CLIENT_DISCONNECTED);
    this.closedExternal =
      new AtomicBoolean(false);
    this.closedInternal =
      new AtomicBoolean(false);
    this.handler =
      this.disconnectedHandler;
  }

  @Override
  public final boolean isClosed()
  {
    return this.closedExternal.get();
  }

  @Override
  public final boolean isConnected()
  {
    return this.handler.onIsConnected();
  }

  @Override
  public final Flow.Publisher<E> events()
  {
    return this.events;
  }

  @Override
  public final Flow.Publisher<HBState> state()
  {
    return this.state;
  }

  @Override
  public final HBState stateNow()
  {
    return this.stateNow.get();
  }

  @Override
  public final void pollEvents()
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
  public final HBResultType<RS, RF> login(
    final CR credentials)
    throws InterruptedException
  {
    Objects.requireNonNull(credentials, "credentials");
    this.checkNotClosed();

    this.disconnect();
    this.publishState(CLIENT_EXECUTING_LOGIN);

    final HBResultType<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> result;
    try {
      result = this.handler.onExecuteLogin(credentials);
    } catch (final Exception e) {
      this.publishState(CLIENT_EXECUTING_LOGIN_FAILED);
      LOG.debug("login failed");
      throw e;
    }

    Objects.requireNonNull(result, "result");

    if (result instanceof final HBResultSuccess<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> success) {
      this.handler = success.result().newHandler();
      this.publishState(CLIENT_EXECUTING_LOGIN_SUCCEEDED);
      this.publishState(CLIENT_CONNECTED);
      LOG.debug("login succeeded");
      return success.map(HBClientNewHandler::loginResponse);
    }
    if (result instanceof final HBResultFailure<HBClientNewHandler<X, C, R, RS, RF, E, CR>, RF> failure) {
      this.publishState(CLIENT_EXECUTING_LOGIN_FAILED);
      LOG.debug("login failed");
      return failure.cast();
    }
    throw new UnreachableCodeException();
  }

  private void checkNotClosed()
  {
    if (this.closedExternal.get()) {
      throw new IllegalStateException("Client is closed!");
    }
  }

  private void publishState(
    final HBState newState)
  {
    if (newState == CLIENT_CLOSED) {
      this.logStateChange(newState);
      this.stateNow.set(newState);
      this.state.submit(newState);
      return;
    }

    if (this.closedExternal.get()) {
      return;
    }

    this.logStateChange(newState);
    this.stateNow.set(newState);
    this.state.submit(newState);
  }

  private void logStateChange(
    final HBState newState)
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("state {} -> {}", this.stateNow.get(), newState);
    }
  }

  @Override
  public final HBResultType<RS, RF> execute(
    final C command)
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

    return result;
  }

  @Override
  public final void disconnect()
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
  public final void close()
  {
    /*
     * We track an "internal" and "external" closed state flag because we
     * want to use the internal flag to prevent multiple close attempts, but
     * we don't want to advertise the "closed" state externally until all
     * resources have actually been closed.
     */

    if (this.closedInternal.compareAndSet(false, true)) {
      try {
        if (LOG.isTraceEnabled()) {
          LOG.trace("close");
        }

        this.publishState(CLIENT_CLOSED);

        try {
          this.state.close();
        } finally {
          this.events.close();
        }
      } finally {
        this.closedExternal.set(true);
      }
    }
  }
}
