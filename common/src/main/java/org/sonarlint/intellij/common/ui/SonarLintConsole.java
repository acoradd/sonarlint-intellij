/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2021 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.common.ui;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface SonarLintConsole {

  static SonarLintConsole get(@NotNull Project p) {
    SonarLintConsole t = ServiceManager.getService(p, SonarLintConsole.class);
    if (t == null) {
      Logger.getInstance(SonarLintConsole.class).error("Could not find service: " + SonarLintConsole.class.getName());
      throw new IllegalArgumentException("Class not found: " + SonarLintConsole.class.getName());
    }

    return t;
  }

  void debug(String msg);

  boolean debugEnabled();

  void info(String msg);

  void error(String msg);

  void error(String msg, Throwable t);

  void clear();

  ConsoleView getConsoleView();

}