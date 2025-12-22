package com.ilittle7.router.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.ilittle7.router.R
import com.ilittle7.router.Router
import com.ilittle7.router.routerIntent

@Router(["/dialog/test"])
class TestDialogFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val routerIntent = routerIntent
        view.findViewById<TextView>(R.id.dialog_text).append(" ${routerIntent?.data}")
    }
}