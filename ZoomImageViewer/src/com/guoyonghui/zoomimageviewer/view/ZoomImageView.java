package com.guoyonghui.zoomimageviewer.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener, OnScaleGestureListener {

	public static final String TAG = "ZoomImageView";

	/**
	 * 图片缩放值是否已经初始化
	 */
	private boolean mScaleInited;

	/**
	 * 图片最小缩放值
	 */
	private float mMinScale;

	/**
	 * 图片最大缩放值
	 */
	private float mMaxScale;

	/**
	 * 图片缩放矩阵
	 */
	private Matrix mScaleMatrix;

	/**
	 * 缩放手势检测器
	 */
	private ScaleGestureDetector mScaleGestureDetector;

	/**
	 * 触发图片拖动的最小距离
	 */
	private float mTouchSlop;

	/**
	 * 上次拖动图片时的焦点的X坐标
	 */
	private float mLastX;

	/**
	 * 上次拖动图片时的焦点的Y坐标
	 */
	private float mLastY;

	/**
	 * 上次MotionEvent发生时的手指数量
	 */
	private int mLastPointerCount;

	/**
	 * 拖动时是否要对水平方向进行调整
	 */
	private boolean mIsHorizontalAdjustNeeded;

	/**
	 * 拖动时是否要对竖直方向进行调整
	 */
	private boolean mIsVerticalAdjustNeeded;
	
	/**
	 * 是否满足拖动条件
	 */
	private boolean mCanDrag;

	public ZoomImageView(Context context) {
		super(context);

		initialize(null);
	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);

		initialize(attrs);
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		initialize(attrs);
	}

	/***
	 * 初始化
	 * @param attrs 属性集合
	 */
	private void initialize(AttributeSet attrs) {
		setScaleType(ScaleType.MATRIX);

		mScaleMatrix = new Matrix();

		mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);

		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	/***
	 * 获取图片当前所在的矩形区域
	 * @return 图片当前所在的矩形区域
	 */
	private RectF getImageRectf() {
		RectF rectF = new RectF();
		Matrix matrix = mScaleMatrix;

		Drawable d = getDrawable();
		if(d != null) {
			rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}
		return rectF;
	}

	/***
	 * 获取图片当前的缩放值
	 * @return 图片当前的缩放值
	 */
	private float getCurrentScale() {
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	/***
	 * 判断给定的距离是否构成拖动
	 * @param deltaX 水平位移
	 * @param deltaY 竖直位移
	 * @return 是否构成拖动
	 */
	private boolean isDragAction(float deltaX, float deltaY) {
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY) > mTouchSlop;
	}

	/***
	 * 当用户进行缩放时调整图片的边界和中心
	 */
	private void adjustBorderAndCenterWhenZooming() {
		RectF rectF = getImageRectf();

		//X轴Y轴增量
		float deltaX = 0f;
		float deltaY = 0f;

		//获取视图的宽高
		int width = getWidth();
		int height = getHeight();

		//根据图片当前所在的矩形区域与视图宽度的关系调整X轴增量
		if(rectF.width() > width) {
			//若矩形宽度大于视图宽度且矩形左边界的位置大于0，则显然在图片的左侧会有白边出现
			if(rectF.left > 0) {
				deltaX = -rectF.left;
			}
			//若矩形宽度大于视图宽度且矩形右边界的位置小于视图宽度值，则显然在图片的右侧会有白边出现
			if(rectF.right < width) {
				deltaX = width - rectF.right;
			}
		} else {
			//若矩形宽度小于等于视图宽度只需将图片调整至水平中心位置
			deltaX = width / 2 - rectF.right + rectF.width() / 2;
		}

		//根据图片当前所在的矩形区域与视图高度的关系调整Y轴增量
		if(rectF.height() > height) {
			//若矩形高度大于视图高度且矩形上边界的位置大于0，则显然在图片的上面会有白边出现
			if(rectF.top > 0) {
				deltaY = - rectF.top;
			}
			//若矩形高度大于视图高度且矩形下边界的位置小于视图高度值，则显然在图片的下面会有白边出现
			if(rectF.bottom < height) {
				deltaY = height - rectF.bottom;
			}
		} else {
			//若矩形高度小于等于视图高度只需将图片调整至竖直中心位置
			deltaY = height / 2 - rectF.bottom + rectF.height() / 2;
		}

		//设置位移
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}
	
	/***
	 * 当用户进行拖动时调整图片的边界
	 */
	private void adjustBorderWhenDragging() {
		RectF rectF = getImageRectf();
		
		float deltaX = 0f;
		float deltaY = 0f;
		
		int width = getWidth();
		int height = getHeight();
		
		if(mIsHorizontalAdjustNeeded) {
			//若矩形宽度大于视图宽度且矩形左边界的位置大于0，则显然在图片的左侧会有白边出现
			if(rectF.left > 0) {
				deltaX = -rectF.left;
			}
			//若矩形宽度大于视图宽度且矩形右边界的位置小于视图宽度值，则显然在图片的右侧会有白边出现
			if(rectF.right < width) {
				deltaX = width - rectF.right;
			}
		}
		
		if(mIsVerticalAdjustNeeded) {
			//若矩形高度大于视图高度且矩形上边界的位置大于0，则显然在图片的上面会有白边出现
			if(rectF.top > 0) {
				deltaY = - rectF.top;
			}
			//若矩形高度大于视图高度且矩形下边界的位置小于视图高度值，则显然在图片的下面会有白边出现
			if(rectF.bottom < height) {
				deltaY = height - rectF.bottom;
			}
		}
		
		//设置位移
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		//注册OnGlobalLayoutListener
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		//注销OnGlobalLayoutListener
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			getViewTreeObserver().removeOnGlobalLayoutListener(this);
		} else {
			getViewTreeObserver().removeGlobalOnLayoutListener(this);
		}
	}

	@Override
	public void onGlobalLayout() {
		if(!mScaleInited) {
			//获取ImageView的图片，如果图片为null直接返回
			Drawable d = getDrawable();
			if(d == null) {
				return;
			}

			float initScale = 1.0f;

			//获取视图即ImageView的宽高
			int width = getWidth();
			int height = getHeight();

			//获取图片的宽高
			int dWidth = d.getIntrinsicWidth();
			int dHeight = d.getIntrinsicHeight();

			//根据图片与视图的宽高的大小关系设置初始缩放值
			if(dWidth > width && dHeight <= height) {
				initScale = width * 1.0f / dWidth;
			} else if(dWidth <= width && dHeight > height) {
				initScale = height * 1.0f / dHeight;
			} else {
				initScale = Math.min(width * 1.0f / dWidth, height * 1.0f / dHeight);
			}

			//设置缩放值的最大最小值
			mMinScale = initScale;
			mMaxScale = initScale * 4;

			//计算将图片移至屏幕中央所需的X轴Y轴的增量
			float deltaX = (width - dWidth) / 2;
			float deltaY = (height - dHeight) / 2;

			//设置平移和以屏幕为中心缩放
			mScaleMatrix.postTranslate(deltaX, deltaY);
			mScaleMatrix.postScale(initScale, initScale, width / 2, height / 2);

			//设置图像变换矩阵
			setImageMatrix(mScaleMatrix);

			mScaleInited = true;
		}
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		//获取ImageView的图片，如果图片为null直接返回
		if(getDrawable() == null) {
			return true;
		}

		//获取图片当前的缩放值以及缩放因子即缩放因子
		float currentScale = getCurrentScale();
		float scaleFactor = detector.getScaleFactor();

		//根据图片当前的缩放值以及缩放因子进行计算
		//若当前缩放值介于最大缩放值和最小缩放值之间且缩放因子不为1则进行计算
		//若当前缩放值与缩放因子的乘积高于最大缩放值则将缩放因子置为最大缩放值除以当前缩放值
		//若当前缩放值与缩放因子的乘机低于最小缩放值则将缩放因子置为最小缩放值除以当前缩放值
		if((currentScale < mMaxScale && scaleFactor > 1.0f) || (currentScale > mMinScale && scaleFactor < 1.0f)) {
			if(currentScale * scaleFactor > mMaxScale) {
				scaleFactor = mMaxScale / currentScale;
			}
			if(currentScale * scaleFactor < mMinScale) {
				scaleFactor = mMinScale / currentScale;
			}

			//设置以手势焦点为中心缩放
			mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

			//动态调整缩放时的边界和中心
			adjustBorderAndCenterWhenZooming();

			//设置图像变换矩阵
			setImageMatrix(mScaleMatrix);
		}

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector detector) {

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//缩放手势检测器处理MotionEvent
		mScaleGestureDetector.onTouchEvent(event);

		//当前MotionEvent下的焦点坐标
		float x = 0f;
		float y = 0f;

		//获取当前MotionEvent下的手指数量并计算焦点坐标
		int pointerCount = event.getPointerCount();
		for(int i = 0; i < pointerCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}
		x /= pointerCount;
		y /= pointerCount;

		//若当前MotionEvent下的手指数量与上一次MotionEvent发生时的手指数量不符则将mLastX和mLastY置为当前焦点坐标
		//当触控点数增加或减少时会执行该操作，使得deltaX和deltaY在触控点数变更时被置为0即不发生拖动行为
		if(mLastPointerCount != pointerCount) {
			mLastX = x;
			mLastY = y;
			mLastPointerCount = pointerCount;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			//计算拖动的X轴Y轴增量
			float deltaX = x - mLastX;
			float deltaY = y - mLastY;

			//判断是否达到可以进行拖动的临界条件
			if(!mCanDrag) {
				mCanDrag = isDragAction(deltaX, deltaY);
			}
			
			//如果满足进行拖动的临界条件则进行拖动
			if(mCanDrag) {
				mIsHorizontalAdjustNeeded = true;
				mIsVerticalAdjustNeeded = true;

				RectF rectF = getImageRectf();
				if(getDrawable() != null) {
					//如果图片当前所在的矩形区域的宽度小于视图宽度则将拖动的X轴增量置为0
					//同时将mIsHorizontalAdjustNeeded置为false
					if(rectF.width() <= getWidth()) {
						deltaX = 0;
						mIsHorizontalAdjustNeeded = false;
					}
					//如果图片当前所在的矩形区域的高度小于视图高度则将拖动的Y轴增量置为0
					//同时将mIsVerticalAdjustNeeded置为false
					if(rectF.height() <= getHeight()) {
						deltaY = 0;
						mIsVerticalAdjustNeeded = false;
					}
				}

				//设置位移
				mScaleMatrix.postTranslate(deltaX, deltaY);

				//动态调整拖动时的边界
				adjustBorderWhenDragging();

				//设置图像变换矩阵
				setImageMatrix(mScaleMatrix);
			}

			//将上一次MotionEvent事件发生时的焦点坐标置为当前焦点坐标
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_UP:
			//发生MotionEvent.ACTION_UP时将mLastPointerCount置为0并将mCanDrag置为false
			mLastPointerCount = 0;
			mCanDrag = false;
			break;
		case MotionEvent.ACTION_CANCEL:
			//发生MotionEvent.ACTION_CANCEL时将mLastPointerCount置为0并将mCanDrag置为false
			mLastPointerCount = 0;
			mCanDrag = false;
			break;
		default:
			break;
		}

		return true;
	}

}
