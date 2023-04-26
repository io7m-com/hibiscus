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

package com.io7m.hibiscus.tests;

import com.io7m.hibiscus.api.HBResultFailure;
import com.io7m.hibiscus.api.HBResultSuccess;
import com.io7m.hibiscus.api.HBResultType;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class HClientResultTest
{
  @Provide("results")
  private static Arbitrary<HBResultType<Integer, Integer>> results()
  {
    final var i0 =
      Arbitraries.integers();

    return i0.map(x -> {
      if (x.intValue() % 2 == 0) {
        return new HBResultSuccess<>(x);
      } else {
        return new HBResultFailure<>(x);
      }
    });
  }

  @Provide("functions")
  private static Arbitrary<Function<Integer, HBResultType<Integer, Integer>>> functions()
  {
    final var i0 =
      Arbitraries.integers();

    return i0.map(x -> {
      if (x.intValue() % 2 == 0) {
        return HBResultSuccess::new;
      } else {
        return HBResultFailure::new;
      }
    });
  }

  @Property
  public void testLeftIdentity(
    final @ForAll Integer a,
    final @ForAll("functions") Function<Integer, HBResultType<Integer, Integer>> f)
  {
    assertEquals(
      new HBResultSuccess<Integer, Integer>(a).flatMap(f),
      f.apply(a)
    );
  }

  @Property
  public void testRightIdentity(
    final @ForAll("results") HBResultType<Integer, Integer> a,
    final @ForAll("functions") Function<Integer, HBResultType<Integer, Integer>> f)
  {
    assertEquals(
      a.flatMap(HBResultSuccess::new),
      a
    );
  }

  @Property
  public void testAssociativity(
    final @ForAll("results") HBResultType<Integer, Integer> a,
    final @ForAll("functions") Function<Integer, HBResultType<Integer, Integer>> f,
    final @ForAll("functions") Function<Integer, HBResultType<Integer, Integer>> g)
  {
    assertEquals(
      a.flatMap(f).flatMap(g),
      a.flatMap(x -> f.apply(x).flatMap(g))
    );
  }

  @Property
  public void testOrElseThrowsSuccess(
    final @ForAll Integer a)
  {
    final var x =
      new HBResultSuccess<>(a).orElseThrow(o -> new IOException());

    assertEquals(a, x);
  }

  @Property
  public void testOrElseThrowsFailure(
    final @ForAll Integer a)
  {
    assertThrows(IOException.class, () -> {
      new HBResultFailure<>(a).orElseThrow(o -> new IOException());
    });
  }

  @Property
  public void testCast(
    final @ForAll("results") HBResultType<Integer, Integer> a)
  {
    if (a instanceof final HBResultSuccess<Integer, Integer> s) {
      assertEquals(a, s.cast());
    } else if (a instanceof final HBResultFailure<Integer, Integer> f) {
      assertEquals(a, f.cast());
    }
  }

  @Property
  public void testMap(
    final @ForAll("results") HBResultType<Integer, Integer> a)
  {
    final var r =
      a.map(x -> x * 2);

    if (a instanceof final HBResultSuccess<Integer, Integer> s) {
      assertEquals(
        s.result() * 2,
        ((HBResultSuccess<Integer, Integer>) r).result()
      );
    } else if (a instanceof final HBResultFailure<Integer, Integer> f) {
      assertEquals(a, r);
    }
  }
}
