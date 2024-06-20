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

import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.lang.Integer.toUnsignedString;

/**
 * A tracker for transfer statistics.
 */

public final class STStatisticsTracker implements AutoCloseable
{
  private final OptionalLong expected;
  private final Consumer<STTransferStatistics> consumer;
  private final ScheduledExecutorService executor;
  private volatile long transferredPeriod;
  private volatile long transferredTotal;

  /**
   * Create a new tracker and start delivering statistics to the given consumer.
   *
   * @param inExecutor A statistics executor
   * @param inExpected The expected transfer size
   * @param inConsumer The statistics receiver
   */

  public STStatisticsTracker(
    final ScheduledExecutorService inExecutor,
    final OptionalLong inExpected,
    final Consumer<STTransferStatistics> inConsumer)
  {
    this.expected =
      inExpected;
    this.consumer =
      Objects.requireNonNull(inConsumer, "consumer");
    this.executor =
      Objects.requireNonNull(inExecutor, "inExecutor");

    this.executor.scheduleAtFixedRate(
      this::broadcast,
      1L,
      1L,
      TimeUnit.SECONDS
    );

    this.broadcast();
  }

  /**
   * The given number of octets have been transferred.
   *
   * @param octets The octet count
   */

  public void add(
    final long octets)
  {
    this.transferredPeriod += octets;
    this.transferredTotal += octets;
  }

  /**
   * Broadcast state now.
   */

  void broadcast()
  {
    this.consumer.accept(this.sample());
    this.transferredPeriod = 0L;
  }

  /**
   * @return A sample of the current statistics
   */

  STTransferStatistics sample()
  {
    return new STTransferStatistics(
      this.expected,
      this.transferredTotal,
      this.transferredPeriod
    );
  }

  @Override
  public void close()
  {

  }

  @Override
  public String toString()
  {
    return "[STStatisticsTracker 0x%s]"
      .formatted(toUnsignedString(this.hashCode(), 16));
  }
}
