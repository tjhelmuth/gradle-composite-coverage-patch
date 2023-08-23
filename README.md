## gradle-composite-coverage-patch


### Problem Scenario:
If you have a gradle composite build as the root project of your idea project, with subprojects that contain code, right-clicking
a test in one of the subprojects and selecting Run with Coverage will result in no coverage being reported.

For example in the following project setup:
root-project/
  - app-project/
  - shared-lib/

with the rootProject having settings.gradle:
```
rootProject.name = "root-project"

includeBuild("app-project")
includeBuild("shared-lib")
```

and you try to run a test located in app-project with coverage, the generated run configuration will have the tasks as
*:app-project:test --tests "MainTest.test"*

and the gradle project as *root-project*

One workaround for this in the IDE is to manually adjust the run configuration that is created so that the gradle project
points to *app-project* and the tasks are *:test --tests "MainTest.test"* 

With this run configuration, coverage seems to be applied correctly.

### Root Cause

**Note**: I'm not an expert on Intellij internals, but this is my understanding so far.

When Coverage is selected as a run configuration, the intellij-coverage-agent is added to the test task by manipulating 
the jvmArgs of the task configuration. The init script that does this manipulation is added by JavaGradleProjectResolver 
and looks like this  
```
gradle.taskGraph.whenReady { taskGraph ->
  taskGraph.allTasks.each { Task task ->
    if (task instanceof JavaForkOptions && (" + names + ".contains(task.name) || " + names + ".contains(task.path))) {
        def jvmArgs = task.jvmArgs.findAll{!it?.startsWith('-agentlib:jdwp') && !it?.startsWith('-Xrunjdwp')}
        jvmArgs << " + jvmArgs
        task.jvmArgs = jvmArgs
    }
  }
}
```
In a composite build, this script will run in all 3 of the includedBuilds. The problem is, in a composite build, the 
parent project that we are running from does not have access to the tasks of the includedBuilds. So taskGraph.allTasks in
the root project will have no tasks that match, and it will not apply any jvmArgs.

The sub project app-project will also run the init script, so it would make sense that it would be able to match the :test
task and apply the jvmArgs. However, the name of the task passed in is the fully qualified :app-project:test, and none
of the tasks in app-project will match that fully qualified name.

### Solution

The simplest solution I could find, which was also used in the JvmDebugInit.gradle init script is for a given project 
where the script is running, strip the composite path out of the task names before trying to match them.

So when we try to match :app-project:test while running the init script in app-project, we will strip out the :app-project
part of the task name and just match on :test. This will allow us to apply the jvmArgs to the intended task.

See /src/main/resources/JvmParamsInit.gradle for the adjusted script.