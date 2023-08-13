package org.sonarlint.intellij.actions.filters.severity;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.UpdateInBackground;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.actions.SonarLintToolWindow;
import org.sonarlint.intellij.analysis.AnalysisSubmitter;
import org.sonarlint.intellij.common.ui.SonarLintConsole;
import org.sonarlint.intellij.config.Settings;
import org.sonarlint.intellij.config.project.SonarLintProjectSettings;
import org.sonarlint.intellij.finding.persistence.FindingsCache;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.sonarlint.intellij.common.ui.ReadActionUtils.runReadActionSafely;
import static org.sonarlint.intellij.common.util.SonarLintUtils.getService;

public abstract class FilterSeverityAction extends DumbAwareToggleAction implements UpdateInBackground {

    private final IssueSeverity severity;

    protected FilterSeverityAction(IssueSeverity severity) {
        this.severity = severity;
    }

    @Override
    public boolean isSelected(final @NotNull AnActionEvent event) {
        return this.getSettings(event)
                .map(settings -> settings.isSeverityDisplayed(this.severity))
                .orElse(true);
    }

    @Override
    public void setSelected(final @NotNull AnActionEvent event, boolean state) {
        ApplicationManager.getApplication().assertReadAccessAllowed();
        final Project project = getEventProject(event);

        if (project != null) {
            runReadActionSafely(project, () -> {
                this.getSettings(event)
                        .ifPresent(settings -> {
                            settings.setSeverityDisplayed(this.severity, state);
                            getService(project, AnalysisSubmitter.class).updateCurrentFileTab(); // refresh file tab
                            getService(project, SonarLintToolWindow.class).updateReportTab(); // refresh report tab

                            this.refreshCodeAnalyzer(project); // refresh code analyzer
                        });
            });

        }
    }

    private Optional<SonarLintProjectSettings> getSettings(final @NotNull AnActionEvent event) {
        return Optional.ofNullable(getEventProject(event))
                .map(Settings::getSettingsFor);
    }

    private void refreshCodeAnalyzer(@NotNull Project project) {
        ApplicationManager.getApplication().assertReadAccessAllowed();

        DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
        var editorManager = FileEditorManager.getInstance(project);
        var openFiles = editorManager.getOpenFiles();
        var psiFiles = findFiles(project, openFiles);
        psiFiles.forEach(codeAnalyzer::restart);
    }

    private Collection<PsiFile> findFiles(Project project, VirtualFile[] files) {
        PsiManager psiManager = PsiManager.getInstance(project);
        List<PsiFile> psiFiles = new ArrayList<PsiFile>(files.length);

        for (VirtualFile vFile : files) {
            if (!vFile.isValid()) {
                continue;
            }
            var psiFile = psiManager.findFile(vFile);
            if (psiFile != null) {
                psiFiles.add(psiFile);
            } else {
                SonarLintConsole.get(project).error("Couldn't find PSI for file: " + vFile.getPath());
            }
        }
        return psiFiles;
    }
}
