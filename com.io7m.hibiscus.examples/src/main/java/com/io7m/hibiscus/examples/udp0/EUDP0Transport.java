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


package com.io7m.hibiscus.examples.udp0;

import com.io7m.hibiscus.api.HBReadNothing;
import com.io7m.hibiscus.api.HBReadType;
import com.io7m.hibiscus.api.HBReadReceived;
import com.io7m.hibiscus.api.HBReadResponse;
import com.io7m.hibiscus.api.HBTransportType;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class EUDP0Transport
  implements HBTransportType<EUDP0MessageType, EUDP0Exception>
{
  private final CloseableCollectionType<EUDP0Exception> resources;
  private final DatagramSocket socket;
  private final LinkedBlockingQueue<EUDP0MessageType> inbox;
  private final Thread readerThread;
  private final Clock clock;
  private final InetSocketAddress remoteAddress;
  private final Map<UUID, EUDP0MessageType> sent;

  EUDP0Transport(
    final Clock inClock,
    final InetSocketAddress inRemoteAddress,
    final DatagramSocket inSocket)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.remoteAddress =
      Objects.requireNonNull(inRemoteAddress, "inRemoteAddress");
    this.resources =
      CloseableCollection.create(() -> {
        return new EUDP0Exception("Failed to close resources.");
      });

    this.socket =
      this.resources.add(
        Objects.requireNonNull(inSocket, "inCloseable"));

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
        final var data =
          new byte[512];
        final var packet =
          new DatagramPacket(data, data.length);

        this.socket.receive(packet);

        final var message =
          EUDP0Messages.fromBytes(
            Arrays.copyOf(packet.getData(), packet.getLength())
          );

        this.inbox.add(message);
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
  public HBReadType<EUDP0MessageType> receive(
    final Duration timeout)
    throws EUDP0Exception, InterruptedException
  {
    Objects.requireNonNull(timeout, "timeout");

    if (this.isClosed()) {
      throw new EUDP0Exception(new ClosedChannelException());
    }

    final var m =
      this.inbox.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);

    if (m == null) {
      return new HBReadNothing<>();
    }

    if (m instanceof final EUDP0ResponseType rr) {
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
    final EUDP0MessageType message)
    throws EUDP0Exception
  {
    this.sendAndForget(message);
    this.sent.put(message.messageId(), message);
  }

  @Override
  public void sendAndForget(
    final EUDP0MessageType message)
    throws EUDP0Exception
  {
    try {
      final var msgBytes =
        EUDP0Messages.toBytes(message);
      final var packet =
        new DatagramPacket(msgBytes, msgBytes.length, this.remoteAddress);

      this.socket.send(packet);
    } catch (final Exception e) {
      this.close();
      throw new EUDP0Exception(e);
    }
  }

  @Override
  public EUDP0MessageType sendAndWait(
    final EUDP0MessageType message,
    final Duration timeout)
    throws EUDP0Exception, InterruptedException, TimeoutException
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

      if (r instanceof final EUDP0ResponseType response) {
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
    throws EUDP0Exception
  {
    this.resources.close();
  }

  @Override
  public boolean isClosed()
  {
    return this.socket.isClosed();
  }
}
