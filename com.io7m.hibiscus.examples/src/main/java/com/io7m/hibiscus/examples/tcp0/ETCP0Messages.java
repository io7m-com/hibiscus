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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class ETCP0Messages
{
  private static final int MESSAGE_ID_LOGIN = 0x0;
  private static final int MESSAGE_ID_HELLO = 0x1;
  private static final int MESSAGE_ID_RESPONSE_FAILURE = 0x3;
  private static final int MESSAGE_ID_RESPONSE_OK = 0x2;

  private ETCP0Messages()
  {

  }

  public static byte[] toBytes(
    final ETCP0MessageType message)
    throws ETCP0Exception
  {
    try {
      return switch (message) {
        case final ETCP0CommandType m -> toBytesCommand(m);
        case final ETCP0ResponseType m -> toBytesResponse(m);
      };
    } catch (final IOException e) {
      throw new ETCP0Exception(e);
    }
  }

  private static byte[] toBytesResponse(
    final ETCP0ResponseType r)
    throws IOException
  {
    return switch (r) {
      case final ETCP0ResponseFailure m -> toBytesResponseFailure(m);
      case final ETCP0ResponseOK m -> toBytesResponseOK(m);
    };
  }

  private static byte[] toBytesResponseOK(
    final ETCP0ResponseOK m)
    throws IOException
  {
    try (var byteArray = new ByteArrayOutputStream()) {
      try (var out = new DataOutputStream(byteArray)) {
        out.writeByte(MESSAGE_ID_RESPONSE_OK);

        out.writeLong(m.messageId().getMostSignificantBits());
        out.writeLong(m.messageId().getLeastSignificantBits());

        out.writeLong(m.correlationId().getMostSignificantBits());
        out.writeLong(m.correlationId().getLeastSignificantBits());

        out.flush();
        return byteArray.toByteArray();
      }
    }
  }

  private static byte[] toBytesResponseFailure(
    final ETCP0ResponseFailure m)
    throws IOException
  {
    try (var byteArray = new ByteArrayOutputStream()) {
      try (var out = new DataOutputStream(byteArray)) {
        out.writeByte(MESSAGE_ID_RESPONSE_FAILURE);

        out.writeLong(m.messageId().getMostSignificantBits());
        out.writeLong(m.messageId().getLeastSignificantBits());

        out.writeLong(m.correlationId().getMostSignificantBits());
        out.writeLong(m.correlationId().getLeastSignificantBits());

        final var msgBytes = m.message().getBytes(UTF_8);
        out.writeInt(msgBytes.length);
        out.write(msgBytes);

        out.flush();
        return byteArray.toByteArray();
      }
    }
  }

  private static byte[] toBytesCommand(
    final ETCP0CommandType c)
    throws IOException
  {
    return switch (c) {
      case final ETCP0CommandHello m -> toBytesCommandHello(m);
      case final ETCP0CommandLogin m -> toBytesCommandLogin(m);
    };
  }

  private static byte[] toBytesCommandLogin(
    final ETCP0CommandLogin m)
    throws IOException
  {
    try (var byteArray = new ByteArrayOutputStream()) {
      try (var out = new DataOutputStream(byteArray)) {
        out.writeByte(MESSAGE_ID_LOGIN);

        out.writeLong(m.messageId().getMostSignificantBits());
        out.writeLong(m.messageId().getLeastSignificantBits());

        final var userBytes = m.user().getBytes(UTF_8);
        out.writeInt(userBytes.length);
        out.write(userBytes);

        final var passBytes = m.password().getBytes(UTF_8);
        out.writeInt(passBytes.length);
        out.write(passBytes);

        out.flush();
        return byteArray.toByteArray();
      }
    }
  }

  private static byte[] toBytesCommandHello(
    final ETCP0CommandHello m)
    throws IOException
  {
    try (var byteArray = new ByteArrayOutputStream()) {
      try (var out = new DataOutputStream(byteArray)) {
        out.writeByte(MESSAGE_ID_HELLO);

        out.writeLong(m.messageId().getMostSignificantBits());
        out.writeLong(m.messageId().getLeastSignificantBits());

        final var msgBytes = m.message().getBytes(UTF_8);
        out.writeInt(msgBytes.length);
        out.write(msgBytes);

        out.flush();
        return byteArray.toByteArray();
      }
    }
  }

  public static ETCP0MessageType fromBytes(
    final byte[] data)
    throws ETCP0Exception
  {
    try {
      return switch (data[0]) {
        case MESSAGE_ID_LOGIN -> {
          yield fromBytesLogin(data);
        }
        case MESSAGE_ID_HELLO -> {
          yield fromBytesHello(data);
        }
        case MESSAGE_ID_RESPONSE_FAILURE -> {
          yield fromBytesResponseFailure(data);
        }
        case MESSAGE_ID_RESPONSE_OK -> {
          yield fromBytesResponseOK(data);
        }
        default -> {
          throw new ETCP0Exception("Unrecognized message ID: " + data[0]);
        }
      };
    } catch (final IOException e) {
      throw new ETCP0Exception(e);
    }
  }

  private static ETCP0MessageType fromBytesResponseOK(
    final byte[] data)
    throws IOException
  {
    try (var dataInput = new DataInputStream(new ByteArrayInputStream(data))) {
      dataInput.readByte();

      final var msgIdH = dataInput.readLong();
      final var msgIdL = dataInput.readLong();
      final var corIdH = dataInput.readLong();
      final var corIdL = dataInput.readLong();

      return new ETCP0ResponseOK(
        new UUID(msgIdH, msgIdL),
        new UUID(corIdH, corIdL)
      );
    }
  }

  private static ETCP0MessageType fromBytesResponseFailure(
    final byte[] data)
    throws IOException
  {
    try (var dataInput = new DataInputStream(new ByteArrayInputStream(data))) {
      dataInput.readByte();

      final var msgIdH = dataInput.readLong();
      final var msgIdL = dataInput.readLong();
      final var corIdH = dataInput.readLong();
      final var corIdL = dataInput.readLong();

      final var msgLen = dataInput.readInt();
      final var msgData = dataInput.readNBytes(msgLen);
      final var msg = new String(msgData, UTF_8);

      return new ETCP0ResponseFailure(
        new UUID(msgIdH, msgIdL),
        new UUID(corIdH, corIdL),
        msg
      );
    }
  }

  private static ETCP0MessageType fromBytesHello(
    final byte[] data)
    throws IOException
  {
    try (var dataInput = new DataInputStream(new ByteArrayInputStream(data))) {
      dataInput.readByte();

      final var msgIdH = dataInput.readLong();
      final var msgIdL = dataInput.readLong();

      final var msgLen = dataInput.readInt();
      final var msgData = dataInput.readNBytes(msgLen);
      final var msg = new String(msgData, UTF_8);

      return new ETCP0CommandHello(
        new UUID(msgIdH, msgIdL),
        msg
      );
    }
  }

  private static ETCP0MessageType fromBytesLogin(
    final byte[] data)
    throws IOException
  {
    try (var dataInput = new DataInputStream(new ByteArrayInputStream(data))) {
      dataInput.readByte();

      final var msgIdH = dataInput.readLong();
      final var msgIdL = dataInput.readLong();

      final var userLen = dataInput.readInt();
      final var userData = dataInput.readNBytes(userLen);
      final var user = new String(userData, UTF_8);

      final var passLen = dataInput.readInt();
      final var passData = dataInput.readNBytes(passLen);
      final var pass = new String(passData, UTF_8);

      return new ETCP0CommandLogin(
        new UUID(msgIdH, msgIdL),
        user,
        pass
      );
    }
  }
}
