package com.ilittle7.router

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

interface LauncherWrapper {
    val context: Context
    fun startActivity(intent: Intent)
    fun startActivityForResult(intent: Intent, requestCode: Int)
    fun startService(intent: Intent)
    fun startDialog(intent: Intent, dialogFragment: DialogFragment)
}

class ActivityLauncher(val activity: Activity) : LauncherWrapper {
    override val context = activity

    override fun startActivity(intent: Intent) {
        val activityOptions = intent.activityOptions
        intent.activityOptions = null
        if (activityOptions != null) {
            activity.startActivity(intent, activityOptions)
        } else {
            activity.startActivity(intent)
        }
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        val activityOptions = intent.activityOptions
        intent.activityOptions = null
        if (activityOptions != null) {
            activity.startActivityForResult(intent, requestCode, activityOptions)
        } else {
            activity.startActivityForResult(intent, requestCode)
        }
    }

    override fun startService(intent: Intent) {
        activity.startService(intent)
    }

    override fun startDialog(intent: Intent, dialogFragment: DialogFragment) {
        if (activity !is FragmentActivity) throw NotFragmentActivityException("${activity::class.simpleName} is not FragmentActivity, it can't start a DialogFragment")
        dialogFragment.show(activity.supportFragmentManager, null)
    }
}

private fun activityIsNull(fragment: Fragment): Nothing {
    throw java.lang.IllegalStateException("The activity of ${fragment::class.simpleName} is null.")
}

class FragmentLauncher(
    @Suppress("MemberVisibilityCanBePrivate")
    val fragment: Fragment
) : LauncherWrapper {
    override val context = fragment.activity ?: activityIsNull(fragment)

    override fun startActivity(intent: Intent) {
        val activityOptions = intent.activityOptions
        intent.activityOptions = null
        if (activityOptions != null) {
            fragment.startActivity(intent, activityOptions)
        } else {
            fragment.startActivity(intent)
        }
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        val activityOptions = intent.activityOptions
        intent.activityOptions = null
        if (activityOptions != null) {
            fragment.startActivityForResult(intent, requestCode, activityOptions)
        } else {
            fragment.startActivityForResult(intent, requestCode)
        }
    }

    override fun startService(intent: Intent) {
        fragment.activity?.startService(intent) ?: activityIsNull(fragment)
    }

    override fun startDialog(intent: Intent, dialogFragment: DialogFragment) {
        dialogFragment.show(fragment.childFragmentManager, null)
    }
}

class ContextLauncher(override val context: Context) : LauncherWrapper {
    override fun startActivity(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activityOptions = intent.activityOptions
        intent.activityOptions = null
        if (activityOptions != null) {
            context.startActivity(intent, activityOptions)
        } else {
            context.startActivity(intent)
        }
    }

    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        throw IllegalStateException("Not implement")
    }

    override fun startService(intent: Intent) {
        context.startService(intent)
    }

    override fun startDialog(intent: Intent, dialogFragment: DialogFragment) {
        if (context !is FragmentActivity) throw NotFragmentActivityException("${context::class.simpleName} is not FragmentActivity, it can't start a DialogFragment")
        dialogFragment.show(context.supportFragmentManager, null)
    }
}

class FragmentHasNoActivityException : Exception()
class NotFragmentActivityException(message: String) : Exception(message)