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

import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The root command.
 */

public class BLCommandRoot implements BLCommandType
{
  @Parameter(
    names = "--verbose",
    converter = BLLogLevelConverter.class,
    description = "Set the minimum logging verbosity level"
  )
  private BLLogLevel verbose = BLLogLevel.LOG_INFO;

  /**
   * The root command.
   */

  public BLCommandRoot()
  {

  }

  /**
   * Set up logging for other commands.
   *
   * @return The command status
   *
   * @throws Exception On errors
   */

  @Override
  public Status execute()
    throws Exception
  {
    final ch.qos.logback.classic.Logger root =
      (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
        Logger.ROOT_LOGGER_NAME);
    root.setLevel(this.verbose.toLevel());
    return Status.SUCCESS;
  }
}
