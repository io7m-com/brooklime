/*
 * Copyright © 2020 Mark Raynsford <code@io7m.com> http://io7m.com
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

import com.io7m.brooklime.api.BLProgressEventType;
import com.io7m.brooklime.api.BLProgressFileStarted;
import com.io7m.brooklime.api.BLProgressUpdate;
import com.io7m.brooklime.vanilla.internal.BLProgressCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;

public final class BLProgressCounterTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLProgressCounterTest.class);

  private BLFakeClock clock;
  private ArrayList<BLProgressEventType> events;

  @BeforeEach
  public void testSetup()
  {
    this.clock = new BLFakeClock();
    this.events = new ArrayList<BLProgressEventType>();
  }

  @Test
  public void testIntegration()
  {
    final BLProgressCounter counter =
      new BLProgressCounter(Clock.systemUTC(), this::logEvent);

    final long waitTimeMs = 100L;
    final int periods = 50;
    final long sizeMax = waitTimeMs * periods;

    counter.startFile("file0", sizeMax, 0, 1, 0, 1);
    for (int index = 0; index < periods; ++index) {
      counter.addSizeReceived(waitTimeMs);
      try {
        Thread.sleep(waitTimeMs);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    Assertions.assertTrue(this.events.size() >= 5);

    {
      final BLProgressFileStarted e = this.take(BLProgressFileStarted.class);
      Assertions.assertEquals("file0", e.name());
      Assertions.assertEquals(1, e.fileIndexCurrent());
      Assertions.assertEquals(1, e.fileIndexMaximum());
    }

    {
      final BLProgressUpdate e = this.take(BLProgressUpdate.class);
      Assertions.assertEquals("file0", e.name());
      Assertions.assertEquals(1, e.fileIndexCurrent());
      Assertions.assertEquals(1, e.fileIndexMaximum());
      Assertions.assertTrue(e.progress() > 0.0 && e.progress() <= 1.0);
    }

    while (!this.events.isEmpty()) {
      final BLProgressUpdate e = this.take(BLProgressUpdate.class);
      Assertions.assertEquals("file0", e.name());
      Assertions.assertEquals(1, e.fileIndexCurrent());
      Assertions.assertEquals(1, e.fileIndexMaximum());
      Assertions.assertTrue(e.progress() > 0.0 && e.progress() <= 1.0);
    }
  }

  @Test
  public void testThreeSeconds()
  {
    final BLProgressCounter counter =
      new BLProgressCounter(this.clock, this::logEvent);

    this.clock.setNow(Instant.ofEpochSecond(0L));
    counter.startFile("file0", 30L, 0, 1, 0, 1);
    counter.addSizeReceived(10L);
    counter.addSizeReceived(10L);
    this.clock.setNow(Instant.ofEpochSecond(2L));
    counter.addSizeReceived(10L);

    Assertions.assertEquals(3, this.events.size());

    {
      final BLProgressFileStarted e = this.take(BLProgressFileStarted.class);
      Assertions.assertEquals("file0", e.name());
      Assertions.assertEquals(1, e.fileIndexCurrent());
      Assertions.assertEquals(1, e.fileIndexMaximum());
    }

    {
      final BLProgressUpdate e = this.take(BLProgressUpdate.class);
      Assertions.assertEquals("file0", e.name());
      Assertions.assertEquals(1, e.fileIndexCurrent());
      Assertions.assertEquals(1, e.fileIndexMaximum());
      Assertions.assertEquals(30L, e.bytesMaximum());
      Assertions.assertEquals(10L, e.bytesPerSecond());
      Assertions.assertEquals(10L, e.bytesSent());
      Assertions.assertEquals(0.333, e.progress(), 0.001);
    }
  }

  private <T extends BLProgressEventType> T take(
    final Class<T> clazz)
  {
    return clazz.cast(this.events.remove(0));
  }

  private void logEvent(
    final BLProgressEventType event)
  {
    LOG.debug("event: {}", event);
    this.events.add(event);
  }
}
