package com.example.segmented_progress_bar

/**
 * @param duration - duration of segment animation
 * @param useDefaultAnimationTimer - if false, the default animation timer will not be started (you should set the progress manually)
 *
 */
class Segment(duration: Long = DEFAULT_DURATION_IN_MS, val useDefaultAnimationTimer: Boolean = true) {
    
    var duration: Long = duration
        private set
    
    companion object {
        const val DEFAULT_DURATION_IN_MS = 5000L
    }
    
    val animationUpdateTime: Long
        get() = duration / 100
    
    private var animationProgress: Float = 0f
    
    var animationState: AnimationState = AnimationState.IDLE
        set(value) {
            animationProgress = when (value) {
                AnimationState.ANIMATED -> 100f
                AnimationState.IDLE -> 0f
                else -> animationProgress
            }
            field = value
        }
    
    enum class AnimationState {
        ANIMATED,
        ANIMATING,
        IDLE
    }
    
    val progressPercentage: Float
        get() = animationProgress / 100
    
    fun progress() = animationProgress++
    
    fun setProgress(progressInMs: Long, duration: Long) {
        if (this.duration != duration)
            this.duration = duration
        animationProgress = (progressInMs * 100 / duration).toFloat()
    }
    
    fun setProgress(progressInMs: Long) {
        animationProgress = (progressInMs * 100 / duration).toFloat()
    }
    
}