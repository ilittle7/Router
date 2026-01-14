package com.ilittle7.router

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class RouterIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> get() = RouterDetector.Issues.all()

    override val api: Int get() = CURRENT_API

    override val vendor: Vendor get() = Vendor(
        vendorName = "ilittle7",
        feedbackUrl = "https://github.com/ilittle7/Router/issues",
        contact = "ilittle7@163.com"
    )
}
