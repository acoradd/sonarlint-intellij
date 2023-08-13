package org.sonarlint.intellij.actions.filters.severity;

import org.sonarsource.sonarlint.core.commons.IssueSeverity;

public class FilterSeverityMinorAction extends FilterSeverityAction{
    public FilterSeverityMinorAction() {
        super(IssueSeverity.MINOR);
    }
}
