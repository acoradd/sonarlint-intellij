package org.sonarlint.intellij.actions.filters.severity;

import org.sonarsource.sonarlint.core.commons.IssueSeverity;

public class FilterSeverityCriticalAction extends FilterSeverityAction{
    public FilterSeverityCriticalAction() {
        super(IssueSeverity.CRITICAL);
    }
}
