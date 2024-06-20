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

import org.apache.commons.io.output.ProxyOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * A timed output stream. The stream accepts a function that will receive
 * transfer statistics at a rate of one update per second.
 */

public final class STTimedOutputStream extends ProxyOutputStream
{
  private final STStatisticsTracker tracker;

  /**
   * Create an output stream.
   *
   * @param inExecutor    A statistics executor
   * @param statsConsumer A function that receives statistics updates
   * @param inStream      The underlying output stream
   */

  public STTimedOutputStream(
    final ScheduledExecutorService inExecutor,
    final Consumer<STTransferStatistics> statsConsumer,
    final OutputStream inStream)
  {
    this(inExecutor, OptionalLong.empty(), statsConsumer, inStream);
  }

  /**
   * Create an output stream.
   *
   * @param inExecutor    A statistics executor
   * @param expected      The expected total number of octets that will be transferred
   * @param statsConsumer A function that receives statistics updates
   * @param inStream      The underlying output stream
   */

  public STTimedOutputStream(
    final ScheduledExecutorService inExecutor,
    final OptionalLong expected,
    final Consumer<STTransferStatistics> statsConsumer,
    final OutputStream inStream)
  {
    super(Objects.requireNonNull(inStream, "inStream"));
    this.tracker = new STStatisticsTracker(inExecutor, expected, statsConsumer);
  }

  @Override
  public void write(
    final byte[] b,
    final int off,
    final int len)
    throws IOException
  {
    this.tracker.add(Integer.toUnsignedLong(len));
    super.write(b, off, len);
  }

  @Override
  public void write(
    final byte[] b)
    throws IOException
  {
    this.tracker.add(Integer.toUnsignedLong(b.length));
    super.write(b);
  }

  @Override
  public void write(
    final int b)
    throws IOException
  {
    this.tracker.add(1L);
    super.write(b);
  }

  @Override
  public void close()
    throws IOException
  {
    this.tracker.close();
    super.close();
  }
}
