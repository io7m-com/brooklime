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

package com.io7m.brooklime.vanilla.internal;

import com.io7m.brooklime.api.BLProgressFileStarted;
import com.io7m.brooklime.api.BLProgressReceiverType;
import com.io7m.brooklime.api.BLProgressUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A progress counter.
 */

public final class BLProgressCounter
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLProgressCounter.class);

  private final BLProgressReceiverType receiver;
  private final Clock clock;
  private boolean atStart;
  private Instant timeLast;
  private long sizePeriod;
  private long sizeReceived;
  private long sizeExpected;
  private String name;
  private int fileIndex;
  private int fileCount;
  private int attemptIndex;
  private int attemptMaximum;

  /**
   * A progress counter.
   *
   * @param inClock    The clock used to track time
   * @param inReceiver The progress receiver
   */

  public BLProgressCounter(
    final Clock inClock,
    final BLProgressReceiverType inReceiver)
  {
    this.clock =
      Objects.requireNonNull(inClock, "inClock");
    this.receiver =
      Objects.requireNonNull(inReceiver, "receiver");

    this.name = "";
    this.sizeExpected = 0L;
    this.sizeReceived = 0L;
    this.sizePeriod = 0L;
    this.atStart = true;
    this.timeLast = this.clock.instant();
  }

  /**
   * Add a number of bytes.
   *
   * @param extra The byte count
   */

  public void addSizeReceived(
    final long extra)
  {
    this.setSizeReceived(this.sizeReceived + extra);
  }

  private double determineProgress()
  {
    final double raw = (double) this.sizeReceived / (double) this.sizeExpected;
    return Math.min(1.0, Math.max(0.0, raw));
  }

  private Duration estimateTimeRemaining()
  {
    if (this.sizePeriod == 0L) {
      return Duration.of(0L, ChronoUnit.SECONDS);
    }

    final long sizeRemaining =
      Math.max(0L, this.sizeExpected - this.sizeReceived);
    return Duration.of(sizeRemaining / this.sizePeriod, ChronoUnit.SECONDS);
  }

  /**
   * Start a new file.
   *
   * @param inName           The file name
   * @param inSizeExpected   The expected size
   * @param inAttemptIndex   The attempt number
   * @param inAttemptMaximum The maximum number of attempts
   * @param inFileIndex      The file index
   * @param inFileCount      The number of files
   */

  public void startFile(
    final String inName,
    final long inSizeExpected,
    final int inAttemptIndex,
    final int inAttemptMaximum,
    final int inFileIndex,
    final int inFileCount)
  {
    this.name = Objects.requireNonNull(inName, "name");
    this.sizeExpected = inSizeExpected;
    this.sizePeriod = 0L;
    this.sizeReceived = 0L;
    this.attemptIndex = inAttemptIndex;
    this.attemptMaximum = inAttemptMaximum;
    this.fileIndex = inFileIndex;
    this.fileCount = inFileCount;
    this.timeLast = this.clock.instant();
    this.atStart = true;

    this.receiver.onProgressEvent(
      BLProgressFileStarted.builder()
        .setAttemptCurrent(this.attemptIndex)
        .setAttemptMaximum(this.attemptMaximum)
        .setFileIndexCurrent(this.fileIndex + 1)
        .setFileIndexMaximum(this.fileCount)
        .setName(this.name)
        .build()
    );
  }

  /**
   * Set the currently received number of bytes.
   *
   * @param size The size
   */

  public void setSizeReceived(
    final long size)
  {
    final var sizeThen =
      this.sizeReceived;
    final var extra =
      size - sizeThen;

    this.sizeReceived += extra;
    this.sizePeriod += extra;

    if (this.sizeReceived > this.sizeExpected) {
      LOG.warn(
        "Wrote more data than expected (expected {} but received {})",
        Long.toUnsignedString(this.sizeExpected),
        Long.toUnsignedString(this.sizeReceived)
      );
    }

    final Instant timeNow =
      this.clock.instant();
    final var between =
      Duration.between(this.timeLast, timeNow);

    if (between.getSeconds() >= 1L || this.atStart) {
      this.timeLast = timeNow;
      this.receiver.onProgressEvent(
        BLProgressUpdate.builder()
          .setAttemptCurrent(this.attemptIndex)
          .setAttemptMaximum(this.attemptMaximum)
          .setBytesMaximum(this.sizeExpected)
          .setBytesPerSecond(this.sizePeriod)
          .setBytesSent(this.sizeReceived)
          .setFileIndexCurrent(this.fileIndex + 1)
          .setFileIndexMaximum(this.fileCount)
          .setName(this.name)
          .setProgress(this.determineProgress())
          .setTimeRemaining(this.estimateTimeRemaining())
          .build()
      );
      this.sizePeriod = 0L;
    }

    this.atStart = false;
  }
}
