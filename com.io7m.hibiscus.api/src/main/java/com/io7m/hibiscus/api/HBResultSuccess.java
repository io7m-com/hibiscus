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
 * A success result.
 *
 * @param result The result value
 * @param <S>    The type of success results
 * @param <F>    The type of failure results
 */

public record HBResultSuccess<S, F>(S result)
  implements HBResultType<S, F>
{
  @Override
  public boolean isSuccess()
  {
    return true;
  }

  /**
   * Safely cast this value.
   *
   * @param <G> The failure type
   *
   * @return This value assuming the failure value was of type {@code G}
   */

  @SuppressWarnings("unchecked")
  public <G> HBResultSuccess<S, G> cast()
  {
    return (HBResultSuccess<S, G>) this;
  }

  @Override
  public <T> HBResultType<T, F> map(
    final Function<S, T> f)
  {
    return new HBResultSuccess<>(f.apply(this.result));
  }

  @Override
  public <T> HBResultType<T, F> flatMap(
    final Function<S, HBResultType<T, F>> f)
  {
    return f.apply(this.result);
  }

  @Override
  public <E extends Exception> S orElseThrow(
    final Function<F, E> f)
  {
    return this.result;
  }
}
