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


package com.io7m.brooklime.tests;


import com.io7m.brooklime.vanilla.internal.streamtime.STTimedOutputStream;
import com.io7m.brooklime.vanilla.internal.streamtime.STTransferStatistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class STTimedOutputStreamTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(STTimedOutputStreamTest.class);

  private ScheduledExecutorService executor;

  @BeforeEach
  public void setup()
  {
    this.executor =
      Executors.newScheduledThreadPool(1);
  }

  @AfterEach
  public void tearDown()
  {
    this.executor.shutdown();
  }

  @Test
  public void testTimed0()
    throws Exception
  {
    final var stats =
      new LinkedBlockingQueue<STTransferStatistics>();
    final var bao =
      new ByteArrayOutputStream();

    final var buffer = new byte[10];
    try (var output =
           new STTimedOutputStream(this.executor, OptionalLong.of(50L), stats::add, bao)) {
      for (int index = 0; index < 5; ++index) {
        output.write(buffer);
        Thread.sleep(1_000L);
      }
    }

    verifyStats(stats);
  }

  static void verifyStats(
    final LinkedBlockingQueue<STTransferStatistics> stats)
  {
    assertTrue(stats.size() >= 5);
    assertTrue(stats.size() <= 7);
    final var statsCopy = List.copyOf(stats);

    statsCopy.forEach(s -> {
      LOG.debug(
        "{} {} {} {}",
        s,
        s.percentNormalized(),
        s.percent(),
        s.expectedSecondsRemaining()
      );
    });

    final var average =
      statsCopy.stream()
        .mapToDouble(STTransferStatistics::octetsPerSecond)
        .average()
        .getAsDouble();

    assertEquals(8.3, average, 3.0);

    final var sum =
      statsCopy.stream()
        .mapToDouble(STTransferStatistics::octetsPerSecond)
        .sum();

    assertEquals(50.0, sum, 25.0);
  }

  @Test
  public void testTimed1()
    throws Exception
  {
    final var stats =
      new LinkedBlockingQueue<STTransferStatistics>();
    final var bao =
      new ByteArrayOutputStream();

    final var buffer = new byte[10];
    try (var output =
           new STTimedOutputStream(this.executor, OptionalLong.of(50L), stats::add, bao)) {
      for (int index = 0; index < 5; ++index) {
        output.write(buffer, 0, buffer.length);
        Thread.sleep(1_000L);
      }
    }

    verifyStats(stats);
  }

  @Test
  public void testTimed2()
    throws Exception
  {
    final var stats =
      new LinkedBlockingQueue<STTransferStatistics>();
    final var bao =
      new ByteArrayOutputStream();

    final var buffer = new byte[10];
    try (var output =
           new STTimedOutputStream(this.executor, OptionalLong.of(50L), stats::add, bao)) {
      for (int index = 0; index < 5; ++index) {
        for (int k = 0; k < 10; ++k) {
          output.write(0);
        }
        Thread.sleep(1_000L);
      }
    }

    verifyStats(stats);
  }

  @Test
  public void testTimed3()
    throws Exception
  {
    final var stats =
      new LinkedBlockingQueue<STTransferStatistics>();
    final var bao =
      new ByteArrayOutputStream();

    final var buffer = new byte[10];
    try (var output = new STTimedOutputStream(this.executor, stats::add, bao)) {
      for (int index = 0; index < 5; ++index) {
        output.write(buffer, 0, buffer.length);
        Thread.sleep(1_000L);
      }
    }

    verifyStats(stats);
  }
}
