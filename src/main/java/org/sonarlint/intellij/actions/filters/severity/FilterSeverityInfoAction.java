package org.sonarlint.intellij.actions.filters.severity;

import org.sonarsource.sonarlint.core.commons.IssueSeverity;

public class FilterSeverityInfoAction extends FilterSeverityAction{
    public FilterSeverityInfoAction() {
        super(IssueSeverity.INFO);
    }
}
