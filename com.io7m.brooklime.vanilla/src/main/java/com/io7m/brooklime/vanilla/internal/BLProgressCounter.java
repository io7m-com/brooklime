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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class BLProgressCounter
{
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

  public void addSizeReceived(
    final long extra)
  {
    this.sizeReceived += extra;
    this.sizePeriod += extra;

    if (this.sizeReceived > this.sizeExpected) {
      throw new IllegalStateException(
        String.format(
          "Wrote more data than expected (expected %d but received %d)",
          Long.valueOf(this.sizeExpected),
          Long.valueOf(this.sizeReceived)
        )
      );
    }

    final Instant timeNow = this.clock.instant();
    if (Duration.between(
      this.timeLast,
      timeNow).getSeconds() >= 1L || this.atStart) {
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
}
