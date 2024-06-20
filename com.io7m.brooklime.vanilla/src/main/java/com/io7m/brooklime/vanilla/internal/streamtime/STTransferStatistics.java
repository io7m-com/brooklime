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


package com.io7m.brooklime.vanilla.internal.streamtime;

import java.util.OptionalDouble;
import java.util.OptionalLong;

/**
 * A snapshot of a transfer.
 *
 * @param sizeExpected    The expected size
 * @param sizeTransferred The amount transferred so far
 * @param octetsPerSecond The average octets per second
 */

public record STTransferStatistics(
  OptionalLong sizeExpected,
  long sizeTransferred,
  double octetsPerSecond)
{
  /**
   * Return the normalized completion percentage.
   *
   * @return The completion "percentage" in the range {@code [0, 1]}
   */

  public OptionalDouble percentNormalized()
  {
    if (this.sizeExpected.isPresent()) {
      final var e = (double) this.sizeExpected.getAsLong();
      final var t = (double) this.sizeTransferred;
      return OptionalDouble.of(t / e);
    }
    return OptionalDouble.empty();
  }

  /**
   * @return The completion percentage in the range {@code [0, 100]}
   */

  public OptionalDouble percent()
  {
    return this.percentNormalized()
      .stream()
      .map(x -> x * 100.0)
      .max();
  }

  /**
   * @return The number of seconds expected until the transfer is completed
   */

  public OptionalLong expectedSecondsRemaining()
  {
    if (this.sizeExpected.isPresent()) {
      if (this.octetsPerSecond == 0.0) {
        return OptionalLong.empty();
      }
      final var e = (double) this.sizeExpected.getAsLong();
      final var t = (double) this.sizeTransferred;
      final var r = e - t;
      return OptionalLong.of((long) (r / this.octetsPerSecond));
    }
    return OptionalLong.empty();
  }
}
