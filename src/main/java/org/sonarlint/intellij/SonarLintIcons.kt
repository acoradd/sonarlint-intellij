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
package org.sonarlint.intellij

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import org.sonarsource.sonarlint.core.commons.ImpactSeverity
import org.sonarsource.sonarlint.core.commons.IssueSeverity
import org.sonarsource.sonarlint.core.commons.RuleType
import org.sonarsource.sonarlint.core.commons.VulnerabilityProbability
import java.awt.Color
import javax.swing.Icon

object SonarLintIcons {

    // From IntelliJ Platform UI Guidelines
    private val red60Color = Color(224, 85, 85, 60)
    private val orange60Color = Color(242, 101, 34, 60)
    private val yellow60Color = Color(244, 175, 61, 60)

    @JvmField
    val ICON_SONARQUBE = getIcon("/images/SonarQube.png")
    @JvmField
    val ICON_SONARCLOUD = getIcon("/images/SonarCloud.png")
    @JvmField
    val ICON_SONARQUBE_16 = getIcon("/images/onde-sonar-16.png")
    @JvmField
    val ICON_SONARCLOUD_16 = getIcon("/images/sonarcloud-16.png")
    @JvmField
    val SONARLINT_TOOLWINDOW = getIcon("/images/sonarlintToolWindow.svg")
    @JvmField
    val SONARLINT_ACTION = getIcon("/images/sonarlintAction.svg")
    @JvmField
    val SONARLINT_TOOLWINDOW_EMPTY = getIcon("/images/sonarlintToolWindowEmpty.svg")
    @JvmField
    val SONARLINT = getIcon("/images/sonarlint.png")
    @JvmField
    val SONARLINT_32 = getIcon("/images/sonarlint@2x.png")
    @JvmField
    val PLAY = getIcon("/images/execute.png")
    @JvmField
    val CLEAN = getIcon("/images/clean.png")
    @JvmField
    val TOOLS = getIcon("/images/externalToolsSmall.png")
    @JvmField
    val SUSPEND = getIcon("/images/suspend.png")
    @JvmField
    val INFO = getIcon("/images/info.png")
    @JvmField
    val WARN = getIcon("/images/warn.png")
    @JvmField
    val SCM = getIcon("/images/toolWindowChanges.png")
    @JvmField
    val PROJECT = getIcon("/images/ideaProject.png")
    @JvmField
    val NOT_CONNECTED = getIcon("/images/not_connected.svg")
    @JvmField
    val CONNECTED = getIcon("/images/connected.svg")
    @JvmField
    val CONNECTION_ERROR = getIcon("/images/io_error.svg")

    @JvmField
    val RESOLVED = getIcon("/images/resolved.svg")

    @JvmField
    val SEVERITY_BLOCKER = getIcon("/images/severity/blocker.svg")
    @JvmField
    val SEVERITY_CRITICAL = getIcon("/images/severity/critical.svg")
    @JvmField
    val SEVERITY_INFO = getIcon("/images/severity/info.svg")
    @JvmField
    val SEVERITY_MAJOR = getIcon("/images/severity/major.svg")
    @JvmField
    val SEVERITY_MINOR = getIcon("/images/severity/minor.svg")

    private val SEVERITY_ICONS = mapOf(
        IssueSeverity.BLOCKER to SEVERITY_BLOCKER,
        IssueSeverity.CRITICAL to SEVERITY_CRITICAL,
        IssueSeverity.INFO to SEVERITY_INFO,
        IssueSeverity.MAJOR to SEVERITY_MAJOR,
        IssueSeverity.MINOR to SEVERITY_MINOR
    )

    private val IMPACT_ICONS = mapOf(
        ImpactSeverity.HIGH to getIcon("/images/impact/high.svg"),
        ImpactSeverity.MEDIUM to getIcon("/images/impact/medium.svg"),
        ImpactSeverity.LOW to getIcon("/images/impact/low.svg")
    )

    private val TYPE_ICONS = mapOf(
        RuleType.BUG to getIcon("/images/type/bug.svg"),
        RuleType.CODE_SMELL to getIcon("/images/type/codeSmell.svg"),
        RuleType.VULNERABILITY to getIcon("/images/type/vulnerability.svg"),
        RuleType.SECURITY_HOTSPOT to getIcon("/images/type/hotspot.svg")
    )

    private val PROBABILITY_ICONS = mapOf(
        VulnerabilityProbability.HIGH to getIcon("/images/type/hotspotHigh.svg"),
        VulnerabilityProbability.MEDIUM to getIcon("/images/type/hotspotMedium.svg"),
        VulnerabilityProbability.LOW to getIcon("/images/type/hotspotLow.svg")
    )

    val colorsByProbability = mapOf(
        VulnerabilityProbability.HIGH to JBColor(red60Color, red60Color),
        VulnerabilityProbability.MEDIUM to JBColor(orange60Color, orange60Color),
        VulnerabilityProbability.LOW to JBColor(yellow60Color, yellow60Color)
    )

    val backgroundColorsByImpact = mapOf(
        ImpactSeverity.HIGH to JBColor(Color(180, 35, 24, 20), Color(180, 35, 24, 60)),
        ImpactSeverity.MEDIUM to JBColor(Color(174, 122, 41, 20), Color(174, 122, 41, 60)),
        ImpactSeverity.LOW to JBColor(Color(49, 108, 146, 20), Color(49, 108, 146, 60))
    )

    val fontColorsByImpact = mapOf(
        ImpactSeverity.HIGH to JBColor(Color(128, 27, 20), Color.LIGHT_GRAY),
        ImpactSeverity.MEDIUM to JBColor(Color(140, 94, 30), Color.LIGHT_GRAY),
        ImpactSeverity.LOW to JBColor(Color(49, 108, 146), Color.LIGHT_GRAY)
    )

    private fun getIcon(path: String): Icon {
        return IconLoader.getIcon(path, SonarLintIcons::class.java)
    }

    @JvmStatic
    fun severity(severity: IssueSeverity): Icon {
        return SEVERITY_ICONS[severity]!!
    }

    @JvmStatic
    fun type(type: RuleType): Icon {
        return TYPE_ICONS[type]!!
    }

    @JvmStatic
    fun impact(impact: ImpactSeverity): Icon {
        return IMPACT_ICONS[impact]!!
    }

    @JvmStatic
    fun toDisabled(icon: Icon): Icon {
        return IconLoader.getDisabledIcon(icon)
    }

    @JvmStatic
    fun hotspotTypeWithProbability(vulnerabilityProbability: VulnerabilityProbability): Icon {
        return PROBABILITY_ICONS[vulnerabilityProbability]!!
    }
}
