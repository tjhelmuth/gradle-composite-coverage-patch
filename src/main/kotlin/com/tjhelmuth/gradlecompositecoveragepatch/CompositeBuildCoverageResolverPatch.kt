package com.tjhelmuth.gradlecompositecoveragepatch

import com.intellij.execution.CommandLineUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Consumer
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

/**
 * This is intended to be a patch for org.jetbrains.plugins.gradle.service.project.JavaGradleProjectResolver which has
 * an init script that handles the case where we are running a test for a module that is included in the root project
 * as a composite build using includeBuild.
 *
 * See https://youtrack.jetbrains.com/issue/IDEA-283140/Unable-to-run-the-code-coverage-in-Gradle-project-when-using-includeBuild-to-include-a-sub-project
 */
@Order(ExternalSystemConstants.UNORDERED)
class CompositeBuildCoverageResolverPatch : AbstractProjectResolverExtension() {
    override fun enhanceTaskProcessing(
        taskNames: MutableList<String>,
        jvmParametersSetup: String?,
        initScriptConsumer: Consumer<String>
    ) {
        if(StringUtil.isEmpty(jvmParametersSetup)) {
            return
        }

        val names = "[${toStringListLiteral(taskNames, ", ")}]"
        var argv = ParametersListUtil.parse(jvmParametersSetup!!)
        if (SystemInfo.isWindows) {
            argv = argv.map { CommandLineUtil.escapeParameterOnWindows(it, false) }
        }
        val jvmArgs = toStringListLiteral(argv, " << ")

        val script = InitScriptUtil.loadJvmParamsInitScript(jvmArgs, names)
        initScriptConsumer.consume(script)
    }


    @NotNull
    private fun toStringListLiteral(strings: List<String>, separator: String): String {
        val quotedStrings = strings.map { s -> StringUtil.escapeChar(toStringLiteral(s), '$') }
        return StringUtil.join(quotedStrings, separator)
    }

    @NotNull
    private fun toStringLiteral(s: String): String {
        return StringUtil.wrapWithDoubleQuote(StringUtil.escapeStringCharacters(s))
    }
}