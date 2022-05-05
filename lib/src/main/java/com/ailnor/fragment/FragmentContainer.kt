/* 
 * Copyright Erkinjanov Anaskhan, 12/02/2022.
 */

package com.ailnor.fragment

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.ailnor.core.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FragmentContainer(context: Context) : FrameLayout(context) {

    private var frameAnimationFinishRunnable: Runnable? = null
    private var animationType = FROM_RIGHT
    private var inAnimation = false
    private var isKeyboardVisible = false
    private val rect = Rect()

    companion object {
        const val FROM_RIGHT = 1
        const val FROM_LEFT = 2
        const val FROM_RIGHT_FLOATING = 3
    }

    private class Container(context: Context) : FrameLayout(context) {

        init {
            setBackgroundColor(Theme.white)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val height = MeasureSpec.getSize(heightMeasureSpec)
            var actionBarHeight = 0

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is ActionBar) {
                    child.measure(measureSpec_exactly(width), measureSpec_unspecified)
                    actionBarHeight = child.measuredHeight
                    break
                }
            }

            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child !is ActionBar)
                    measureChildWithMargins(
                        child,
                        widthMeasureSpec,
                        0,
                        heightMeasureSpec,
                        actionBarHeight
                    )
            }

            setMeasuredDimension(
                width, height
            )
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            val count = childCount
            var actionBarHeight = 0
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child is ActionBar) {
                    actionBarHeight = child.measuredHeight
                    child.layout(0, 0, measuredWidth, actionBarHeight)
                    break
                }
            }
            for (i in 0 until count) {
                val child = getChildAt(i)
                if (child !is ActionBar) {
                    val layoutParams = child.layoutParams as LayoutParams
                    child.layout(
                        layoutParams.leftMargin,
                        layoutParams.topMargin + actionBarHeight,
                        layoutParams.leftMargin + child.measuredWidth,
                        layoutParams.topMargin + actionBarHeight + child.measuredHeight
                    )
                }
            }
        }

    }

    private inner class GroupContainer(context: Context) : FrameLayout(context) {

        private var leftFrame = Container(context)
        private var rightFrame: Container? = null
        private var frame: Container? = null

        init {
            addView(leftFrame, Params(1f, 0))
        }

        fun addGroup(view: View, actionBar: ActionBar?) {
            if (rightFrame != null)
                (rightFrame!!.layoutParams as Params).update(0f, 0)
            if (frame != null)
                (frame!!.layoutParams as Params).update(0f, 0)
            (leftFrame.layoutParams as Params).update(1f, 0)
            requestLayout()
            leftFrame.addView(view)
            if (actionBar != null)
                leftFrame.addView(actionBar)
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val height = MeasureSpec.getSize(heightMeasureSpec)
            val width = MeasureSpec.getSize(widthMeasureSpec)

            val leftFrameParams = leftFrame.layoutParams as Params
            if (inAnimation) {
                var availableWidth = width
                if (animationType == FROM_RIGHT) {
                    leftFrame.measure(
                        measureSpec_exactly((leftFrameParams.weight * width).toInt()),
                        heightMeasureSpec
                    )
                    availableWidth -= leftFrame.measuredWidth
                    if (rightFrame != null) {
                        val frameParams = frame?.layoutParams as? Params
                        val rightFrameParams = rightFrame!!.layoutParams as Params
                        rightFrame!!.measure(
                            measureSpec_exactly(if (frameParams == null || frameParams.weight == 0f) availableWidth else (width * rightFrameParams.weight).toInt()),
                            heightMeasureSpec
                        )
                        availableWidth -= rightFrame!!.measuredWidth
                        if (frameParams != null) {
                            frame!!.measure(
                                measureSpec_exactly((width * frameParams.weight).toInt()),
                                heightMeasureSpec
                            )
                        }
                    }
                } else if (animationType == FROM_LEFT) {
                    val frameParams = frame!!.layoutParams as Params
                    frame!!.measure(
                        measureSpec_exactly((width * frameParams.weight).toInt()),
                        heightMeasureSpec
                    )
                    availableWidth -= frame!!.measuredWidth
                    val rightFrameParams = rightFrame!!.layoutParams as Params
                    leftFrame.measure(
                        measureSpec_exactly(if (rightFrameParams.weight == 0f) availableWidth else (width * leftFrameParams.weight).toInt()),
                        heightMeasureSpec
                    )
                    availableWidth -= leftFrame.measuredWidth
                    rightFrame!!.measure(
                        measureSpec_exactly((width * rightFrameParams.weight).toInt()),
                        heightMeasureSpec
                    )
                } else {
                    val frameParams = frame!!.layoutParams as Params
                    frame!!.measure(
                        measureSpec_exactly((width * frameParams.weight).toInt()),
                        heightMeasureSpec
                    )
                    leftFrame.measure(
                        measureSpec_exactly((width * leftFrameParams.weight).toInt()),
                        heightMeasureSpec
                    )
                    rightFrame!!.measure(
                        measureSpec_exactly(width - leftFrame.measuredWidth),
                        heightMeasureSpec
                    )
                }
            } else {
                if (leftFrameParams.weight > 0.5f) {
                    leftFrame.measure(
                        widthMeasureSpec,
                        heightMeasureSpec
                    )
                    rightFrame?.measure(
                        measureSpec_exactly(0),
                        measureSpec_exactly(0)
                    )
                } else {
                    leftFrame.measure(
                        measureSpec_exactly((width * 0.35f).toInt()),
                        heightMeasureSpec
                    )
                    rightFrame?.measure(
                        measureSpec_exactly(width - leftFrame.measuredWidth),
                        heightMeasureSpec
                    )
                }
            }

            setMeasuredDimension(
                width,
                height
            )
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            var l = 0

            val leftFrameParams = leftFrame.layoutParams as Params
            if (inAnimation) {
                if (animationType == FROM_RIGHT) {
                    l += leftFrameParams.leftOffset
                    leftFrame.layout(
                        l, 0, l + leftFrame.measuredWidth, leftFrame.measuredHeight
                    )
                    l += leftFrame.measuredWidth
                    if (rightFrame != null) {
                        rightFrame!!.layout(
                            l, 0, l + rightFrame!!.measuredWidth, rightFrame!!.measuredHeight
                        )
                        l += rightFrame!!.measuredWidth
                        if (frame != null) {
                            frame!!.layout(
                                l, 0, l + frame!!.measuredWidth, frame!!.measuredHeight
                            )
                        }
                    }
                } else if (animationType == FROM_LEFT) {
                    val frameParams = frame!!.layoutParams as Params
                    l += frameParams.leftOffset
                    frame!!.layout(
                        l, 0, l + frame!!.measuredWidth, frame!!.measuredHeight
                    )
                    l += frame!!.measuredWidth
                    leftFrame.layout(
                        l, 0, l + leftFrame.measuredWidth, leftFrame.measuredHeight
                    )
                    l += leftFrame.measuredWidth
                    rightFrame!!.layout(
                        l, 0, l + rightFrame!!.measuredWidth, rightFrame!!.measuredHeight
                    )
                } else {
                    leftFrame.layout(
                        0, 0, leftFrame.measuredWidth, leftFrame.measuredHeight
                    )
                    rightFrame!!.layout(
                        leftFrame.measuredWidth, 0, measuredWidth, rightFrame!!.measuredHeight
                    )
                    val frameParams = frame!!.layoutParams as Params
                    frame!!.layout(
                        frameParams.leftOffset,
                        0,
                        frameParams.leftOffset + frame!!.measuredWidth,
                        frame!!.measuredHeight
                    )
                }
            } else {
                if (leftFrameParams.weight > 0.5f) {
                    leftFrame.layout(
                        0, 0, measuredWidth, measuredHeight
                    )
                    rightFrame?.layout(
                        0, 0, 0, 0
                    )
                } else {
                    leftFrame.layout(
                        0, 0, leftFrame.measuredWidth, measuredHeight
                    )
                    rightFrame?.layout(
                        leftFrame.measuredWidth, 0, measuredWidth, measuredHeight
                    )
                }
            }

            val rootView = rootView
            getWindowVisibleDisplayFrame(rect)
            val usableViewHeight: Int =
                rootView.height - (if (rect.top != 0) Utilities.statusBarHeight else 0) - Utilities.getViewInset(
                    rootView
                )
            isKeyboardVisible = usableViewHeight - (rect.bottom - rect.top) > 0
            if (waitingForKeyboardCloseRunnable != null && isKeyboardVisible) {
//                cancelRunOnUIThread(waitingForKeyboardCloseRunnable)
                waitingForKeyboardCloseRunnable!!.run()
                waitingForKeyboardCloseRunnable = null
            }
        }


        fun nextScreen(view: View, actionBar: ActionBar?, forceWithoutAnimation: Boolean) {
            if (forceWithoutAnimation) {
                (leftFrame.layoutParams as Params).update(0.35f, 0)
                if (rightFrame == null) {
                    rightFrame = Container(context)
                    addView(rightFrame, Params(0.65f, 0))
                } else
                    (rightFrame!!.layoutParams as Params).update(0.65f, 0)
                if (oldFragment != null) {
                    pauseFragment(oldFragment!!)
                    oldFragment = null
                    val temp = leftFrame
                    leftFrame = rightFrame!!
                    rightFrame = temp
                    val tempParams = rightFrame!!.layoutParams
                    rightFrame!!.layoutParams = leftFrame.layoutParams
                    leftFrame.layoutParams = tempParams
                }
                rightFrame!!.addView(view)
                if (actionBar != null)
                    rightFrame!!.addView(actionBar)
                newFragment!!.onBecomeFullyVisible()
                newFragment!!.resume()
                newFragment!!.onGetFirstInStack()
                newFragment = null
            } else if (oldFragment == null)
                startFirstNextAnimation(view, actionBar)
            else
                startNextAnimation(view, actionBar)

        }

        private fun startFirstNextAnimation(view: View, actionBar: ActionBar?) {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_RIGHT

            if (rightFrame == null) {
                rightFrame = Container(context)
                addView(rightFrame, Params(0.65f, 0))
            } else
                (rightFrame!!.layoutParams as Params).update(0.65f, 0)
            rightFrame!!.addView(view)
            if (actionBar != null)
                rightFrame!!.addView(actionBar)

            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                inAnimation = false
                                thisInAnimation = false
                                (leftFrame.layoutParams as Params).update(0.35f, 0)
                                (rightFrame!!.layoutParams as Params).update(0.65f, 0)
                                requestLayout()
                                newFragment!!.onBecomeFullyVisible()
                                newFragment!!.resume()
                                newFragment!!.onGetFirstInStack()
                                newFragment = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(
                        interpolatedTime: Float,
                        t: Transformation?
                    ) {
                        if (thisInAnimation) {
                            (leftFrame.layoutParams as Params).weight =
                                1f - interpolatedTime * 0.65f
                            requestLayout()
                        }
                    }
                }
            )
        }

        private fun startNextAnimation(view: View, actionBar: ActionBar?) {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_RIGHT

            if (frame == null) {
                frame = Container(context)
                frame!!.layoutParams = Params(0.65f, 0)
            } else
                (frame!!.layoutParams as Params).update(0.65f, 0)
            frame!!.addView(view)
            addView(frame)
            if (actionBar != null)
                frame!!.addView(actionBar)

            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                val temp = leftFrame
                                leftFrame = rightFrame!!
                                rightFrame = frame
                                frame = temp
                                inAnimation = false
                                thisInAnimation = false
                                removeViewInLayout(frame!!)
                                (leftFrame.layoutParams as Params).update(0.35f, 0)
                                (rightFrame!!.layoutParams as Params).update(0.65f, 0)
                                requestLayout()
                                pauseFragment(oldFragment!!)
                                oldFragment = null
                                newFragment!!.onBecomeFullyVisible()
                                newFragment!!.resume()
                                newFragment!!.onGetFirstInStack()
                                newFragment = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(
                        interpolatedTime: Float,
                        t: Transformation?
                    ) {
                        if (thisInAnimation) {
                            (leftFrame.layoutParams as Params).update(
                                0.35f - (0.15f) * interpolatedTime,
                                -(measuredWidth * 0.20 * interpolatedTime).toInt()
                            )
                            (rightFrame!!.layoutParams as Params).weight =
                                0.65f - (0.30f) * interpolatedTime
                            requestLayout()
                        }
                    }
                }
            )
        }

        fun previousScreen(view: View?, actionBar: ActionBar?, forceWithoutAnimation: Boolean) {
            if (forceWithoutAnimation) {
                pauseFragment(oldFragment!!)
                oldFragment = null
                if (view == null) {
                    (leftFrame.layoutParams as Params).update(1f, 0)
                    newFragment!!.onGetFirstInStack()
                    newFragment = null
                } else {
                    val temp = rightFrame
                    rightFrame = leftFrame
                    leftFrame = temp!!

                    val tempParams = leftFrame.layoutParams
                    leftFrame.layoutParams = rightFrame!!.layoutParams
                    rightFrame!!.layoutParams = tempParams
                    rightFrame!!.addView(view)
                    if (actionBar != null)
                        rightFrame!!.addView(actionBar)

                    newFragment!!.onBecomeFullyVisible()
                    newFragment!!.resume()
                    newFragment = null

                    newFragment2!!.onGetFirstInStack()
                    newFragment2 = null
                }
                requestLayout()
            } else if (view == null)
                startLastPreviousAnimation()
            else
                startPreviousAnimation(view, actionBar)
        }

        private fun startLastPreviousAnimation() {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_LEFT

            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                inAnimation = false
                                thisInAnimation = false
                                (leftFrame.layoutParams as Params).update(1f, 0)
                                requestLayout()
                                pauseFragment(oldFragment!!)
                                oldFragment = null
                                newFragment!!.onGetFirstInStack()
                                newFragment = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(
                        interpolatedTime: Float,
                        t: Transformation?
                    ) {
                        if (thisInAnimation) {
                            (leftFrame.layoutParams as Params).weight =
                                0.35f + interpolatedTime * 0.65f
                            requestLayout()
                        }
                    }
                }
            )
        }

        private fun startPreviousAnimation(view: View, actionBar: ActionBar?) {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_LEFT

            if (frame == null) {
                frame = Container(context)
                frame!!.layoutParams = Params(0.20f, (-(measuredWidth * 0.20f)).toInt())
            } else
                (frame!!.layoutParams as Params).update(0.20f, -(measuredWidth * 0.20f).toInt())
            frame!!.addView(view)
            addView(frame)
            if (actionBar != null)
                frame!!.addView(actionBar)
            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                val temp = rightFrame
                                rightFrame = leftFrame
                                leftFrame = frame!!
                                frame = temp
                                inAnimation = false
                                thisInAnimation = false
                                removeViewInLayout(frame!!)
                                (leftFrame.layoutParams as Params).update(0.35f, 0)
                                (rightFrame!!.layoutParams as Params).update(0.65f, 0)
                                requestLayout()
                                pauseFragment(oldFragment!!)
                                oldFragment = null
                                newFragment!!.onBecomeFullyVisible()
                                newFragment!!.resume()
                                newFragment = null
                                newFragment2!!.onGetFirstInStack()
                                newFragment2 = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(
                        interpolatedTime: Float,
                        t: Transformation?
                    ) {
                        if (thisInAnimation) {
                            (frame!!.layoutParams as Params).update(
                                0.20f + 0.15f * interpolatedTime,
                                -(measuredWidth * 0.20f * (1 - interpolatedTime)).toInt()
                            )
                            (leftFrame.layoutParams as Params).weight =
                                0.35f + 0.30f * interpolatedTime
                            requestLayout()
                        }
                    }
                }
            )
        }

        fun openLeft(view: View, actionBar: ActionBar?) {
            startOpenLeftAnimation(view, actionBar)
        }

        private fun startOpenLeftAnimation(view: View, actionBar: ActionBar?) {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_LEFT

            if (rightFrame == null) {
                rightFrame = Container(context)
                addView(rightFrame, Params(0f, 0))
            } else
                (rightFrame!!.layoutParams as Params).update(0f, 0)

            if (frame == null) {
                frame = Container(context)
                frame!!.layoutParams = Params(0.20f, -(measuredWidth * 0.20f).toInt())
            } else
                (frame!!.layoutParams as Params).update(0.20f, -(measuredWidth * 0.20f).toInt())

            frame!!.addView(view)
            addView(frame)
            if (actionBar != null)
                frame!!.addView(actionBar)

            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                val temp = rightFrame
                                rightFrame = leftFrame
                                leftFrame = frame!!
                                frame = temp
                                inAnimation = false
                                thisInAnimation = false
                                removeViewInLayout(frame!!)
                                (leftFrame.layoutParams as Params).update(0.35f, 0)
                                (rightFrame!!.layoutParams as Params).update(0.65f, 0)
                                requestLayout()
                                newFragment!!.onBecomeFullyVisible()
                                newFragment!!.resume()
                                newFragment!!.onGetFirstInStack()
                                newFragment = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                        if (thisInAnimation) {
                            (frame!!.layoutParams as Params).update(
                                0.20f + 0.15f * interpolatedTime,
                                -(measuredWidth * 0.20f * (1 - interpolatedTime)).toInt()
                            )
                            (leftFrame.layoutParams as Params).weight =
                                0.35f + 0.30f * interpolatedTime
                            requestLayout()
                        }
                    }
                }
            )
        }

        fun closeLeft() {
            startCloseLeftAnimation()
        }

        private fun startCloseLeftAnimation() {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_RIGHT

            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                val temp = rightFrame
                                rightFrame = leftFrame
                                leftFrame = temp!!
                                inAnimation = false
                                thisInAnimation = false
                                (leftFrame.layoutParams as Params).update(1f, 0)
                                (rightFrame!!.layoutParams as Params).update(0f, 0)
                                requestLayout()
                                pauseFragment(oldFragment!!)
                                oldFragment = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                        if (thisInAnimation) {
                            (leftFrame.layoutParams as Params).update(
                                0.35f - 0.15f * interpolatedTime,
                                -(measuredWidth * 0.20f * interpolatedTime).toInt()
                            )
                            (rightFrame!!.layoutParams as Params).weight =
                                0.65f + 0.35f * interpolatedTime
                        }
                    }
                }
            )
        }


        fun replaceRight(view: View, actionBar: ActionBar?) {
            startReplaceAnimation(view, actionBar)
        }

        private fun startReplaceAnimation(view: View, actionBar: ActionBar?) {
            inAnimation = true
            var thisInAnimation = true
            animationType = FROM_RIGHT_FLOATING

            if (frame == null) {
                frame = Container(context)
                frame!!.layoutParams = Params(0.65f, measuredWidth)
            } else
                (frame!!.layoutParams as Params).update(0.65f, measuredWidth)
            frame!!.addView(view)
            addView(frame)
            if (actionBar != null)
                frame!!.addView(actionBar)

            startAnimation(
                object : Animation() {
                    init {
                        duration = 300
                        setAnimationListener(object : AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                                val temp = rightFrame
                                rightFrame = frame!!
                                frame = temp
                                inAnimation = false
                                thisInAnimation = false
                                removeViewInLayout(frame!!)
                                (rightFrame!!.layoutParams as Params).leftOffset = 0
                                requestLayout()

                                finishFragment(oldFragment!!)
                                oldFragment = null
                                resumeFragment(newFragment!!)
                                newFragment = null
                                frameAnimationFinishRunnable?.run()
                            }

                            override fun onAnimationRepeat(animation: Animation?) {
                            }
                        })
                    }

                    override fun applyTransformation(
                        interpolatedTime: Float,
                        t: Transformation?
                    ) {
                        if (thisInAnimation) {
                            (frame!!.layoutParams as Params).leftOffset =
                                measuredWidth - (measuredWidth * 0.65f * interpolatedTime).toInt()
                            requestLayout()
                        }
                    }
                }
            )
        }
    }

    private class Params(var weight: Float, var leftOffset: Int) :
        FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT) {
        fun update(weight: Float, leftOffset: Int) {
            this.weight = weight
            this.leftOffset = leftOffset
        }
    }

    private var waitingForKeyboardCloseRunnable: Runnable? = null
//    private val delayedOpenAnimationRunnable: Runnable? = null

    private var containerView = GroupContainer(context)
    private var containerViewBack = GroupContainer(context)
    private var currentAnimationSet: AnimatorSet? = null
    private var interpolator = FastOutLinearInInterpolator()
    private var currentGroupId = 0

    private var oldFragment: Fragment? = null
    private var newFragment: Fragment? = null
    private var newFragment2: Fragment? = null

    val parentActivity: AppCompatActivity = context as AppCompatActivity

    private val fragmentStack = arrayListOf<Fragment>()

    init {
        addView(containerViewBack)
        addView(containerView)
    }

    private fun resumeFragment(fragment: Fragment){
        fragment.onBecomeFullyVisible()
        fragment.resume()
        fragment.onGetFirstInStack()
    }

    private fun pauseFragment(fragment: Fragment) {
//        fragment.onBecomeFullyHidden()
        fragment.pause()
        if (fragment.savedView != null) {
            val parent = fragment.savedView?.parent as? ViewGroup
            if (parent != null) {
                fragment.onRemoveFromParent()
                try {
                    parent.removeViewInLayout(fragment.savedView)
                } catch (e: Exception) {
                    try {
                        parent.removeView(fragment.savedView)
                    } catch (e2: Exception) {
                    }
                }
            }

            if (fragment.actionBar != null && fragment.requiredActionBar.shouldAddToContainer) {
                val actionBarParent = fragment.requiredActionBar.parent as? ViewGroup
                actionBarParent?.removeViewInLayout(fragment.actionBar)
            }
        }
    }

    private fun finishFragment(fragment: Fragment) {
        //        fragment.onBecomeFullyHidden()
        fragment.pause()
        if (fragment.savedView != null) {
            val parent = fragment.savedView?.parent as? ViewGroup
            if (parent != null) {
                fragment.onRemoveFromParent()
                try {
                    parent.removeViewInLayout(fragment.savedView)
                } catch (e: Exception) {
                    try {
                        parent.removeView(fragment.savedView)
                    } catch (e2: Exception) {
                    }
                }
            }

            if (fragment.actionBar != null && fragment.requiredActionBar.shouldAddToContainer) {
                val actionBarParent = fragment.requiredActionBar.parent as? ViewGroup
                actionBarParent?.removeViewInLayout(fragment.actionBar)
            }
        }
        fragment.onFragmentDestroy()
        fragment.parentLayout = null
        fragmentStack.remove(fragment)
    }

    private fun presentScreenInternalRemoveOld(
        groupRemoved: Boolean,
        removeLast: Boolean,
        resumeLast: Boolean,
        getFirstStackLast: Boolean,
        fragment: Fragment?
    ) {
        if (fragment == null) {
            return
        }
//        fragment.onBecomeFullyHidden()
        fragment.pause()
        if (removeLast) {
            if (groupRemoved)
                currentGroupId--
            fragment.onFragmentDestroy()
            fragment.parentLayout = null
            fragmentStack.remove(fragment)
            if (fragmentStack.size != 0) {
                val oldScreen = fragmentStack[fragmentStack.size - 1]
                if (resumeLast)
                    oldScreen.resume()
                if (getFirstStackLast)
                    oldScreen.onGetFirstInStack()
            }
        } else {
            if (fragment.savedView != null) {
                val parent = fragment.savedView?.parent as? ViewGroup
                if (parent != null) {
                    fragment.onRemoveFromParent()
                    try {
                        parent.removeViewInLayout(fragment.savedView)
                    } catch (e: Exception) {
                        try {
                            parent.removeView(fragment.savedView)
                        } catch (e2: Exception) {
                        }
                    }
                }
            }
            if (fragment.actionBar != null && fragment.requiredActionBar.shouldAddToContainer) {
                val parent = fragment.requiredActionBar.parent as? ViewGroup
                parent?.removeViewInLayout(fragment.actionBar)
            }
        }
        containerViewBack.visibility = GONE
    }

    fun presentFragmentGroup(
        screen: Fragment,
        removeLast: Boolean = false,
        forceWithoutAnimation: Boolean = false,
    ): Boolean {
        return presentFragment(screen, true, removeLast, forceWithoutAnimation)
    }

    fun presentFragment(
        screen: Fragment,
        newGroup: Boolean,
        removeLast: Boolean = false,
        forceWithoutAnimation: Boolean = false
    ): Boolean {
        return if (inAnimation && frameAnimationFinishRunnable == null) {
            frameAnimationFinishRunnable = Runnable {
                presentFragmentInternal(screen, newGroup, removeLast, forceWithoutAnimation)
            }
            false
        } else
            presentFragmentInternal(screen, newGroup, removeLast, forceWithoutAnimation)
    }

    private fun presentFragmentInternal(fragment: Fragment, newGroup: Boolean, removeLast: Boolean, forceWithoutAnimation: Boolean): Boolean{
        if (!fragment.onScreenCreate())
            return false

        if (parentActivity.currentFocus != null && fragment.hideKeyboardOnShow())
            parentActivity.currentFocus.hideKeyboard()

        if (newGroup)
            currentGroupId++
        fragment.groupId = currentGroupId
        fragment.parentLayout = this

        var screenView = fragment.savedView
        if (screenView != null) {
            val parent = screenView.parent as? ViewGroup
            if (parent != null) {
                fragment.onRemoveFromParent()
                parent.removeView(screenView)
            }
        } else
            screenView = fragment.createView(context)
        fragment.onViewCreated()

        if (fragment.actionBar != null && fragment.requiredActionBar.shouldAddToContainer) {
            val parent = fragment.requiredActionBar.parent as? ViewGroup
            parent?.removeView(fragment.actionBar)
            containerViewBack.addGroup(screenView, fragment.requiredActionBar)
        } else
            containerViewBack.addGroup(screenView, null)

        fragmentStack.add(fragment)

        val temp = containerView
        containerView = containerViewBack
        containerViewBack = temp

        if (forceWithoutAnimation) {
            containerView.translationX = 0f
            containerViewBack.visibility = GONE
            containerView.visibility = View.VISIBLE
            bringChildToFront(containerView)
            if (fragmentStack.size > 1) {
                val oldFragment = fragmentStack[fragmentStack.size - 2]
                if(removeLast)
                    finishFragment(oldFragment)
                else
                    pauseFragment(oldFragment)
            }
            resumeFragment(fragment)
        } else {
            containerView.translationX = measuredWidth * 0.5f
            containerView.alpha = 0f
            containerView.visibility = VISIBLE
            containerView.elevation = 8f
            bringChildToFront(containerView)

            currentAnimationSet = AnimatorSet()
            currentAnimationSet!!.duration = 200
            val alphaAnimation = ObjectAnimator.ofFloat(containerView, View.ALPHA, 0.8f, 1.0f)
            val translationXAnimation = ObjectAnimator.ofFloat(containerView, View.TRANSLATION_X, containerView.measuredWidth * 0.3f, 0f)

            currentAnimationSet!!.addListener(object: Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    containerView.elevation = 0f
                    if (fragmentStack.size > 1) {
                        val oldFragment = fragmentStack[fragmentStack.size - 2]
                        if(removeLast)
                            finishFragment(oldFragment)
                        else
                            pauseFragment(oldFragment)
                    }
                    resumeFragment(fragment)
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationRepeat(animation: Animator?) {

                }

            })

            currentAnimationSet!!.playTogether(translationXAnimation, alphaAnimation)
            currentAnimationSet!!.start()
        }

        return true
    }

    fun nextScreen(
        fragment: Fragment,
        removeLast: Boolean,
        forceWithoutAnimation: Boolean = false
    ): Boolean {
       return if (!Utilities.isLandscapeTablet)
           presentFragment(fragment, false, removeLast, forceWithoutAnimation)
        else if(inAnimation && frameAnimationFinishRunnable == null){
           frameAnimationFinishRunnable = Runnable {
               nextScreenInternal(fragment, removeLast, forceWithoutAnimation)
           }
           false
       } else
           nextScreenInternal(fragment, removeLast, forceWithoutAnimation)
    }

    private fun nextScreenInternal(fragment: Fragment, removeLast: Boolean, forceWithoutAnimation: Boolean): Boolean{
        if (!fragment.onScreenCreate()) {
            return false
        }

        fragment.groupId = currentGroupId
        fragment.parentLayout = this
        var screenView = fragment.savedView
        if (screenView != null) {
            val parent = screenView.parent as? ViewGroup
            if (parent != null) {
                fragment.onRemoveFromParent()
                parent.removeView(screenView)
            }
        } else
            screenView = fragment.createView(context)

        newFragment = fragment

        oldFragment = if (fragmentStack.size > 1) {
            if (removeLast)
                fragmentStack[fragmentStack.size - 1]
            else if (fragmentStack[fragmentStack.size - 2].groupId == currentGroupId)
                fragmentStack[fragmentStack.size - 2]
            else
                null
        } else
            null

        fragmentStack.add(fragment)

        if (fragment.actionBar != null && fragment.requiredActionBar.shouldAddToContainer) {
            val parent = fragment.requiredActionBar.parent as? ViewGroup
            parent?.removeView(fragment.actionBar)
            if (removeLast)
                containerView.replaceRight(screenView, fragment.actionBar)
            else
                containerView.nextScreen(screenView, fragment.actionBar, false)
        } else if (removeLast)
            containerView.replaceRight(screenView, null)
        else
            containerView.nextScreen(screenView, null, false)
        return true
    }

    fun presentAsSheet(screen: Fragment) {
        screen.parentLayout = this
        screen.groupId = -2
        fragmentStack.add(screen)
        BottomSheet(screen).show(parentActivity.supportFragmentManager, "Sheet")
    }

    fun addFragmentToStack(screen: Fragment, newGroup: Boolean = true): Boolean {
        return addFragmentToStack(screen, newGroup, -1)
    }

    fun addFragmentToStack(screen: Fragment, newGroup: Boolean, position: Int): Boolean {
        if (!screen.onScreenCreate()) {
            return false
        }
        if (newGroup)
            currentGroupId++
        screen.groupId = currentGroupId
        screen.parentLayout = this
        if (position == -1) {
            if (fragmentStack.isNotEmpty()) {
                val previousFragment: Fragment = fragmentStack[fragmentStack.size - 1]
                previousFragment.pause()
                if (previousFragment.actionBar != null && previousFragment.requiredActionBar.shouldAddToContainer) {
                    val parent = previousFragment.requiredActionBar.parent as? ViewGroup
                    parent?.removeView(previousFragment.actionBar)
                }
                if (previousFragment.savedView != null) {
                    val parent = previousFragment.savedView?.parent as? ViewGroup
                    if (parent != null) {
                        previousFragment.onRemoveFromParent()
                        parent.removeView(previousFragment.savedView)
                    }
                }
            }
            fragmentStack.add(screen)
        } else {
            fragmentStack.add(position, screen)
        }
        return true
    }

    fun showLastFragment() {
        if (fragmentStack.isEmpty()) {
            return
        }
        for (a in 0 until fragmentStack.size - 1) {
            val previousFragment: Fragment = fragmentStack.get(a)
            if (previousFragment.actionBar != null && previousFragment.requiredActionBar.shouldAddToContainer) {
                val parent = previousFragment.requiredActionBar.parent as ViewGroup
                parent.removeView(previousFragment.actionBar)
            }
            if (previousFragment.savedView != null) {
                val parent = previousFragment.savedView!!.parent as? ViewGroup
                if (parent != null) {
                    previousFragment.onPause()
                    previousFragment.onRemoveFromParent()
                    parent.removeView(previousFragment.savedView)
                }
            }
        }
        val previousFragment: Fragment = fragmentStack[fragmentStack.size - 1]
        previousFragment.parentLayout = this
        var fragmentView: View? = previousFragment.savedView
        if (fragmentView == null) {
            fragmentView = previousFragment.createView(parentActivity)
        } else {
            val parent = fragmentView.parent as? ViewGroup
            if (parent != null) {
                previousFragment.onRemoveFromParent()
                parent.removeView(fragmentView)
            }
        }
        containerView.addView(fragmentView)
        if (previousFragment.actionBar != null && previousFragment.requiredActionBar.shouldAddToContainer) {
            val parent = previousFragment.requiredActionBar.parent as? ViewGroup
            parent?.removeView(previousFragment.actionBar)
            containerView.addView(previousFragment.actionBar)
        }
        previousFragment.onResume()
    }

    fun closeLastScreen(animated: Boolean = true) {
//        if (inPreviousAnimation || inNextAnimation || inReplaceAnimation)
//            return
        currentAnimationSet?.cancel()

        val currentScreen = fragmentStack[fragmentStack.size - 1]

        if (currentScreen.groupId == -2) {
            (parentActivity.supportFragmentManager.findFragmentByTag("Sheet") as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
        } else if (fragmentStack.size != 1) {
            val groupRemoved: Boolean
            if (fragmentStack[fragmentStack.size - 2].groupId == currentGroupId) {
                groupRemoved = false
                if (Utilities.isLandscapeTablet) {
                    oldFragment = currentScreen
                    if (fragmentStack.size > 2 && fragmentStack[fragmentStack.size - 3].groupId == currentGroupId) {
                        val preScreen = fragmentStack[fragmentStack.size - 3]
                        var preView = preScreen.savedView
                        newFragment = preScreen
                        if (preView != null) {
                            val parent = preView.parent as? ViewGroup
                            parent?.removeView(preView)
                        } else
                            preView = preScreen.createView(context)
                        if (preScreen.actionBar != null && preScreen.requiredActionBar.shouldAddToContainer) {
                            val parent = preScreen.requiredActionBar.parent as? ViewGroup
                            parent?.removeView(preScreen.actionBar)
                        }
                        containerView.previousScreen(
                            preView,
                            preScreen.actionBar,
                            forceWithoutAnimation = true
                        )
                    } else {
                        newFragment = null
                        containerView.previousScreen(null, null, forceWithoutAnimation = true)
                    }
                    return
                }
            } else
                groupRemoved = true

            val screen = fragmentStack[fragmentStack.size - 2]

            var rightView: View? = null
            var rightActionBar: ActionBar? = null
            if (Utilities.isLandscapeTablet && fragmentStack.size > 2 && fragmentStack[fragmentStack.size - 3].groupId == screen.groupId) {
                val rightScreen = fragmentStack[fragmentStack.size - 3]
                newFragment2 = rightScreen
                rightView = rightScreen.savedView
                if (rightView == null)
                    rightView = rightScreen.createView(context)
                else {
                    (rightView.parent as? ViewGroup)?.removeView(rightView)
                }
                rightActionBar = rightScreen.actionBar
                if (rightActionBar != null && rightActionBar.shouldAddToContainer) {
                    val parent = rightActionBar.parent as? ViewGroup
                    parent?.removeView(rightActionBar)
                }
            }

            var view = screen.savedView
            if (view == null)
                view = screen.createView(context)
            else {
                (view.parent as? ViewGroup)?.removeView(view)
            }
            var actionBar: ActionBar? = screen.actionBar
            if (actionBar != null && actionBar.shouldAddToContainer) {
                val parent = actionBar.parent as? ViewGroup
                parent?.removeView(actionBar)
            } else
                actionBar = null


            if (rightView == null)
                containerViewBack.addGroup(view, actionBar)
            else {
                containerViewBack.addGroup(rightView, rightActionBar)
                containerViewBack.nextScreen(view, actionBar, true)
            }

            val temp = containerViewBack
            containerViewBack = containerView
            containerView = temp
            containerView.visibility = VISIBLE

            bringChildToFront(containerView)
            presentScreenInternalRemoveOld(groupRemoved, true, true, true, currentScreen)
        } else
            presentScreenInternalRemoveOld(true, true, false, false, currentScreen)

    }

    fun closeAllUntil(screen: Fragment, containThis: Boolean = true) {
        var index = fragmentStack.indexOf(screen)
        if (index == -1)
            return
        if (!containThis)
            index++
        while (fragmentStack.size != index) {
            val currentScreen = fragmentStack[index]
            if (currentScreen.groupId == -2)
                (parentActivity.supportFragmentManager.findFragmentByTag("Sheet") as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
            else {
                currentScreen.pause()
                currentScreen.onFragmentDestroy()
                currentScreen.parentLayout = null
                fragmentStack.remove(currentScreen)
                if (fragmentStack.size != 0)
                    fragmentStack[fragmentStack.size - 1].onGetFirstInStack()
            }
        }

        val currentScreen = fragmentStack[fragmentStack.size - 1]

        var rightView: View? = null
        var rightActionBar: ActionBar? = null
        if (Utilities.isLandscapeTablet && fragmentStack.size > 1 && fragmentStack[fragmentStack.size - 2].groupId == currentScreen.groupId) {
            val rightScreen = fragmentStack[fragmentStack.size - 2]
            newFragment2 = rightScreen
            rightView = rightScreen.savedView
            if (rightView == null)
                rightView = rightScreen.createView(context)
            else {
                (rightView.parent as? ViewGroup)?.removeView(rightView)
            }
            rightActionBar = rightScreen.actionBar
            if (rightActionBar != null && rightActionBar.shouldAddToContainer) {
                val parent = rightActionBar.parent as? ViewGroup
                parent?.removeView(rightActionBar)
            }
        }

        var view = currentScreen.savedView
        if (view == null)
            view = currentScreen.createView(context)
        else {
            (view.parent as? ViewGroup)?.removeView(view)
        }
        var actionBar: ActionBar? = currentScreen.actionBar
        if (actionBar != null && actionBar.shouldAddToContainer) {
            val parent = actionBar.parent as? ViewGroup
            parent?.removeView(actionBar)
        } else
            actionBar = null

        if (rightView == null)
            containerViewBack.addGroup(view, actionBar)
        else {
            containerViewBack.addGroup(rightView, rightActionBar)
            containerViewBack.nextScreen(view, actionBar, true)
        }


        val temp = containerViewBack
        containerViewBack = containerView
        containerView = temp
        containerView.visibility = VISIBLE

        bringChildToFront(containerView)
    }

    private fun removeScreenFromStackInternal(fragment: Fragment, updateGroupId: Boolean) {
        if (fragment.groupId != -2)
            fragment.pause()
        fragment.onFragmentDestroy()
        fragment.parentLayout = null
        fragmentStack.remove(fragment)
        if (fragment.groupId != -2 && updateGroupId)
            currentGroupId = if (fragmentStack.size >= 1) {
                fragmentStack[fragmentStack.size - 1].onGetFirstInStack()
                fragmentStack[fragmentStack.size - 1].groupId
            } else
                0
    }

    fun popScreensFromStack(count: Int, removeLatest: Boolean) {
        var index = fragmentStack.size - 2
        val lastIndex = index - count
        while (index > lastIndex) {
            removeScreenFromStack(index, false)
            index--
        }
        if (removeLatest)
            fragmentStack[fragmentStack.size].finishFragment(true)
    }

    fun removeScreenFromStack(index: Int, updateGroupId: Boolean) {
        if (index >= fragmentStack.size) {
            return
        }
        removeScreenFromStackInternal(fragmentStack[index], updateGroupId)
    }

    fun removeScreenFromStack(fragment: Fragment) {
        removeScreenFromStackInternal(fragment, true)
    }

    fun removeAllFragments() {
        while (fragmentStack.size != 0) {
            removeScreenFromStack(fragmentStack.size - 1, true)
        }
    }

//    override fun onConfigurationChanged(newConfig: Configuration?) {
//        super.onConfigurationChanged(newConfig)
//        if (screensStack.isNotEmpty()) {
//            screensStack.forEach {
//                it.onConfigurationChanged(newConfig)
//            }
//        }
//    }

    fun send(toRight: Boolean, vararg data: Any?) {
        fragmentStack[fragmentStack.size - if (toRight) 1 else 2].onReceive(*data)
    }

    fun send(current: Fragment, step: Int, vararg data: Any?) {
        fragmentStack[fragmentStack.indexOf(current) + step].onReceive(*data)
    }


    fun onResume() {
        if (fragmentStack.isNotEmpty())
            fragmentStack[fragmentStack.size - 1].resume()
    }

    fun onPause() {
        if (fragmentStack.isNotEmpty())
            fragmentStack[fragmentStack.size - 1].pause()
    }

    fun getLastFragment(): Fragment? {
        return if (fragmentStack.isEmpty()) {
            null
        } else fragmentStack[fragmentStack.size - 1]
    }

    fun onOrientationChanged() {
        if (fragmentStack.isNotEmpty()) {
            fragmentStack.forEach {
                it.onOrientationChanged()
            }
            if (fragmentStack.size > 1) {
                val preScreen = fragmentStack[fragmentStack.size - 2]
                if (preScreen.groupId == fragmentStack[fragmentStack.size - 1].groupId) {
                    if (Utilities.isLandscapeTablet) {
                        newFragment = preScreen
                        var screenView = preScreen.savedView
                        if (screenView != null) {
                            val parent = screenView.parent as? ViewGroup
                            if (parent != null) {
                                preScreen.onRemoveFromParent()
                                parent.removeView(screenView)
                            }
                        } else
                            screenView = preScreen.createView(context)

                        if (preScreen.actionBar != null && preScreen.requiredActionBar.shouldAddToContainer) {
                            val parent = preScreen.requiredActionBar.parent as? ViewGroup
                            parent?.removeView(preScreen.actionBar)
                            containerView.openLeft(screenView, preScreen.actionBar)
                        } else
                            containerView.openLeft(screenView, null)
                    } else {
                        oldFragment = preScreen
                        containerView.closeLeft()
                    }
                }
            }
        }
    }

    fun startActivityForResult(
        intent: Intent?,
        requestCode: Int
    ) {
        parentActivity.startActivityForResult(intent, requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fragmentStack.forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun requestPermissions(
        permissions: Array<out String>, requestCode: Int
    ) {
        ActivityCompat.requestPermissions(parentActivity, permissions, requestCode)
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        fragmentStack.forEach {
            it.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun onBackPressed(): Boolean {
        if (fragmentStack.isEmpty())
            return false
        val lastFragment: Fragment = fragmentStack[fragmentStack.size - 1]
        if (!lastFragment.onBackPressed()) {
            if (fragmentStack.isNotEmpty()) {
                closeLastScreen(true)
                return fragmentStack.isNotEmpty()
            }
        } else
            return true
        return false
    }


}
