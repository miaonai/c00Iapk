package com.example.c001apk.testing

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Offline background for testing the real translucent ReplyActivity across windows. */
class ReplyScrollTestActivity : Activity() {
    lateinit var feed: RecyclerView
    var replies: RecyclerView? = null
    private var sheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feed = createList()
        setContentView(feed)
    }

    fun showReplies() {
        val list = createList()
        replies = list
        sheet = BottomSheetDialog(this).apply {
            setContentView(list)
            show()
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun createList() = RecyclerView(this).apply {
        layoutParams = ViewGroup.LayoutParams(-1, -1)
        layoutManager = LinearLayoutManager(this@ReplyScrollTestActivity)
        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = 100
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                    layoutParams = RecyclerView.LayoutParams(-1, 160)
                    // Like the real comment rows: focusable, but not in touch mode.
                    isFocusable = true
                    isClickable = true
                }) {}

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                (holder.itemView as TextView).text = "Comment $position"
            }
        }
    }

    override fun onDestroy() {
        sheet?.dismiss()
        super.onDestroy()
    }
}
