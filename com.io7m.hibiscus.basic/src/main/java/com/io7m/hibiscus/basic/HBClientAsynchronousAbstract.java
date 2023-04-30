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
import com.io7m.hibiscus.api.HBStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
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

  private final AtomicBoolean closing;
  private final HBClientSynchronousType<X, C, R, RS, RF, E, CR> delegate;
  private final String commandThreadNamePrefix;
  private final ExecutorService commandExecutor;

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
    this.closing =
      new AtomicBoolean(false);

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
  }

  @Override
  public final boolean isClosed()
  {
    return this.delegate.isClosed();
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
  public final Flow.Publisher<HBStateType<C, R, RF, CR>> state()
  {
    return this.delegate.state();
  }

  @Override
  public final HBStateType<C, R, RF, CR> stateNow()
  {
    return this.delegate.stateNow();
  }

  @Override
  public final CompletableFuture<Void> pollEvents()
  {
    this.checkNotClosingOrClosed();

    final var future = new CompletableFuture<Void>();
    this.commandExecutor.execute(() -> {
      try {
        this.delegate.pollEvents();
        future.complete(null);
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public final CompletableFuture<HBResultType<RS, RF>>
  loginAsync(
    final CR credentials)
  {
    Objects.requireNonNull(credentials, "credentials");
    this.checkNotClosingOrClosed();

    final var future = new CompletableFuture<HBResultType<RS, RF>>();
    this.commandExecutor.execute(() -> {
      try {
        future.complete(this.delegate.login(credentials));
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public final CompletableFuture<HBResultType<RS, RF>>
  executeAsync(
    final C command)
  {
    Objects.requireNonNull(command, "command");
    this.checkNotClosingOrClosed();

    final var future = new CompletableFuture<HBResultType<RS, RF>>();
    this.commandExecutor.execute(() -> {
      try {
        future.complete(this.delegate.execute(command));
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public final CompletableFuture<Void> disconnectAsync()
  {
    this.checkNotClosingOrClosed();

    final var future = new CompletableFuture<Void>();
    this.commandExecutor.execute(() -> {
      try {
        this.delegate.disconnect();
        future.complete(null);
      } catch (final Throwable e) {
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  @Override
  public final void close()
  {
    LOG.trace("close requested");

    if (this.closing.compareAndSet(false, true)) {
      LOG.trace("close scheduled");

      final var future = new CompletableFuture<Void>();
      this.commandExecutor.execute(() -> {
        try {
          this.delegate.close();
        } catch (final Throwable e) {
          LOG.debug("close op: ", e);
        }

        try {
          this.commandExecutor.shutdown();
        } catch (final Throwable e) {
          LOG.debug("close op: ", e);
        }

        try {
          future.complete(null);
        } catch (final Throwable e) {
          LOG.debug("close op: ", e);
        }
      });

      try {
        future.get();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (final ExecutionException e) {
        // Nothing we can do about it.
      } finally {
        LOG.trace("close future completed");
      }
    }
  }

  protected final HBClientSynchronousType<X, C, R, RS, RF, E, CR> delegate()
  {
    return this.delegate;
  }

  protected final ExecutorService commandExecutor()
  {
    return this.commandExecutor;
  }

  protected final void checkNotClosingOrClosed()
  {
    if (this.closing.get()) {
      throw new IllegalStateException("Client is closed!");
    }
  }
}
