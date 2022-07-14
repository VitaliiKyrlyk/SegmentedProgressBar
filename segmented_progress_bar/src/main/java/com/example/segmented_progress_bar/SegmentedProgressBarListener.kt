package com.example.segmented_progress_bar

import android.view.MotionEvent
import android.view.View

interface SegmentedProgressBarListener {

    fun onPageSelected(oldPageIndex: Int, newPageIndex: Int)

    fun onFinished()

    fun onPause()

    fun onStart()
    
    fun onTouch(view: View?, motionEvent: MotionEvent?)

}