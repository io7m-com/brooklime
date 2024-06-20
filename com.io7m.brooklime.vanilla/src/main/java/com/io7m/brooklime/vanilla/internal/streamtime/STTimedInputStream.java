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

import org.apache.commons.io.input.ProxyInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * A timed input stream. The stream accepts a function that will receive
 * transfer statistics at a rate of one update per second.
 */

public final class STTimedInputStream extends ProxyInputStream
{
  private final STStatisticsTracker tracker;

  /**
   * Create an input stream.
   *
   * @param inExecutor    A statistics executor
   * @param statsConsumer A function that receives statistics updates
   * @param inStream      The underlying input stream
   */

  public STTimedInputStream(
    final ScheduledExecutorService inExecutor,
    final Consumer<STTransferStatistics> statsConsumer,
    final InputStream inStream)
  {
    this(inExecutor, OptionalLong.empty(), statsConsumer, inStream);
  }

  /**
   * Create an input stream.
   *
   * @param inExecutor    A statistics executor
   * @param expected      The expected total number of octets that will be transferred
   * @param statsConsumer A function that receives statistics updates
   * @param inStream      The underlying input stream
   */

  public STTimedInputStream(
    final ScheduledExecutorService inExecutor,
    final OptionalLong expected,
    final Consumer<STTransferStatistics> statsConsumer,
    final InputStream inStream)
  {
    super(Objects.requireNonNull(inStream, "inStream"));
    this.tracker = new STStatisticsTracker(inExecutor, expected, statsConsumer);
  }

  @Override
  public int read()
    throws IOException
  {
    final var r = super.read();
    if (r >= 0) {
      this.tracker.add(1L);
    }
    return r;
  }

  @Override
  public int read(
    final byte[] bts,
    final int off,
    final int len)
    throws IOException
  {
    final var r = super.read(bts, off, len);
    if (r >= 0) {
      this.tracker.add(Integer.toUnsignedLong(r));
    }
    return r;
  }

  @Override
  public int read(
    final byte[] b)
    throws IOException
  {
    final var r = super.read(b);
    if (r >= 0) {
      this.tracker.add(Integer.toUnsignedLong(r));
    }
    return r;
  }

  @Override
  public void close()
    throws IOException
  {
    this.tracker.close();
    super.close();
  }
}
