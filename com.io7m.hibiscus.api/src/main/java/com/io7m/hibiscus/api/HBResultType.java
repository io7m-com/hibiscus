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

import java.util.function.Function;

/**
 * The type of results.
 *
 * @param <S> The type of success results
 * @param <F> The type of failure results
 */

public sealed interface HBResultType<S, F>
  permits HBResultFailure, HBResultSuccess
{
  /**
   * @return {@code true} if this is a success result
   */

  boolean isSuccess();

  /**
   * The functor map for result values.
   *
   * @param f   The function to be applied to the value
   * @param <T> The type of return values
   *
   * @return The mapped value
   */

  <T> HBResultType<T, F> map(Function<S, T> f);

  /**
   * The monadic "bind" for result values.
   *
   * @param f   The function to be applied to the value
   * @param <T> The type of return values
   *
   * @return The mapped value
   */

  <T> HBResultType<T, F> flatMap(Function<S, HBResultType<T, F>> f);

  /**
   * If this result is success, return it. Otherwise, construct an exception
   * using the failure value and throw it.
   *
   * @param f   The exception producer
   * @param <E> The type of thrown exceptions
   *
   * @return The success value
   *
   * @throws E On failure
   */

  <E extends Exception> S orElseThrow(Function<F, E> f)
    throws E;
}
