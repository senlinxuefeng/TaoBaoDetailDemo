package com.hankkin.library;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 包含两个ScrollView的容器
 */
public class ScrollViewContainer extends RelativeLayout {

    private OnUpOrDownListener onUpOrDownListener;
    private GestureDetector gestureDetector;

    public interface OnUpOrDownListener {
        public void onUpOrDown(boolean isUp);
    }

    public void setOnUpOrDownListener(OnUpOrDownListener onUpOrDownListener) {
        this.onUpOrDownListener = onUpOrDownListener;
    }

    /**
     * 自动上滑
     */
    public static final int AUTO_UP = 0;
    /**
     * 自动下滑
     */
    public static final int AUTO_DOWN = 1;
    /**
     * 动画完成
     */
    public static final int DONE = 2;
    /**
     * 动画速度
     */
    public static final float SPEED = 8.5f;

    private boolean isMeasured = false;

    /**
     * 用于计算手滑动的速度
     */
    private VelocityTracker vt;

    private int mViewHeight;
    private int mViewWidth;

    private View topView;
    private View bottomView;

    private boolean canPullDown;
    private boolean canPullUp;
    private int state = DONE;

    /**
     * 记录当前展示的是哪个view，0是topView，1是bottomView
     */
    private int mCurrentViewIndex = 0;
    /**
     * 手滑动距离，这个是控制布局的主要变量
     */
    private float mMoveLen;
    private MyTimer mTimer;
    private float mLastY;
    /**
     * 用于控制是否变动布局的另一个条件，mEvents==0时布局可以拖拽了，mEvents==-1时可以舍弃将要到来的第一个move事件，
     * 这点是去除多点拖动剧变的关键
     */
    private int mEvents;
    boolean isTuninginterface = true;
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (mMoveLen != 0) {
                if (state == AUTO_UP) {
                    mMoveLen -= SPEED;
                    if (mMoveLen <= -mViewHeight) {
                        mMoveLen = -mViewHeight;
                        state = DONE;
                        mCurrentViewIndex = 1;
                        if (isTuninginterface) {
                            isTuninginterface = false;
                        }
                    }
                } else if (state == AUTO_DOWN) {
                    mMoveLen += SPEED;
                    if (mMoveLen >= 0) {
                        mMoveLen = 0;
                        state = DONE;
                        mCurrentViewIndex = 0;
                    }
                } else {
                    mTimer.cancel();
                }
            }
            requestLayout();
        }

    };
    private int tempHeight;
    private View centerView;

    public ScrollViewContainer(Context context) {
        super(context);
        init();
    }

    public ScrollViewContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ScrollViewContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), onGestureListener);
        mTimer = new MyTimer(handler);
    }


    GestureDetector.OnGestureListener onGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            Log.i("onGestureListener","onDown");
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            Log.i("onGestureListener","onShowPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Log.i("onGestureListener","onSingleTapUp");
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.i("onGestureListener","onScroll");
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.i("onGestureListener","onLongPress");
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            Log.i("onGestureListener","onFling");

            return false;
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (vt == null)
                    vt = VelocityTracker.obtain();//获得VelocityTracker类实例
                else
                    vt.clear();
                mLastY = ev.getY();
                System.out.println("---ACTION_DOWN-mLastY------" + ev.getY());
                vt.addMovement(ev);
                mEvents = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_POINTER_UP:
                // 多一只手指按下或抬起时舍弃将要到来的第一个事件move，防止多点拖拽的bug
                mEvents = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                vt.addMovement(ev);//将事件加入到VelocityTracker类实例中
                if (canPullUp && mCurrentViewIndex == 0 && mEvents == 0) {
                    mMoveLen += (ev.getY() - mLastY);
                    // 防止上下越界
                    if (mMoveLen > 0) {
                        mMoveLen = 0;
                        mCurrentViewIndex = 0;
                    } else if (mMoveLen < -mViewHeight) {
//                        mMoveLen = -mViewHeight;
                        mMoveLen = -topView.getMeasuredHeight();
                        mCurrentViewIndex = 1;
                        if (isTuninginterface) {
                            isTuninginterface = false;
                        }
                    }
                    if (mMoveLen < -8) {
                        // 防止事件冲突
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                    }
                } else if (canPullDown && mCurrentViewIndex == 1 && mEvents == 0) {
                    mMoveLen += (ev.getY() - mLastY);
                    // 防止上下越界
                    if (mMoveLen < -mViewHeight) {
//                        mMoveLen = -mViewHeight;
                        mMoveLen = -topView.getMeasuredHeight();
                        mCurrentViewIndex = 1;
                    } else if (mMoveLen > 0) {
                        mMoveLen = 0;
                        mCurrentViewIndex = 0;
                    }
                    if (mMoveLen > 8 - mViewHeight) {
                        // 防止事件冲突
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                    }
                } else
                    mEvents++;
                mLastY = ev.getY();

                System.out.println("======onMeasure====mMoveLen======" + mMoveLen + "     " + tempHeight);

                if (mCurrentViewIndex == 0 && Math.abs(mMoveLen) < tempHeight) {
                    requestLayout();
                } else if (mCurrentViewIndex == 1 && Math.abs(mViewHeight + mMoveLen) < tempHeight) {
                    requestLayout();
                } else {

                }

                //	requestLayout：当view确定自身已经不再适合现有的区域时，该view本身调用这个方法要求parent view重新调用他的onMeasure onLayout来对重新设置自己位置。

                break;
            case MotionEvent.ACTION_UP:
                mLastY = ev.getY();
                vt.addMovement(ev);
                //参数：units  你想要指定的得到的速度单位，如果值为1，代表1毫秒运动了多少像素。如果值为1000，代表 1秒内运动了多少像素
                vt.computeCurrentVelocity(700);
                // 获取Y方向的速度 可以通过getXVelocity()和getYVelocity()获得横向和竖向的速率
                float mYV = vt.getYVelocity();
                if (mMoveLen == 0 || mMoveLen == -mViewHeight)
                    break;

                System.out.println("======ACTION_UP====mMoveLen======" + mMoveLen + "     " + tempHeight);

                if (Math.abs(mYV) < 500) {
                    // 速度小于一定值的时候当作静止释放，这时候两个View往哪移动取决于滑动的距离
//                    if (mMoveLen <= -mViewHeight / 2) {
//                        state = AUTO_UP;
//                    } else if (mMoveLen > -mViewHeight / 2) {
//                        state = AUTO_DOWN;
//                    }

                    int temp = Math.abs(-tempHeight / 2);

                    if (mCurrentViewIndex == 1) {
                        if (Math.abs(mViewHeight + mMoveLen) > temp) {
                            state = AUTO_DOWN;
                        } else {
                            state = AUTO_UP;
                        }
                    } else if (mCurrentViewIndex == 0) {
                        if (Math.abs(mMoveLen) <= temp) {
                            state = AUTO_DOWN;
                        } else {
                            state = AUTO_UP;
                        }
                    }
                } else {
                    // 抬起手指时速度方向决定两个View往哪移动
                    if (mYV < 0)
                        state = AUTO_UP;
                    else
                        state = AUTO_DOWN;
                }

                if (onUpOrDownListener != null) {
                    onUpOrDownListener.onUpOrDown(state == 1);
                }

                mTimer.schedule(2);
                try {
                    vt.recycle();
                    vt = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;

            default:

                break;

        }
        super.dispatchTouchEvent(ev);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        System.out.println("======onLayout====onLayout======");

        topView.layout(0, (int) mMoveLen, mViewWidth,
                topView.getMeasuredHeight() + (int) mMoveLen);
        bottomView.layout(0, topView.getMeasuredHeight() + (int) mMoveLen,
                mViewWidth, topView.getMeasuredHeight() + (int) mMoveLen
                        + bottomView.getMeasuredHeight());

        if (mCurrentViewIndex == 0) {
            centerView.layout(0, topView.getMeasuredHeight() + (int) mMoveLen,
                    mViewWidth, topView.getMeasuredHeight() + (int) mMoveLen
                            + centerView.getMeasuredHeight());
        } else {
            centerView.layout(0, topView.getMeasuredHeight() + (int) mMoveLen - tempHeight,
                    mViewWidth, topView.getMeasuredHeight() + (int) mMoveLen + centerView.getMeasuredHeight() - tempHeight);

//            centerView.layout(0, topView.getMeasuredHeight() + (int) mMoveLen - 0,
//                    mViewWidth, topView.getMeasuredHeight() + (int) mMoveLen + centerView.getMeasuredHeight() - 0);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!isMeasured) {
            isMeasured = true;
            mViewHeight = getMeasuredHeight();
            mViewWidth = getMeasuredWidth();
            System.out.println("======onMeasure====mViewHeight======" + mViewHeight);
            System.out.println("======onMeasure====mViewWidth======" + mViewWidth);


            topView = getChildAt(0);
            bottomView = getChildAt(1);

            centerView = getChildAt(2);
            tempHeight = CommonUtils.dip2px(getContext(), 80);

            bottomView.setOnTouchListener(bottomViewTouchListener);
            topView.setOnTouchListener(topViewTouchListener);
        } else {
            //修复手机小米兼容问题
            if (topView.getMeasuredHeight() > mViewHeight) {
                mViewHeight = topView.getMeasuredHeight();
            }
        }

//        System.out.println("======onMeasure====mViewHeight======" + mViewHeight + "      topView   " + topView.getMeasuredHeight());

    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (topView != null) {
            System.out.println("======onMeasure====mViewHeight======" + mViewHeight + "      topView   " + topView.getMeasuredHeight());
        }
    }

    private OnTouchListener topViewTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ScrollView sv = (ScrollView) v;
            if (sv.getScrollY() == (sv.getChildAt(0).getMeasuredHeight() - sv
                    .getMeasuredHeight()) && mCurrentViewIndex == 0)
                canPullUp = true;
            else
                canPullUp = false;
            return false;
        }
    };
    private OnTouchListener bottomViewTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
//            ScrollView sv = (ScrollView) v;
            RecyclerView sv = (RecyclerView) v;
            if (sv.getScrollY() == 0 && mCurrentViewIndex == 1)
                canPullDown = true;
            else
                canPullDown = false;
            return false;
        }
    };

    class MyTimer {
        private Handler handler;
        private Timer timer;
        private MyTask mTask;

        public MyTimer(Handler handler) {
            this.handler = handler;
            timer = new Timer();
        }

        public void schedule(long period) {
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
            mTask = new MyTask(handler);
            timer.schedule(mTask, 0, period);
        }

        public void cancel() {
            if (mTask != null) {
                mTask.cancel();
                mTask = null;
            }
        }

        class MyTask extends TimerTask {
            private Handler handler;

            public MyTask(Handler handler) {
                this.handler = handler;
            }

            @Override
            public void run() {
                handler.obtainMessage().sendToTarget();
            }

        }
    }
}
