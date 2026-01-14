package com.ilittle7.router

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class RouterDetector : Detector(), Detector.UastScanner {
    override fun getApplicableMethodNames() =
        listOf("startActivity", "startActivityForResult", "startService")

    @Suppress("OverridingDeprecatedMember")
    override fun visitMethod(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodName = node.methodName ?: return
        infix fun PsiMethod.isMemberOf(className: String) =
            context.evaluator.isMemberInClass(this, className)

        fun reportStartActivity() = context.report(
            Issues.START_ACTIVITY,
            node,
            context.getLocation(node),
            """Use 'route' instead of 'startActivity'""",
            fix().replace()
                .name("""Replace 'startActivity' with 'route'""")
                .text(methodName)
                .with("route")
                .build()
        )

        fun reportStartActivityForResult() = context.report(
            Issues.START_ACTIVITY_FOR_RESULT,
            node,
            context.getLocation(node),
            """Use 'routeForResult' instead of 'startActivityForResult'""",
            fix().replace()
                .name("""Replace 'startActivityForResult' with 'routeForResult'""")
                .text(node.methodName)
                .with("routeForResult")
                .build()
        )

        fun reportStartService() = context.report(
            Issues.START_SERVICE,
            node,
            context.getLocation(node),
            """Use 'route' instead of 'startService' with an uri or intent"""
        )

        when {
            methodName == "startActivity" && method isMemberOf "android.app.Activity" -> reportStartActivity()
            methodName == "startActivity" && method isMemberOf "android.content.Context" -> reportStartActivity()
            methodName == "startActivity" && method isMemberOf "android.content.ContextWrapper" -> reportStartActivity()
            methodName == "startActivity" && method isMemberOf "androidx.fragment.app.Fragment" -> reportStartActivity()
            methodName == "startActivityForResult" && method isMemberOf "android.app.Activity" -> reportStartActivityForResult()
            methodName == "startActivityForResult" && method isMemberOf "androidx.fragment.app.FragmentActivity" -> reportStartActivityForResult()
            methodName == "startActivityForResult" && method isMemberOf "androidx.fragment.app.Fragment" -> reportStartActivityForResult()
            methodName == "startService" && method isMemberOf "android.content.ContextWrapper" -> reportStartService()
            methodName == "startService" && method isMemberOf "android.content.Context" -> reportStartService()
        }
    }

    object Issues {
        val START_ACTIVITY = Issue.create(
            "RouterStartActivity",
            "Use router to start activity.",
            "Use router to start the activity.",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            Implementation(RouterDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val START_ACTIVITY_FOR_RESULT = Issue.create(
            "RouterStartActivityForResult",
            "Use router to start activity for result.",
            "Use Router to start the activity for result.",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            Implementation(RouterDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )

        val START_SERVICE = Issue.create(
            "RouterStartService",
            "Use router to start the service.",
            "Use router to start the service.",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            Implementation(RouterDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
        
        fun all() = listOf(START_ACTIVITY, START_ACTIVITY_FOR_RESULT, START_SERVICE)
    }
}
