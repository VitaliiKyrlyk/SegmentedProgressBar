package com.example.segmented_progress_bar

interface SegmentedProgressBarListener {

    fun onPageSelected(oldPageIndex: Int, newPageIndex: Int)

    fun onFinished()

    fun onPause()

    fun onStart()

}