package org.sonarlint.intellij.actions.filters.severity;

import org.sonarsource.sonarlint.core.commons.IssueSeverity;

public class FilterSeverityBlockerAction extends FilterSeverityAction{
    public FilterSeverityBlockerAction() {
        super(IssueSeverity.BLOCKER);
    }
}
