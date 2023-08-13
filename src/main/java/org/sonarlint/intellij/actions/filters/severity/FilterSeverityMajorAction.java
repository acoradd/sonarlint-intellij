package org.sonarlint.intellij.actions.filters.severity;

import org.sonarsource.sonarlint.core.commons.IssueSeverity;

public class FilterSeverityMajorAction extends FilterSeverityAction{
    public FilterSeverityMajorAction() {
        super(IssueSeverity.MAJOR);
    }
}
