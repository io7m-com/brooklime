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

package com.io7m.brooklime.cmdline.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class to periodically log a message at INFO level, in order to keep
 * the client from accidentally causing overly-strict continuous integration
 * systems from thinking the operation is hanging.
 */

public final class BLChatter
{
  private static final Logger LOG =
    LoggerFactory.getLogger(BLChatter.class);
  private static final BLChatter INSTANCE = new BLChatter();
  private final ScheduledExecutorService executor;
  private final AtomicBoolean speaking;

  private BLChatter()
  {
    this.speaking = new AtomicBoolean(false);

    this.executor =
      Executors.newScheduledThreadPool(1, r -> {
        final Thread thread = new Thread(r);
        thread.setName(String.format(
          "com.io7m.brooklime.cmdline.internal.BLChatter[%d]",
          Long.valueOf(thread.getId())));
        thread.setDaemon(true);
        return thread;
      });

    this.executor.scheduleAtFixedRate(
      this::chat, 0L, 20L, TimeUnit.SECONDS
    );
  }

  /**
   * @return A chatter instance
   */

  public static BLChatter getInstance()
  {
    return INSTANCE;
  }

  private void chat()
  {
    if (this.speaking.get()) {
      LOG.info("still executing...");
    }
  }

  /**
   * Start speaking.
   */

  public void start()
  {
    this.speaking.set(true);
  }

  /**
   * Stop speaking.
   */

  public void stop()
  {
    this.speaking.set(false);
  }
}
