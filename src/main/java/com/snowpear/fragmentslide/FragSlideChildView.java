package com.snowpear.fragmentslide;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;
import android.widget.Scroller;

/**
 * Created by wangjinpeng on 15/11/3.
 */
public class FragSlideChildView extends RelativeLayout {
    public FragSlideChildView(Context context) {
        super(context);
        init();
    }

    public FragSlideChildView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FragSlideChildView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private static final Interpolator sInterpolator = new Interpolator() {
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    /** 最小滑动速率，超过这个速率就可以翻页 **/
    private static final int mSlideVelocity = 2500;
    private static final String TAG = "FragSlideChildView";
    private static boolean DEBUG = true;
    /** 能否拖拽 **/
    private boolean mIsUnableToDrag;
    /** 是否已经在拖拽中 **/
    private boolean mIsBeingDragged;
    protected int mMaximumVelocity;
    protected VelocityTracker mVelocityTracker;

    private FragSlideChildView mViewBehind;
    private static final boolean USE_CACHE = true;
    /** 滑动动画的最大时间 **/
    private static final int MAX_SETTLE_DURATION = 400; // ms
    private float mScrollX = 0.0f;

    /** 目前指边缘触摸范围，在此范围必须滑动 **/
    private int MUST_SCROLL_MARGIN = 20;

    /** 自动滑动时  动画执行前的延时 **/
    private static final int AUTO_SCROLL_DELAY = 300;


    /** 上次触摸时间的坐标 **/
    private float mLastMotionX,mLastMotionY;

    private float mInitialMotionX;
    private int mActivePointerId = 0;
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    private int mTouchSlop = 0;

    /**
     * 设置背后的View
     * @param view
     */
    public void setBehindView(FragSlideChildView view){
        mViewBehind = view;
    }

    public void init(){
        mScroller = new Scroller(getContext(), sInterpolator);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mTouchSlop = configuration.getScaledTouchSlop();
    }

    public void recordStartPos(MotionEvent ev){
        // 记录开始坐标的index 和  ID
        int index = MotionEventCompat.getActionIndex(ev);
        mActivePointerId = MotionEventCompat.getPointerId(ev, index);
        mLastMotionX = mInitialMotionX = ev.getX();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        debug(TAG, "---onInterceptTouchEvent---A");
        if(mViewBehind==null){//说明只有自己，不需要滑动
            debug(TAG, "---onInterceptTouchEvent---view null");
            return false;
        }
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                debug(TAG, "---onInterceptTouchEvent---down");
                int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                if (mActivePointerId == INVALID_POINTER) {
                    debug(TAG, "---onInterceptTouchEvent---down--invalid-pointer");
                    break;
                }
                //按下事件记录一下初始坐标位置和上次触摸的位置
                mLastMotionX = mInitialMotionX = MotionEventCompat.getX(ev, index);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                //暂时写死左侧xx像素内触摸 拦截事件
                if(mLastMotionX >= 0 && mLastMotionX < MUST_SCROLL_MARGIN){
                    startDrag();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                debug(TAG, "---onInterceptTouchEvent---move");
                if(mActivePointerId == INVALID_POINTER){
                    recordStartPos(ev);
                }
                determineDrag(ev);
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                endDrag();
                break;
        }
        if (!mIsBeingDragged) {
            if (mVelocityTracker == null) {
                mVelocityTracker = VelocityTracker.obtain();
            }
            mVelocityTracker.addMovement(ev);
        }
        return mIsBeingDragged;
    }


    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        debug(TAG, "---onTouchEvent---A");
        if(mViewBehind==null){
            debug(TAG, "---onTouchEvent---view--behind");
            return false;
        }else{
            debug(TAG, "---onTouchEvent---view--top");
        }

        final int action = ev.getAction();

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action & MotionEventCompat.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                debug(TAG, "---onTouchEvent---Down");
                completeScroll();//结束上一次触摸
                recordStartPos(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                debug(TAG, "---onTouchEvent---move");
                //如果不是拖拽状态再计算一次
                if (!mIsBeingDragged) {
                    determineDrag(ev);
                }
                if (mIsBeingDragged) {
                    debug(TAG, "---onTouchEvent---move--unable--mIsBeingDragged");
                    final int activePointerIndex = getPointerIndex(ev, mActivePointerId);
                    if (mActivePointerId == INVALID_POINTER) {
                        break;
                    }
                    final float x = MotionEventCompat.getX(ev, activePointerIndex);
                    final float deltaX = mLastMotionX - x;//当前触摸位置和初始位置的偏差

                    //更新上一次的位置
                    mLastMotionX = x;
                    float oldScrollX = getScrollX();//取到当前滚动的值
                    float scrollX = oldScrollX + deltaX;//最终要滚动到的值
                    int measuredWidth = getMeasuredWidth();
                    if(scrollX>=0){
                        debug(TAG, "---onTouchEvent--ACTION_MOVE--A");
                        scrollX = 0;
                    }else if(scrollX<=(-measuredWidth)){
                        debug(TAG, "---onTouchEvent--ACTION_MOVE--B");
                        scrollX = -measuredWidth;
                    }else{
                        debug(TAG, "---onTouchEvent--ACTION_MOVE--C");
                    }
                    scrollTo((int)scrollX, getScrollY());
                    pageScrolled((int) scrollX);
                }
                break;
            case MotionEvent.ACTION_UP:
                debug(TAG, "---onTouchEvent--ACTION_UP");
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(velocityTracker, mActivePointerId);
                    final int activePointerIndex = getPointerIndex(ev, mActivePointerId);
                    if (mActivePointerId != INVALID_POINTER) {
                        final float x = MotionEventCompat.getX(ev, activePointerIndex);
                        final int totalDelta = (int) (x - mInitialMotionX);
                        //根据滑动的距离判断是否进入上一页或者下一页
                        if(initialVelocity>mSlideVelocity){
                            //返回上一页
                            debug(TAG, "---onTouchEvent--ACTION_UP--A");
                            smoothExit(initialVelocity);
                        }else if(initialVelocity<-mSlideVelocity){
                            //回到当前状态
                            debug(TAG, "---onTouchEvent--ACTION_UP--B");
                            smoothEnter(initialVelocity);
                        }else{
                            if(totalDelta>getMeasuredWidth() / 3){
                                debug(TAG, "---onTouchEvent--ACTION_UP--C");
                                smoothExit(initialVelocity);
                            }else{
                                debug(TAG, "---onTouchEvent--ACTION_UP--D");
                                smoothEnter(initialVelocity);
                            }
                        }
                    } else {
                        //回到当前界面
                        //回到当前状态
                        debug(TAG, "---onTouchEvent--ACTION_UP--E");
                        smoothEnter(initialVelocity);
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    smoothEnter(0);
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int indexx = MotionEventCompat.getActionIndex(ev);
                mLastMotionX = MotionEventCompat.getX(ev, indexx);
                mActivePointerId = MotionEventCompat.getPointerId(ev, indexx);
                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                int pointerIndex = getPointerIndex(ev, mActivePointerId);
                if (mActivePointerId == INVALID_POINTER)
                    break;
                mLastMotionX = MotionEventCompat.getX(ev, pointerIndex);
                break;
        }
        return true;
    }

    /**
     * 判断是否拖拽，给是mIsUnableToDrag（是否能拖拽）置状态
     * @param ev
     */
    private void determineDrag(MotionEvent ev) {
        final int activePointerId = mActivePointerId;
        final int pointerIndex = getPointerIndex(ev, activePointerId);
        if (activePointerId == INVALID_POINTER)
            return;
        final float x = MotionEventCompat.getX(ev, pointerIndex);
        final float dx = x - mLastMotionX;
        final float xDiff = Math.abs(dx);
        final float y = MotionEventCompat.getY(ev, pointerIndex);
        final float dy = y - mLastMotionY;
        final float yDiff = Math.abs(dy);
        if (xDiff > mTouchSlop && xDiff > yDiff) {//此处只用判断是否能左右滑动即可  否则时间不走这里  会错失touch事件的拦截
            startDrag();
            mLastMotionX = x;
            mLastMotionY = y;
            setScrollingCacheEnabled(true);
            // TODO add back in touch slop check
        } else {
            mIsUnableToDrag = true;
        }
    }

    private int getPointerIndex(MotionEvent ev, int id) {
        int activePointerIndex = MotionEventCompat.findPointerIndex(ev, id);
        if (activePointerIndex == -1)
            mActivePointerId = INVALID_POINTER;
        return activePointerIndex;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        debug(TAG, "onSecondaryPointerUp called");
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    /**
     * 结束拖拽
     */
    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;
        mActivePointerId = INVALID_POINTER;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        //此处调用监听器，通知调用者已经结束拖拽
        if (mToggleChangedListener != null)
            mToggleChangedListener.onEndDrag();
    }

    /**
     * 开始拖拽
     */
    private void startDrag() {
        mIsBeingDragged = true;
        mScrolling = false;
        isEnter = false;
        isExit = true;
        //此处调用监视器，通知调用者现在开始拖拽
        if (mToggleChangedListener != null) {
            mToggleChangedListener.onStartDrag();
            mToggleChangedListener.onExitStart();
        }
    }

    private void setScrollingCacheEnabled(boolean enabled) {
        if (USE_CACHE) {
            final int size = getChildCount();
            for (int i = 0; i < size; ++i) {
                final View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    child.setDrawingCacheEnabled(!enabled);
                }
            }
        }
    }

    public boolean isScrolling(){
        return mScrolling;
    }

    public void clearScrolling(){
        completeScroll();
    }

    private boolean mScrolling;
    private Scroller mScroller;
    private void completeScroll() {
        boolean needPopulate = mScrolling;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            mScroller.abortAnimation();
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();
            if (oldX != x || oldY != y) {
                scrollTo(x, y);
            }
            if (isSmoothEnter) {
                if (mToggleChangedListener != null)
                    mToggleChangedListener.onEnter(isFirstOpen);
            } else {
                if (mToggleChangedListener != null)
                    mToggleChangedListener.onExit();
            }

            isFirstOpen = false;
        }
        mScrolling = false;
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished()) {
            if (mScroller.computeScrollOffset()) {
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();

                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                    pageScrolled(x);
                }

                // Keep on drawing until the animation has finished.
                invalidateView();
                return;
            }
        }

        // Done with scroll, clean up state.
        completeScroll();
    }

    /**
     * 预留方法，为以后回调控制着滚动距离做预留
     * @param xpos
     */
    private void pageScrolled(int xpos) {
        final int widthWithMargin = getWidth();
        final float offset = (float) xpos / widthWithMargin;
        final int percent = (int)Math.abs(offset * 100);
        debug(TAG, "pageScrolled--percent:" + percent);
        if(mToggleChangedListener!=null){
            if(isEnter) {
                mToggleChangedListener.onScrollProgress(this, mViewBehind, 100 - percent);
            }else{
                mToggleChangedListener.onScrollProgress(mViewBehind, this, percent);
            }
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        mScrollX = x;
        if(mViewBehind!=null) {
            mViewBehind.scrollByTop(this, x, y);
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
    void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    void smoothScrollTo(int x, int y, int velocity) {
//        if (getChildCount() == 0) {
//            // Nothing to do.
//            setScrollingCacheEnabled(false);
//            return;
//        }
        int sx = getScrollX();

        final int width = getMeasuredWidth();
        int sy = getScrollY();
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {

            debug(TAG, "---onTouchEvent--smoothScrollTo--over scroll");
            completeScroll();
            if (isSmoothEnter) {
                if (mToggleChangedListener != null)
                    mToggleChangedListener.onEnter(isFirstOpen);
            } else {
                if (mToggleChangedListener != null)
                    mToggleChangedListener.onExit();
            }
            isFirstOpen = false;
            return;
        }

        debug(TAG, "---onTouchEvent--smoothScrollTo--scrolling");
        setScrollingCacheEnabled(true);
        mScrolling = true;

        final int halfWidth = width / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
        final float distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio);

        int duration = 0;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(500 * Math.abs(distance / velocity));
        } else {
            duration = MAX_SETTLE_DURATION;
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION);

        debug(TAG, "---onTouchEvent--smoothScrollTo--sx:" + sx);
        debug(TAG, "---onTouchEvent--smoothScrollTo--dx:" + dx);
        debug(TAG, "---onTouchEvent--smoothScrollTo--duration:" + duration);
        mScroller.startScroll(sx, sy, dx, dy, duration);
//        setLayerType(View.LAYER_TYPE_HARDWARE, null);
        invalidateView();
    }

    float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    public void exit() {
        if (mToggleChangedListener != null)
            mToggleChangedListener.onExitStart();
        isEnter = false;
        isExit = true;
        smoothExit(0);
    }

    public void enter() {
        if (mToggleChangedListener != null)
            mToggleChangedListener.onEnterStart();
        isEnter = true;
        isExit = false;
        smoothEnter(0);
    }

    boolean isFirstOpen = true;
    /** 退出的标志 */
    boolean isExit = false;
    /** 进入的标志 */
    boolean isEnter = false;

    public void enterFrom(int x, int y) {
        enterFrom(x, y, true);
    }

    public void enterFrom(int x, int y, boolean isNeedDelay) {
        scrollTo(x, y);
        isFirstOpen = true;
        if (AUTO_SCROLL_DELAY > 0 && isNeedDelay) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    enter();
                }
            }, AUTO_SCROLL_DELAY);
        } else {
            enter();
        }
    }

    /** 滑动方向的标志**/
    boolean isSmoothEnter = false;
    public void smoothExit(int velocity) {
        isSmoothEnter = false;
        smoothScrollTo(-getMeasuredWidth(), 0, velocity);
    }

    public void smoothEnter(int velocity) {
        isSmoothEnter = true;
        smoothScrollTo(0, 0, velocity);
    }

    private OnToggleChangedListener mToggleChangedListener;
    public void setOnToggleChangedListener(OnToggleChangedListener listener){
        mToggleChangedListener = listener;
    }

    public interface OnToggleChangedListener{

        void onStartDrag();
        void onEndDrag();
        void onExitStart();
        void onEnterStart();
        void onExit();
        void onEnter(boolean isFirstOpen);
        void onScrollProgress(View viewEnter, View viewExit, int progress);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        //此处绘制下一层的遮罩和阴影
        if(mViewBehind!=null){
            mViewBehind.drawShadow(this, canvas);
            mViewBehind.drawFade(this, canvas, getOpenPercent());
        }
    }

    private Drawable mShadowDrawable;//滑动两个边缘的shadow的drawable
    private int shadowWidth = 30;//shadow的宽度，像素值
    private final Paint mFadePaint = new Paint();//遮罩画笔
    private float fadePercent = 1f;//背景界面的加黑百分比  1为全黑
    private float scrollBehindScale = 0.33f;//背部界面的结束位置占整个界面的百分比

    /**
     *
     * @param percent
     */
    public void setFadePercent(float percent){
        this.fadePercent = percent;
    }


    public void setShadowDrawable(Drawable shadow, int shadowWidth) {
        mShadowDrawable = shadow;
        this.shadowWidth = shadowWidth;
        if(mShadowDrawable == null || shadowWidth <= 0){
            return;
        } else{
            invalidateView();
        }
    }

    private void invalidateView(){
        postInvalidate();
    }

    /**
     * 绘制阴影，view在第二层的时候会被调用
     * @param content
     * @param canvas
     */
    public void drawShadow(View content, Canvas canvas) {
        if (mShadowDrawable == null || shadowWidth <= 0) return;

        int left = content.getLeft() - shadowWidth;
        mShadowDrawable.setBounds(left, 0, left + shadowWidth, getHeight());
        mShadowDrawable.draw(canvas);
    }

    /**
     * 此方法为绘制遮罩的方法，只有view在第二层的时候会被调用
     * @param content
     * @param canvas
     * @param openPercent
     */
    public void drawFade(View content, Canvas canvas, float openPercent) {
        if(fadePercent<=0){
            return ;
        }
        final int alpha = (int) (fadePercent * 255 * Math.abs(1-openPercent));
        mFadePaint.setColor(Color.argb(alpha, 0, 0, 0));
        int left = content.getLeft() - getMeasuredWidth();
        int right = 0;
        canvas.drawRect(left, 0, right, getHeight(), mFadePaint);
    }

    public void scrollByTop(View content, int x, int y){
        scrollTo((int) ((x + getMeasuredWidth()) * scrollBehindScale), y);
    }

    public void setScrollBehindScale(float scrollBehindScale) {
        this.scrollBehindScale = scrollBehindScale;
    }

    /**
     * 计算view打开的百分比
     * @return
     */
    private float getOpenPercent(){
        if(mViewBehind==null){
            return 1;
        }
        return Math.abs(mScrollX-getLeft()) / mViewBehind.getMeasuredWidth();
    }

    private void debug(String tag, String info){
        if(DEBUG) {
            Log.d(tag, info);
        }
    }
}
