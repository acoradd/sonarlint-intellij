/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2023 SonarSource
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
package org.sonarlint.intellij.finding.hotspot;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonarlint.intellij.common.ui.SonarLintConsole;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.common.vcs.VcsService;
import org.sonarlint.intellij.config.global.ServerConnection;
import org.sonarlint.intellij.core.BackendService;
import org.sonarlint.intellij.core.ModuleBindingManager;
import org.sonarlint.intellij.core.ProjectBindingManager;
import org.sonarlint.intellij.exception.InvalidBindingException;
import org.sonarlint.intellij.finding.persistence.FindingsCache;
import org.sonarlint.intellij.finding.tracking.ServerFindingTracker;
import org.sonarlint.intellij.finding.tracking.Trackable;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot;
import org.sonarsource.sonarlint.core.serverconnection.DownloadException;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;

import static org.sonarlint.intellij.common.util.SonarLintUtils.getService;
import static org.sonarlint.intellij.config.Settings.getSettingsFor;
import static org.sonarlint.intellij.util.FutureUtils.waitForTask;
import static org.sonarlint.intellij.util.FutureUtils.waitForTasks;
import static org.sonarlint.intellij.util.ProjectUtils.getRelativePaths;


@Service(Service.Level.PROJECT)
public final class ServerSecurityHotspotUpdater implements Disposable {
  private static final int THREADS_NUM = 5;
  private static final int QUEUE_LIMIT = 100;
  private static final int FETCH_ALL_HOTSPOTS_THRESHOLD = 10;
  private final Project myProject;

  private final ExecutorService executorService;
  private final ServerFindingTracker serverFindingTracker = new ServerFindingTracker();

  public ServerSecurityHotspotUpdater(Project project) {
    myProject = project;

    // Equivalent to Executors.newFixedThreadPool(THREADS_NUM), but instead of the default unlimited LinkedBlockingQueue,
    // we use ArrayBlockingQueue with a cap. This means that if QUEUE_LIMIT tasks are already queued (and THREADS_NUM being executed),
    // new tasks will be rejected with RejectedExecutionException.
    // http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
    this.executorService = new ThreadPoolExecutor(THREADS_NUM, THREADS_NUM, 0L, TimeUnit.MILLISECONDS, queue);
  }

  public void matchServerSecurityHotspots(Map<Module, Collection<VirtualFile>> filesPerModule) {
    var projectSettings = getSettingsFor(myProject);
    if (!projectSettings.isBound()) {
      // not in connected mode
      return;
    }
    try {
      var projectBindingManager = getService(myProject, ProjectBindingManager.class);
      var connection = projectBindingManager.getServerConnection();
      var engine = projectBindingManager.getConnectedEngine();

      filesPerModule.forEach((module, files) -> {
        var relativePathPerFile = getRelativePaths(module.getProject(), files);
        var updater = new SecurityHotspotUpdater(connection, engine);

        relativePathPerFile.forEach((vFile, relativePath) -> {
          var projectBinding = getProjectBinding(module);
          var branchName = getService(myProject, VcsService.class).getServerBranchName(module);
          updater.matchHotspotsSingleFile(projectBinding, branchName, vFile, relativePath);
        });
      });
    } catch (InvalidBindingException e) {
      // ignore, do nothing
    }
  }

  public void fetchAndMatchServerSecurityHotspots(Map<Module, Collection<VirtualFile>> filesPerModule, ProgressIndicator indicator) {
    var projectSettings = getSettingsFor(myProject);
    if (!projectSettings.isBound()) {
      // not in connected mode
      return;
    }
    ServerConnection connection;
    ConnectedSonarLintEngine engine;
    try {
      var projectBindingManager = getService(myProject, ProjectBindingManager.class);
      connection = projectBindingManager.getServerConnection();
      engine = projectBindingManager.getConnectedEngine();
    } catch (InvalidBindingException e) {
      // ignore, do nothing
      return;
    }

    var numFiles = filesPerModule.values().stream().mapToInt(Collection::size).sum();
    var downloadAll = numFiles >= FETCH_ALL_HOTSPOTS_THRESHOLD;
    String msg;

    if (downloadAll) {
      msg = "Fetching all server hotspots";
    } else {
      msg = "Fetching server hotspots in " + numFiles + SonarLintUtils.pluralize(" file", numFiles);
    }
    msg += " (waiting for results)";
    var console = getService(myProject, SonarLintConsole.class);
    console.debug(msg);
    indicator.setText(msg);

    // submit tasks
    var updateTasks = fetchAndMatchServerSecurityHotspots(filesPerModule, connection, engine, downloadAll);
    Future<?> waitForTasksTask = executorService.submit(() -> waitForTasks(myProject, indicator, updateTasks, "ServerHotspotUpdater"));
    waitForTask(myProject, indicator, waitForTasksTask, "Wait", Duration.ofSeconds(60));
  }

  private List<Future<?>> fetchAndMatchServerSecurityHotspots(Map<Module, Collection<VirtualFile>> filesPerModule,
    ServerConnection connection, ConnectedSonarLintEngine engine, boolean downloadAll) {
    var futureList = new LinkedList<Future<?>>();

    if (!downloadAll) {
      for (var e : filesPerModule.entrySet()) {
        var projectKey = getService(e.getKey(), ModuleBindingManager.class).resolveProjectKey();
        futureList.addAll(fetchAndMatchServerSecurityHotspots(Objects.requireNonNull(projectKey), e.getKey(), e.getValue(), connection,
          engine));
      }
    } else {
      futureList.addAll(downloadAndMatchAllServerHotspots(filesPerModule, connection, engine));
    }
    return futureList;
  }

  private List<Future<?>> downloadAndMatchAllServerHotspots(Map<Module, Collection<VirtualFile>> filesPerModule, ServerConnection server,
    ConnectedSonarLintEngine engine) {
    var hotspotUpdater = new SecurityHotspotUpdater(server, engine);
    var futuresList = new ArrayList<Future<?>>();
    var updatedProjects = ConcurrentHashMap.newKeySet();
    for (var e : filesPerModule.entrySet()) {
      var module = e.getKey();
      var projectKey = getService(module, ModuleBindingManager.class).resolveProjectKey();
      var branchName = getService(myProject, VcsService.class).getServerBranchName(module);
      Runnable task = () -> {
        if (updatedProjects.add(projectKey)) {
          hotspotUpdater.downloadAllServerHotspots(module, Objects.requireNonNull(projectKey));
        }
        var binding = getProjectBinding(module);
        var relativePathPerFile = getRelativePaths(module.getProject(), e.getValue());

        for (var entry : relativePathPerFile.entrySet()) {
          hotspotUpdater.matchHotspotsSingleFile(binding, branchName, entry.getKey(), entry.getValue());
        }
      };
      futuresList.add(submit(task, Objects.requireNonNull(projectKey), null));
    }

    return futuresList;
  }

  private static ProjectBinding getProjectBinding(Module module) {
    var moduleBindingManager = getService(module, ModuleBindingManager.class);
    return moduleBindingManager.getBinding();
  }

  private List<Future<?>> fetchAndMatchServerSecurityHotspots(String projectKey, Module module, Collection<VirtualFile> files,
    ServerConnection server, ConnectedSonarLintEngine engine) {
    var futureList = new LinkedList<Future<?>>();
    var relativePathPerFile = getRelativePaths(module.getProject(), files);
    var hotspotUpdater = new SecurityHotspotUpdater(server, engine);

    for (var e : relativePathPerFile.entrySet()) {
      Runnable task = () -> hotspotUpdater.downloadAndMatchHotspotsSingleFile(module, e.getKey(), e.getValue());
      futureList.add(submit(task, projectKey, e.getValue()));
    }
    return futureList;
  }

  private Future<?> submit(Runnable task, String projectKey, @Nullable String moduleRelativePath) {
    try {
      return this.executorService.submit(task);
    } catch (RejectedExecutionException e) {
      SonarLintConsole.get(myProject).error("fetch and match server hotspots rejected for projectKey=" + projectKey + ", filepath=" + moduleRelativePath, e);
      return CompletableFuture.completedFuture(null);
    }
  }

  @Override
  public void dispose() {
    var rejected = executorService.shutdownNow();
    if (!rejected.isEmpty()) {
      SonarLintConsole.get(myProject).error("rejected " + rejected.size() + " pending tasks");
    }
  }

  private class SecurityHotspotUpdater {
    private final ServerConnection server;
    private final ConnectedSonarLintEngine engine;

    private SecurityHotspotUpdater(ServerConnection server, ConnectedSonarLintEngine engine) {
      this.server = server;
      this.engine = engine;
    }

    public void matchHotspotsSingleFile(ProjectBinding projectBinding, @Nullable String branchName, VirtualFile virtualFile,
      String relativePath) {
      Collection<ServerHotspot> serverHotspots;
      if (branchName == null) {
        SonarLintConsole.get(myProject).debug("Skip loading stored hotspots, branch is unknown");
        serverHotspots = List.of();
      } else {
        serverHotspots = engine.getServerHotspots(projectBinding, branchName, relativePath);
      }
      matchFile(virtualFile, serverHotspots);
    }

    private void downloadAndMatchHotspotsSingleFile(Module module, VirtualFile virtualFile, String relativePath) {
      var serverHotspots = downloadAllServerHotspotsForFile(module, relativePath);
      matchFile(virtualFile, serverHotspots);
    }

    private void downloadAllServerHotspots(Module module, String projectKey) {
      try {
        SonarLintConsole.get(myProject).debug("fetchServerHotspots projectKey=" + projectKey);
        var branchName = getService(myProject, VcsService.class).getServerBranchName(module);
        if (branchName == null) {
          SonarLintConsole.get(myProject).debug("Skip fetching server hotspots, branch is unknown");
          return;
        }
        engine.downloadAllServerHotspots(server.getEndpointParams(), getService(BackendService.class).getHttpClient(server.getName()), projectKey, branchName, null);
      } catch (DownloadException e) {
        var console = getService(myProject, SonarLintConsole.class);
        console.info(e.getMessage());
      }
    }

    private void matchFile(VirtualFile virtualFile, Collection<ServerHotspot> serverHotspots) {
      try {
        Collection<Trackable> serverHotspotsTrackable = serverHotspots.stream()
          .map(ServerSecurityHotspotTrackable::new)
          .collect(Collectors.toList());

        if (!serverHotspotsTrackable.isEmpty()) {
          var securityHotspotsForFile = getService(myProject, FindingsCache.class).getSecurityHotspotsForFile(virtualFile);
          serverFindingTracker.matchLocalWithServerFindings(serverHotspotsTrackable, securityHotspotsForFile);
        }
      } catch (Throwable t) {
        // note: without catching Throwable, any exceptions raised in the thread will not be visible
        var console = getService(myProject, SonarLintConsole.class);
        console.error("error while fetching and matching server hotspots", t);
      }
    }

    private Collection<ServerHotspot> downloadAllServerHotspotsForFile(Module module, String relativePath) {
      var projectBinding = getProjectBinding(module);
      SonarLintConsole.get(myProject).debug("downloadAllServerHotspotsForFile projectKey=" + projectBinding.projectKey() + ", filepath=" + relativePath);
      var branchName = getService(myProject, VcsService.class).getServerBranchName(module);
      if (branchName == null) {
        SonarLintConsole.get(myProject).debug("Skip downloading hotspots, branch is unknown");
        return List.of();
      }
      try {
        engine.downloadAllServerHotspotsForFile(server.getEndpointParams(), getService(BackendService.class).getHttpClient(server.getName()), projectBinding, relativePath,
          branchName, null);
      } catch (DownloadException e) {
        var console = getService(myProject, SonarLintConsole.class);
        console.info(e.getMessage());
      }
      return engine.getServerHotspots(projectBinding, branchName, relativePath);
    }
  }
}
