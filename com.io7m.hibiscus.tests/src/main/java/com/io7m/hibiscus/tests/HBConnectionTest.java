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


package com.io7m.hibiscus.tests;

import com.io7m.hibiscus.api.HBConnection;
import com.io7m.hibiscus.api.HBConnectionReceiveQueueOverflowException;
import com.io7m.hibiscus.api.HBMessageType;
import com.io7m.hibiscus.api.HBTransportType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class HBConnectionTest
{
  private static interface MessageType
    extends HBMessageType
  {

  }

  private record MessageNumber(
    int value)
    implements MessageType
  {
    @Override
    public boolean isResponseFor(
      final HBMessageType message)
    {
      return false;
    }

  }

  private record MessageResponse(
    MessageType source)
    implements MessageType
  {
    @Override
    public boolean isResponseFor(
      final HBMessageType message)
    {
      return Objects.equals(this.source, message);
    }

  }

  private record MessageError(
    MessageType source)
    implements MessageType
  {
    @Override
    public boolean isResponseFor(
      final HBMessageType message)
    {
      return Objects.equals(this.source, message);
    }

  }

  private static final class MockTransport
    implements HBTransportType<MessageType, Exception>
  {
    private final AtomicBoolean closed;
    private final LinkedBlockingQueue<MessageType> write;
    private final LinkedBlockingQueue<MessageType> toRead;

    MockTransport()
    {
      this.closed =
        new AtomicBoolean(false);
      this.write =
        new LinkedBlockingQueue<>();
      this.toRead =
        new LinkedBlockingQueue<>();
    }

    @Override
    public boolean isClosed()
    {
      return this.closed.get();
    }

    @Override
    public void close()
    {
      this.closed.set(true);
    }

    @Override
    public Optional<MessageType> read(
      final Duration timeout)
    {
      return Optional.ofNullable(this.toRead.poll());
    }

    @Override
    public void write(
      final MessageType message)
    {
      this.write.add(message);
    }
  }

  /**
   * Closing connections works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionClose()
    throws Exception
  {
    final var transport =
      new MockTransport();

    final var connection = new HBConnection<>(
      Clock.systemUTC(),
      transport,
      1000);
    assertFalse(connection.isClosed());
    connection.close();
    assertTrue(connection.isClosed());
  }

  /**
   * Sending messages works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionSend()
    throws Exception
  {
    final var transport =
      new MockTransport();

    final var connection = new HBConnection<>(
      Clock.systemUTC(),
      transport,
      1000);
    connection.send(new MessageNumber(23));

    assertEquals(new MessageNumber(23), transport.write.poll());
    assertEquals(null, transport.write.poll());
  }

  /**
   * Asking works if the server responds with the right message.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionAskOK()
    throws Exception
  {
    final var transport =
      new MockTransport();

    transport.toRead.add(new MessageResponse(new MessageNumber(23)));

    final var connection =
      new HBConnection<>(Clock.systemUTC(), transport, 1000);
    final var r =
      connection.ask(new MessageNumber(23), Duration.ofSeconds(1L));

    assertEquals(
      new MessageNumber(23),
      transport.write.poll()
    );
    assertEquals(
      new MessageResponse(new MessageNumber(23)),
      r
    );
  }

  /**
   * The connection times out if the server does not respond in time.
   */

  @Test
  public void testConnectionAskTimesOut()
  {
    final var transport =
      new MockTransport();

    final var connection =
      new HBConnection<>(Clock.systemUTC(), transport, 1000);

    assertThrows(TimeoutException.class, () -> {
      connection.ask(new MessageNumber(23), Duration.ofSeconds(1L));
    });
  }

  /**
   * The connection queue can overflow.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionOverflow()
    throws Exception
  {
    final var transport =
      new MockTransport();

    final var connection =
      new HBConnection<>(
        Clock.fixed(Instant.now(), ZoneId.of("UTC")),
        transport,
        10
      );

    for (int index = 0; index < 11; ++index) {
      transport.toRead.add(new MessageResponse(new MessageNumber(1)));
    }

    assertThrows(HBConnectionReceiveQueueOverflowException.class, () -> {
      connection.ask(new MessageNumber(23), Duration.ofSeconds(1L));
    });
  }

  /**
   * The connection can receive messages.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionReceiveOK()
    throws Exception
  {
    final var transport =
      new MockTransport();

    transport.toRead.add(new MessageNumber(1));

    final var connection =
      new HBConnection<>(Clock.systemUTC(), transport, 1000);

    final var r =
      connection.receive(Duration.ofSeconds(1L));

    assertEquals(
      Optional.of(new MessageNumber(1)),
      r
    );
  }

  /**
   * The connection can receive messages that were received while an ask()
   * was in progress.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionReceiveAsk()
    throws Exception
  {
    final var transport =
      new MockTransport();

    transport.toRead.add(new MessageNumber(1));
    transport.toRead.add(new MessageResponse(new MessageNumber(2)));

    final var connection =
      new HBConnection<>(Clock.systemUTC(), transport, 1000);

    final var r =
      connection.ask(new MessageNumber(2), Duration.ofSeconds(1L));

    assertEquals(
      new MessageResponse(new MessageNumber(2)),
      r
    );

    assertEquals(
      Optional.of(new MessageNumber(1)),
      connection.receive(Duration.ofSeconds(1L))
    );
  }

  /**
   * Receive can be interrupted.
   *
   * @throws Exception On errors
   */

  @Test
  public void testConnectionReceiveInterrupt()
    throws Exception
  {
    final var transport =
      new MockTransport();

    transport.toRead.add(new MessageNumber(1));

    final var connection =
      new HBConnection<>(Clock.systemUTC(), transport, 1000);

    Thread.currentThread().interrupt();

    final var r =
      connection.receive(Duration.ofSeconds(1L));

    assertEquals(
      Optional.of(new MessageNumber(1)),
      r
    );
  }
}
