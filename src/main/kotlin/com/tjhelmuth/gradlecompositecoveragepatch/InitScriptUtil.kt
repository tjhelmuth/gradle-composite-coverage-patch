package com.tjhelmuth.gradlecompositecoveragepatch

import java.io.IOException
import java.util.regex.Matcher


class InitScriptUtil {
    companion object {
        /**
         * Loads a .gradle groovy source file as a string, replacing any parameters
         */
        fun loadJvmParamsInitScript(params: String, tasks: String): String {
            return loadInitScript("/JvmParamsInit.gradle", mapOf(
                "JVM_PARAMS" to params,
                "TASK_NAMES" to tasks
            ))
        }

        /**
         * These initScript functions are from org.jetbrains.plugins.gradle.service.execution.GradleInitScriptUtil
         */
        private fun loadInitScript(resourcePath: String, parameters: Map<String, String> = emptyMap()): String {
            var script = loadInitScript(InitScriptUtil::class.java, resourcePath)
            for ((key, value) in parameters) {
                val replacement = Matcher.quoteReplacement(value)
                script = script.replaceFirst(key.toRegex(), replacement)
            }
            return script
        }

        private fun loadInitScript(aClass: Class<*>, resourcePath: String): String {
            val resource = aClass.getResource(resourcePath)
                ?: throw IllegalArgumentException("Cannot find init file $resourcePath")

            try {
                return resource.readText()
            }
            catch (e: IOException) {
                throw IllegalStateException("Cannot read init file $resourcePath", e)
            }
        }
    }
}