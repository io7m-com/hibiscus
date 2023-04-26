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

import com.io7m.hibiscus.api.HBClientAsynchronousType;
import com.io7m.hibiscus.api.HBClientSynchronousType;
import com.io7m.hibiscus.api.HBCommandType;
import com.io7m.hibiscus.api.HBCredentialsType;
import com.io7m.hibiscus.api.HBEventType;
import com.io7m.hibiscus.api.HBResponseType;
import com.io7m.hibiscus.api.HBResultType;
import com.io7m.hibiscus.api.HBState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The basic asynchronous client.
 *
 * @param <X>  The type of exceptions that can be raised by the client
 * @param <C>  The type of commands sent by the client
 * @param <R>  The type of responses returned from the server
 * @param <RS> The type of responses returned that indicate successful commands
 * @param <RF> The type of responses returned that indicate failed commands
 * @param <CR> The type of credentials
 * @param <E>  The type of events
 */

public abstract class HBClientAsynchronousAbstract<
  X extends Exception,
  C extends HBCommandType,
  R extends HBResponseType,
  RS extends R,
  RF extends R,
  E extends HBEventType,
  CR extends HBCredentialsType>
  implements HBClientAsynchronousType<X, C, R, RS, RF, E, CR>
{
  private static final Logger LOG =
    LoggerFactory.getLogger(HBClientAsynchronousAbstract.class);

  private final AtomicBoolean closed;
  private final HBClientSynchronousType<X, C, R, RS, RF, E, CR> delegate;
  private final String commandThreadNamePrefix;
  private final ExecutorService commandExecutor;
  private final LinkedBlockingQueue<OpType<X, C, R, RS, RF, E, CR>> operationQueue;

  /**
   * Construct an asynchronous client.
   *
   * @param inDelegate                The synchronous client to which operations
   *                                  will be delegated.
   * @param inCommandThreadNamePrefix The command thread name prefix
   */

  protected HBClientAsynchronousAbstract(
    final HBClientSynchronousType<X, C, R, RS, RF, E, CR> inDelegate,
    final String inCommandThreadNamePrefix)
  {
    this.delegate =
      Objects.requireNonNull(inDelegate, "delegate");
    this.commandThreadNamePrefix =
      Objects.requireNonNull(
        inCommandThreadNamePrefix,
        "commandThreadNamePrefix");
    this.closed =
      new AtomicBoolean(false);

    this.operationQueue =
      new LinkedBlockingQueue<>();
    this.commandExecutor =
      Executors.newSingleThreadExecutor(r -> {
        final var th =
          new Thread(r);
        final var threadName =
          "%s[%d]".formatted(
            this.commandThreadNamePrefix,
            Long.valueOf(th.getId())
          );
        th.setName(threadName);
        th.setDaemon(true);
        return th;
      });

    this.commandExecutor.execute(this::runCommandProcessing);
  }

  @Override
  public final boolean isClosed()
  {
    return this.delegate.isClosed();
  }

  private void runCommandProcessing()
  {
    while (!this.closed.get()) {
      OpType<X, C, R, RS, RF, E, CR> op = null;
      try {
        op = this.operationQueue.poll(100L, TimeUnit.MILLISECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      if (op == null) {
        continue;
      }

      if (op instanceof final OpType.Login<X, C, R, RS, RF, E, CR> login) {
        this.executeLogin(login);
      } else if (op instanceof final OpType.Command<X, C, R, RS, RF, E, CR> command) {
        this.executeCommand(command);
      } else if (op instanceof final OpType.Disconnect<X, C, R, RS, RF, E, CR> disconnect) {
        this.executeDisconnect(disconnect);
      } else if (op instanceof final OpType.Close<X, C, R, RS, RF, E, CR> close) {
        this.executeClose(close);
      } else if (op instanceof final OpType.PollEvents<X, C, R, RS, RF, E, CR> poll) {
        this.executePollEvents(poll);
      }
    }
  }

  private void executeClose(
    final OpType.Close<X, C, R, RS, RF, E, CR> close)
  {
    /*
     * Cancel all pending operations in the queue. The guard on the "closed"
     * atomic boolean prevents new operations from being submitted after a
     * close request has been submitted, so there should be no risk of
     * operations submitted after closing hanging forever.
     */

    try {
      this.operationQueue.forEach(op -> {
        if (!Objects.equals(close.future, op.future())) {
          op.future().cancel(true);
        }
      });
      try {
        this.delegate.close();
      } catch (final Exception e) {
        LOG.debug("close: ", e);
      } finally {
        close.future.complete(null);
      }
    } finally {
      this.commandExecutor.shutdown();
    }
  }

  private void executeDisconnect(
    final OpType.Disconnect<X, C, R, RS, RF, E, CR> disconnect)
  {
    try {
      this.delegate.disconnect();
      disconnect.future.complete(null);
    } catch (final Throwable e) {
      disconnect.future.completeExceptionally(e);
    }
  }

  private <RS1 extends RS> void executeCommand(
    final OpType.Command<X, C, R, RS, RF, E, CR> command)
  {
    try {
      final var c = command.command;
      final var r = this.delegate.<C, RS1>execute(c);
      command.future.complete(r);
    } catch (final Throwable e) {
      command.future.completeExceptionally(e);
    }
  }

  private void executeLogin(
    final OpType.Login<X, C, R, RS, RF, E, CR> login)
  {
    try {
      final var c = login.credentials;
      final var r = this.delegate.login(c);
      login.future.complete(r);
    } catch (final Throwable e) {
      login.future.completeExceptionally(e);
    }
  }

  private void executePollEvents(
    final OpType.PollEvents<X, C, R, RS, RF, E, CR> poll)
  {
    try {
      this.delegate.pollEvents();
      poll.future.complete(null);
    } catch (final Throwable e) {
      poll.future.completeExceptionally(e);
    }
  }

  @Override
  public final boolean isConnected()
  {
    return this.delegate.isConnected();
  }

  @Override
  public final Flow.Publisher<E> events()
  {
    return this.delegate.events();
  }

  @Override
  public final Flow.Publisher<HBState> state()
  {
    return this.delegate.state();
  }

  @Override
  public final HBState stateNow()
  {
    return this.delegate.stateNow();
  }

  @Override
  public final CompletableFuture<Void> pollEvents()
  {
    this.checkNotClosed();

    final var future = new CompletableFuture<Void>();
    this.operationQueue.add(new OpType.PollEvents<>(future));
    return future;
  }

  @Override
  public final CompletableFuture<HBResultType<RS, RF>>
  loginAsync(
    final CR credentials)
  {
    Objects.requireNonNull(credentials, "credentials");
    this.checkNotClosed();

    final var future = new CompletableFuture<HBResultType<RS, RF>>();
    this.operationQueue.add(new OpType.Login<>(credentials, future));
    return future;
  }

  @Override
  public final CompletableFuture<HBResultType<RS, RF>>
  executeAsync(
    final C command)
  {
    Objects.requireNonNull(command, "command");
    this.checkNotClosed();

    final var future = new CompletableFuture<HBResultType<RS, RF>>();
    this.operationQueue.add(new OpType.Command<>(command, future));
    return future;
  }

  @Override
  public final CompletableFuture<Void> disconnectAsync()
  {
    this.checkNotClosed();

    final var future = new CompletableFuture<Void>();
    this.operationQueue.add(new OpType.Disconnect<>(future));
    return future;
  }

  @Override
  public final void close()
  {
    if (this.closed.compareAndSet(false, true)) {
      final var future =
        CompletableFuture.completedFuture(null);
      this.operationQueue.add(new OpType.Close<>(future));
      try {
        future.get();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (final ExecutionException e) {
        // Nothing we can do about it.
      }
    }
  }

  private void checkNotClosed()
  {
    if (this.closed.get()) {
      throw new IllegalStateException("Client is closed!");
    }
  }

  private sealed interface OpType
    <X extends Exception,
      C extends HBCommandType,
      R extends HBResponseType,
      RS extends R,
      RF extends R,
      E extends HBEventType,
      CR extends HBCredentialsType>
  {
    CompletableFuture<?> future();

    record Command<
      X extends Exception,
      C extends HBCommandType,
      R extends HBResponseType,
      RS extends R,
      RF extends R,
      E extends HBEventType,
      CR extends HBCredentialsType>(
      C command,
      CompletableFuture<HBResultType<RS, RF>> future)
      implements HBClientAsynchronousAbstract.OpType<X, C, R, RS, RF, E, CR>
    {

    }

    record Disconnect<
      X extends Exception,
      C extends HBCommandType,
      R extends HBResponseType,
      RS extends R,
      RF extends R,
      E extends HBEventType,
      CR extends HBCredentialsType>(
      CompletableFuture<Void> future)
      implements HBClientAsynchronousAbstract.OpType<X, C, R, RS, RF, E, CR>
    {

    }

    record Login<
      X extends Exception,
      C extends HBCommandType,
      R extends HBResponseType,
      RS extends R,
      RF extends R,
      E extends HBEventType,
      CR extends HBCredentialsType>(
      CR credentials,
      CompletableFuture<HBResultType<RS, RF>> future)
      implements HBClientAsynchronousAbstract.OpType<X, C, R, RS, RF, E, CR>
    {

    }

    record Close<
      X extends Exception,
      C extends HBCommandType,
      R extends HBResponseType,
      RS extends R,
      RF extends R,
      E extends HBEventType,
      CR extends HBCredentialsType>(
      CompletableFuture<Object> future)
      implements HBClientAsynchronousAbstract.OpType<X, C, R, RS, RF, E, CR>
    {

    }

    record PollEvents<
      X extends Exception,
      C extends HBCommandType,
      R extends HBResponseType,
      RS extends R,
      RF extends R,
      E extends HBEventType,
      CR extends HBCredentialsType>(
      CompletableFuture<Void> future)
      implements HBClientAsynchronousAbstract.OpType<X, C, R, RS, RF, E, CR>
    {

    }
  }
}
