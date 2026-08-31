package com.gtdvm.echopoint.utils

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.ReviewManager
import android.util.Log
import android.widget.Toast
import com.gtdvm.echopoint.R

//import com.google.android.play.core.tasks.Task


object Feedback {

    // function that displays a dialog for feedback on Google Play directly from the app
    fun showReviewDialog(activity: Activity, onComplete: (() -> Unit)? = null) {
        Log.d("FEEDBACK", "The feedback dialog was opened")
        val manager: ReviewManager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
//                    Toast.makeText(activity, activity.getString(R.string.MessageFeedbackforsuccesed), Toast.LENGTH_SHORT).show()
                    Log.d("FEEDBACK", "Feedback has been successfully sent")
                    onComplete?.invoke()
                }
                .addOnFailureListener {exception ->
                    Toast.makeText(activity, activity.getString(R.string.MessageFeedbackError), Toast.LENGTH_SHORT).show()
                    Log.d("FEEDBACK", "An error occurred when displaying feedback dialog: $exception.message")
                    onComplete?.invoke()
                }
            } else {
                Toast.makeText(activity, activity.getString(R.string.MessageFeedbackError), Toast.LENGTH_SHORT).show()
                Log.d("FEEDBACK", "An error occurred when sending feedback")
                onComplete?.invoke()
            }
        }
    }


}

