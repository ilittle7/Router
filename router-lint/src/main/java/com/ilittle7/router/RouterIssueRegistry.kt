package com.ilittle7.router

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue

class RouterIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = RouterDetector.Issues.values().map { it.issue }
}