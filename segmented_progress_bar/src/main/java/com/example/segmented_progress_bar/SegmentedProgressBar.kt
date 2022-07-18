package com.example.segmented_progress_bar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.example.segmented_progress_bar.Segment.Companion.DEFAULT_DURATION_IN_MS

class SegmentedProgressBar : View, Runnable, View.OnTouchListener {
    
    /**
     * Sets callbacks for progress bar state changes
     * @see SegmentedProgressBarListener
     */
    var listener: SegmentedProgressBarListener? = null
    
    var viewPager2: ViewPager2? = null
        @SuppressLint("ClickableViewAccessibility")
        set(value) {
            field = value
            if (value == null)
                clearViewPagersCallback()
            else setViewPagersCallback()
        }
    
    private val viewPagerOnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            this@SegmentedProgressBar.setPosition(position)
        }
    }
    
    /**
     * Number of total segments to draw
     * WARMING!
     * If viewPager2 setted it's take size of itemCount
     */
    private var segmentCount: Int = resources.getInteger(R.integer.default_segments_count)
        get() = viewPager2?.adapter?.itemCount ?: field
    
    var margin: Int = resources.getDimensionPixelSize(R.dimen.default_segment_margin)
        private set
    var radius: Int = resources.getDimensionPixelSize(R.dimen.default_corner_radius)
        private set
    var segmentStrokeWidth: Int =
        resources.getDimensionPixelSize(R.dimen.default_segment_stroke_width)
        private set
    
    var segmentBackgroundColor: Int = Color.WHITE
        private set
    var segmentSelectedBackgroundColor: Int =
        context.getThemeColor(android.R.attr.colorAccent)
        private set
    var segmentStrokeColor: Int = Color.BLACK
        private set
    var segmentSelectedStrokeColor: Int = Color.BLACK
        private set
    
    private var durationPerSegmentInMs: Long = DEFAULT_DURATION_IN_MS
    
    private var segments = mutableListOf<Segment>()
    private val selectedSegment: Segment?
        get() = segments.firstOrNull { it.animationState == Segment.AnimationState.ANIMATING }
    private val selectedSegmentIndex: Int
        get() = segments.indexOf(this.selectedSegment)
    
    private val animationHandler = Handler()
    
    val strokeApplicable: Boolean
        get() = segmentStrokeWidth * 4 <= measuredHeight
    
    val segmentWidth: Float
        get() = (measuredWidth - margin * (segmentCount - 1)).toFloat() / segmentCount
    
    constructor(context: Context) : super(context)
    
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        
        val typedArray =
            context.theme.obtainStyledAttributes(attrs, R.styleable.SegmentedProgressBar, 0, 0)
        
        initSegments(List(typedArray.getInt(R.styleable.SegmentedProgressBar_totalSegments, segmentCount)) {
            Segment(durationPerSegmentInMs)
        })
        
        margin =
            typedArray.getDimensionPixelSize(
                R.styleable.SegmentedProgressBar_segmentMargins,
                margin
            )
        radius =
            typedArray.getDimensionPixelSize(
                R.styleable.SegmentedProgressBar_segmentCornerRadius,
                radius
            )
        segmentStrokeWidth =
            typedArray.getDimensionPixelSize(
                R.styleable.SegmentedProgressBar_segmentStrokeWidth,
                segmentStrokeWidth
            )
        
        segmentBackgroundColor =
            typedArray.getColor(
                R.styleable.SegmentedProgressBar_segmentBackgroundColor,
                segmentBackgroundColor
            )
        segmentSelectedBackgroundColor =
            typedArray.getColor(
                R.styleable.SegmentedProgressBar_segmentSelectedBackgroundColor,
                segmentSelectedBackgroundColor
            )
        
        segmentStrokeColor =
            typedArray.getColor(
                R.styleable.SegmentedProgressBar_segmentStrokeColor,
                segmentStrokeColor
            )
        segmentSelectedStrokeColor =
            typedArray.getColor(
                R.styleable.SegmentedProgressBar_segmentSelectedStrokeColor,
                segmentSelectedStrokeColor
            )
        
        durationPerSegmentInMs =
            typedArray.getInt(
                R.styleable.SegmentedProgressBar_timePerSegment,
                durationPerSegmentInMs.toInt()
            ).toLong()
        
        typedArray.recycle()
    }
    
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
    
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        
        segments.forEachIndexed { index, segment ->
            val drawingComponents = getDrawingComponents(segment, index)
            drawingComponents.first.forEachIndexed { drawingIndex, rectangle ->
                canvas?.drawRoundRect(
                    rectangle,
                    radius.toFloat(),
                    radius.toFloat(),
                    drawingComponents.second[drawingIndex]
                )
            }
        }
    }
    
    private fun setViewPagersCallback() {
        viewPager2?.registerOnPageChangeCallback(viewPagerOnPageChangeCallback)
        viewPager2?.getChildAt(0)?.setOnTouchListener(this)
    }
    
    private fun clearViewPagersCallback() {
        viewPager2?.unregisterOnPageChangeCallback(viewPagerOnPageChangeCallback)
        viewPager2?.getChildAt(0)?.setOnTouchListener(null)
        listener = null
    }
    
    /**
     * Set viewPagerCallbacks and start animation
     */
    fun resume(listener: SegmentedProgressBarListener) {
        this.listener = listener
        setViewPagersCallback()
        start()
    }
    
    /**
     * Start/Resume progress animation
     */
    fun start() {
        removeAnimationCallback()
        val animationUpdateTime = selectedSegment?.animationUpdateTime
        if (animationUpdateTime == null)
            next()
        else animationHandler.postDelayed(this, animationUpdateTime)
        listener?.onStart()
    }
    
    /**
     * Pauses the animation process
     */
    fun pause() {
        listener?.onPause()
        removeAnimationCallback()
    }
    
    private fun removeAnimationCallback() {
        animationHandler.removeCallbacks(this)
    }
    
    /**
     * Pauses the animation process and clear all callbacks from ViewPager2
     */
    fun stop() {
        pause()
        clearViewPagersCallback()
    }
    
    /**
     * Resets the whole animation state and selected segments
     * !Doesn't restart it!
     * To restart, call the start() method
     */
    fun reset() {
        this.segments.map { it.animationState = Segment.AnimationState.IDLE }
        this.invalidate()
    }
    
    /**
     * Starts animation for the following segment
     */
    fun next() {
        loadSegment(offset = 1, userAction = true)
    }
    
    /**
     * Starts animation for the following segment or finish if there are no more segments
     */
    fun nextOrFinish() {
        loadSegment(offset = 1, userAction = false)
    }
    
    /**
     * Starts animation for the previous segment
     */
    fun previous() {
        loadSegment(offset = -1, userAction = true)
    }
    
    /**
     * Restarts animation for the current segment
     */
    fun restartSegment() {
        setProgressOnce(0)
        restartAnimationHandler()
    }
    
    /**
     * Skips a number of segments
     * @param offset number o segments fo skip
     */
    fun skip(offset: Int) {
        loadSegment(offset = offset, userAction = true)
    }
    
    /**
     * Set selected segment position
     * @param position index
     */
    fun setPosition(position: Int) {
        loadSegment(offset = position - this.selectedSegmentIndex, userAction = true)
    }
    
    /**
     * If set once for segment, auto animation will be stopped unless the next segment is started
     * Stopped current timer
     * P.S. If you want to use your own progress timer you should set it constantly for current segment
     * e.g. for using with audio/video player with its own progress
     */
    fun setProgress(progressInMs: Long, durationInMs: Long) {
        removeAnimationCallback()
        if (durationInMs <= 0) {
            this.invalidate()
            return
        }
        selectedSegment?.setProgress(progressInMs, durationInMs)
        if (progressInMs >= durationInMs)
            loadSegment(offset = 1, userAction = false)
        else this.invalidate()
    }
    
    /**
     * For using with audio/video player better use it with duration
     * @see SegmentedProgressBar.setProgress
     * assert selectedSegment != null
     */
    fun setProgress(progressInMs: Long) {
        removeAnimationCallback()
        selectedSegment?.let {
            it.setProgress(progressInMs)
            if (progressInMs >= it.duration)
                loadSegment(offset = 1, userAction = false)
            else this.invalidate()
        }
    }
    
    /**
     * Sets progress for current segment
     * Do not stopped current timer
     * @param progress - should be less then duration
     */
    fun setProgressOnce(progressInMs: Long, durationInMs: Long) {
        selectedSegment?.setProgress(progressInMs, durationInMs)
    }
    fun setProgressOnce(progressInMs: Long) {
        selectedSegment?.setProgress(progressInMs)
    }
    
    private fun restartAnimationHandler() {
        this.invalidate()
        animationHandler.removeCallbacks(this)
        animationHandler.postDelayed(this, this.selectedSegment?.animationUpdateTime ?: durationPerSegmentInMs)
    }
    
    private fun loadSegment(offset: Int, userAction: Boolean) {
        val oldSegmentIndex = this.segments.indexOf(this.selectedSegment)
        val nextSegmentIndex = oldSegmentIndex + offset
        if (userAction && nextSegmentIndex !in 0 until segmentCount) {
            return
        }
        if (oldSegmentIndex == nextSegmentIndex)
            return
        segments.mapIndexed { index, segment ->
            when {
                offset > 0 && index < nextSegmentIndex ->
                    segment.animationState = Segment.AnimationState.ANIMATED
                offset < 0 && index > nextSegmentIndex - 1 ->
                    segment.animationState = Segment.AnimationState.IDLE
                offset == 0 && index == nextSegmentIndex ->
                    segment.animationState = Segment.AnimationState.IDLE
            }
        }
        val nextSegment = this.segments.getOrNull(nextSegmentIndex)
        removeAnimationCallback()
        if (nextSegment != null) {
            nextSegment.animationState = Segment.AnimationState.ANIMATING
            if (nextSegment.useDefaultAnimationTimer)
                animationHandler.postDelayed(this, nextSegment.animationUpdateTime)
            this.listener?.onPageSelected(oldSegmentIndex, nextSegmentIndex)
            changeItemInViewPagerIfNeeded()
        } else {
            this.listener?.onFinished()
        }
    }
    
    private fun changeItemInViewPagerIfNeeded() {
        if (viewPager2?.currentItem != this.selectedSegmentIndex)
            viewPager2?.currentItem = this.selectedSegmentIndex
    }
    
    fun initSegments(segments: List<Segment>) {
        segmentCount = segments.size
        this.segments.clear()
        this.segments.addAll(segments)
        this.invalidate()
        reset()
    }
    
    override fun run() {
        if ((this.selectedSegment?.progress() ?: 0f) >= 100f) {
            loadSegment(offset = 1, userAction = false)
        } else {
            this.invalidate()
            animationHandler.postDelayed(this, this.selectedSegment?.animationUpdateTime ?: durationPerSegmentInMs)
        }
    }
    
    override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
        listener?.onTouch(p0, p1)
        when (p1?.action){
            MotionEvent.ACTION_DOWN -> pause()
            MotionEvent.ACTION_UP -> start()
        }
        return false
    }
    
}