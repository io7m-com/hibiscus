/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.hibiscus.examples.tcp0;

import com.io7m.hibiscus.api.HBReadNothing;
import com.io7m.hibiscus.api.HBReadReceived;
import com.io7m.hibiscus.api.HBReadResponse;
import com.io7m.hibiscus.api.HBReadType;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ETCP0Transport
  implements HBTransportType<ETCP0MessageType, ETCP0Exception>
{
  private final CloseableCollectionType<ETCP0Exception> resources;
  private final Socket socket;
  private final DataInputStream input;
  private final DataOutputStream output;
  private final LinkedBlockingQueue<ETCP0MessageType> inbox;
  private final Map<UUID, ETCP0MessageType> sent;
  private final Thread readerThread;
  private final Clock clock;

  ETCP0Transport(
    final Clock inClock,
    final Socket inSocket,
    final InputStream inInputStream,
    final OutputStream inOutputStream)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");

    this.resources =
      CloseableCollection.create(() -> {
        return new ETCP0Exception("Failed to close resources.");
      });

    this.socket =
      this.resources.add(
        Objects.requireNonNull(inSocket, "inCloseable"));
    this.input =
      this.resources.add(
        new DataInputStream(
          Objects.requireNonNull(inInputStream, "inInput")));
    this.output =
      this.resources.add(
        new DataOutputStream(
          Objects.requireNonNull(inOutputStream, "inOutput")));

    this.inbox =
      new LinkedBlockingQueue<>();
    this.sent =
      new HashMap<>();

    this.readerThread =
      Thread.startVirtualThread(this::readLoop);
  }

  private void readLoop()
  {
    while (true) {
      try {
        final var msgLength =
          this.input.readInt();
        final var msgData =
          this.input.readNBytes(msgLength);

        this.inbox.add(ETCP0Messages.fromBytes(msgData));
      } catch (final Throwable e) {
        try {
          this.close();
          return;
        } catch (final Throwable ex) {
          throw new RuntimeException(ex);
        }
      }
    }
  }

  @Override
  public HBReadType<ETCP0MessageType> receive(
    final Duration timeout)
    throws ETCP0Exception, InterruptedException
  {
    Objects.requireNonNull(timeout, "timeout");

    if (this.isClosed()) {
      throw new ETCP0Exception(new ClosedChannelException());
    }

    final var m =
      this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);

    if (m == null) {
      return new HBReadNothing<>();
    }

    if (m instanceof final ETCP0ResponseType rr) {
      final var original =
        this.sent.remove(rr.correlationId());

      if (original != null) {
        return new HBReadResponse<>(original, rr);
      }
    }

    return new HBReadReceived<>(m);
  }

  @Override
  public void send(
    final ETCP0MessageType message)
    throws ETCP0Exception
  {
    this.sendAndForget(message);
    this.sent.put(message.messageId(), message);
  }

  @Override
  public void sendAndForget(
    final ETCP0MessageType message)
    throws ETCP0Exception
  {
    try {
      final var msgBytes = ETCP0Messages.toBytes(message);
      this.output.writeInt(msgBytes.length);
      this.output.write(msgBytes);
      this.output.flush();
    } catch (final Exception e) {
      this.close();
      throw new ETCP0Exception(e);
    }
  }

  @Override
  public ETCP0MessageType sendAndWait(
    final ETCP0MessageType message,
    final Duration timeout)
    throws ETCP0Exception, InterruptedException, TimeoutException
  {
    this.send(message);

    final var slices =
      Math.max(1L, (long) this.inbox.size() * 2L);
    final var timeSlice =
      timeout.dividedBy(slices);

    final var timeLater =
      Instant.now(this.clock)
        .plus(timeout);

    while (true) {
      final var timeNow = Instant.now(this.clock);
      if (timeNow.isAfter(timeLater)) {
        throw new TimeoutException(
          "No response received in %s".formatted(timeout));
      }

      final var r =
        this.inbox.poll(timeSlice.toNanos(), TimeUnit.NANOSECONDS);

      if (r == null) {
        continue;
      }

      if (r instanceof final ETCP0ResponseType response) {
        if (Objects.equals(response.correlationId(), message.messageId())) {
          this.sent.remove(message.messageId());
          return response;
        }
      }

      this.inbox.put(r);
    }
  }

  @Override
  public void close()
    throws ETCP0Exception
  {
    this.resources.close();
  }

  @Override
  public boolean isClosed()
  {
    if (!this.socket.isClosed()) {
      return !this.socket.isConnected();
    }
    return this.socket.isClosed();
  }
}
