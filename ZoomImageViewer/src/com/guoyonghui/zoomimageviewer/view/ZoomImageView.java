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
	 * ͼƬ����ֵ�Ƿ��Ѿ���ʼ��
	 */
	private boolean mScaleInited;

	/**
	 * ͼƬ��С����ֵ
	 */
	private float mMinScale;

	/**
	 * ͼƬ�������ֵ
	 */
	private float mMaxScale;

	/**
	 * ͼƬ���ž���
	 */
	private Matrix mScaleMatrix;

	/**
	 * �������Ƽ����
	 */
	private ScaleGestureDetector mScaleGestureDetector;

	/**
	 * ����ͼƬ�϶�����С����
	 */
	private float mTouchSlop;

	/**
	 * �ϴ��϶�ͼƬʱ�Ľ����X����
	 */
	private float mLastX;

	/**
	 * �ϴ��϶�ͼƬʱ�Ľ����Y����
	 */
	private float mLastY;

	/**
	 * �ϴ�MotionEvent����ʱ����ָ����
	 */
	private int mLastPointerCount;

	/**
	 * �϶�ʱ�Ƿ�Ҫ��ˮƽ������е���
	 */
	private boolean mIsHorizontalAdjustNeeded;

	/**
	 * �϶�ʱ�Ƿ�Ҫ����ֱ������е���
	 */
	private boolean mIsVerticalAdjustNeeded;
	
	/**
	 * �Ƿ������϶�����
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
	 * ��ʼ��
	 * @param attrs ���Լ���
	 */
	private void initialize(AttributeSet attrs) {
		setScaleType(ScaleType.MATRIX);

		mScaleMatrix = new Matrix();

		mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);

		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
	}

	/***
	 * ��ȡͼƬ��ǰ���ڵľ�������
	 * @return ͼƬ��ǰ���ڵľ�������
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
	 * ��ȡͼƬ��ǰ������ֵ
	 * @return ͼƬ��ǰ������ֵ
	 */
	private float getCurrentScale() {
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	/***
	 * �жϸ����ľ����Ƿ񹹳��϶�
	 * @param deltaX ˮƽλ��
	 * @param deltaY ��ֱλ��
	 * @return �Ƿ񹹳��϶�
	 */
	private boolean isDragAction(float deltaX, float deltaY) {
		return Math.sqrt(deltaX * deltaX + deltaY * deltaY) > mTouchSlop;
	}

	/***
	 * ���û���������ʱ����ͼƬ�ı߽������
	 */
	private void adjustBorderAndCenterWhenZooming() {
		RectF rectF = getImageRectf();

		//X��Y������
		float deltaX = 0f;
		float deltaY = 0f;

		//��ȡ��ͼ�Ŀ��
		int width = getWidth();
		int height = getHeight();

		//����ͼƬ��ǰ���ڵľ�����������ͼ��ȵĹ�ϵ����X������
		if(rectF.width() > width) {
			//�����ο�ȴ�����ͼ����Ҿ�����߽��λ�ô���0������Ȼ��ͼƬ�������аױ߳���
			if(rectF.left > 0) {
				deltaX = -rectF.left;
			}
			//�����ο�ȴ�����ͼ����Ҿ����ұ߽��λ��С����ͼ���ֵ������Ȼ��ͼƬ���Ҳ���аױ߳���
			if(rectF.right < width) {
				deltaX = width - rectF.right;
			}
		} else {
			//�����ο��С�ڵ�����ͼ���ֻ�轫ͼƬ������ˮƽ����λ��
			deltaX = width / 2 - rectF.right + rectF.width() / 2;
		}

		//����ͼƬ��ǰ���ڵľ�����������ͼ�߶ȵĹ�ϵ����Y������
		if(rectF.height() > height) {
			//�����θ߶ȴ�����ͼ�߶��Ҿ����ϱ߽��λ�ô���0������Ȼ��ͼƬ��������аױ߳���
			if(rectF.top > 0) {
				deltaY = - rectF.top;
			}
			//�����θ߶ȴ�����ͼ�߶��Ҿ����±߽��λ��С����ͼ�߶�ֵ������Ȼ��ͼƬ��������аױ߳���
			if(rectF.bottom < height) {
				deltaY = height - rectF.bottom;
			}
		} else {
			//�����θ߶�С�ڵ�����ͼ�߶�ֻ�轫ͼƬ��������ֱ����λ��
			deltaY = height / 2 - rectF.bottom + rectF.height() / 2;
		}

		//����λ��
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}
	
	/***
	 * ���û������϶�ʱ����ͼƬ�ı߽�
	 */
	private void adjustBorderWhenDragging() {
		RectF rectF = getImageRectf();
		
		float deltaX = 0f;
		float deltaY = 0f;
		
		int width = getWidth();
		int height = getHeight();
		
		if(mIsHorizontalAdjustNeeded) {
			//�����ο�ȴ�����ͼ����Ҿ�����߽��λ�ô���0������Ȼ��ͼƬ�������аױ߳���
			if(rectF.left > 0) {
				deltaX = -rectF.left;
			}
			//�����ο�ȴ�����ͼ����Ҿ����ұ߽��λ��С����ͼ���ֵ������Ȼ��ͼƬ���Ҳ���аױ߳���
			if(rectF.right < width) {
				deltaX = width - rectF.right;
			}
		}
		
		if(mIsVerticalAdjustNeeded) {
			//�����θ߶ȴ�����ͼ�߶��Ҿ����ϱ߽��λ�ô���0������Ȼ��ͼƬ��������аױ߳���
			if(rectF.top > 0) {
				deltaY = - rectF.top;
			}
			//�����θ߶ȴ�����ͼ�߶��Ҿ����±߽��λ��С����ͼ�߶�ֵ������Ȼ��ͼƬ��������аױ߳���
			if(rectF.bottom < height) {
				deltaY = height - rectF.bottom;
			}
		}
		
		//����λ��
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		//ע��OnGlobalLayoutListener
		getViewTreeObserver().addOnGlobalLayoutListener(this);
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		//ע��OnGlobalLayoutListener
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			getViewTreeObserver().removeOnGlobalLayoutListener(this);
		} else {
			getViewTreeObserver().removeGlobalOnLayoutListener(this);
		}
	}

	@Override
	public void onGlobalLayout() {
		if(!mScaleInited) {
			//��ȡImageView��ͼƬ�����ͼƬΪnullֱ�ӷ���
			Drawable d = getDrawable();
			if(d == null) {
				return;
			}

			float initScale = 1.0f;

			//��ȡ��ͼ��ImageView�Ŀ��
			int width = getWidth();
			int height = getHeight();

			//��ȡͼƬ�Ŀ��
			int dWidth = d.getIntrinsicWidth();
			int dHeight = d.getIntrinsicHeight();

			//����ͼƬ����ͼ�Ŀ�ߵĴ�С��ϵ���ó�ʼ����ֵ
			if(dWidth > width && dHeight <= height) {
				initScale = width * 1.0f / dWidth;
			} else if(dWidth <= width && dHeight > height) {
				initScale = height * 1.0f / dHeight;
			} else {
				initScale = Math.min(width * 1.0f / dWidth, height * 1.0f / dHeight);
			}

			//��������ֵ�������Сֵ
			mMinScale = initScale;
			mMaxScale = initScale * 4;

			//���㽫ͼƬ������Ļ���������X��Y�������
			float deltaX = (width - dWidth) / 2;
			float deltaY = (height - dHeight) / 2;

			//����ƽ�ƺ�����ĻΪ��������
			mScaleMatrix.postTranslate(deltaX, deltaY);
			mScaleMatrix.postScale(initScale, initScale, width / 2, height / 2);

			//����ͼ��任����
			setImageMatrix(mScaleMatrix);

			mScaleInited = true;
		}
	}

	@Override
	public boolean onScale(ScaleGestureDetector detector) {
		//��ȡImageView��ͼƬ�����ͼƬΪnullֱ�ӷ���
		if(getDrawable() == null) {
			return true;
		}

		//��ȡͼƬ��ǰ������ֵ�Լ��������Ӽ���������
		float currentScale = getCurrentScale();
		float scaleFactor = detector.getScaleFactor();

		//����ͼƬ��ǰ������ֵ�Լ��������ӽ��м���
		//����ǰ����ֵ�����������ֵ����С����ֵ֮�����������Ӳ�Ϊ1����м���
		//����ǰ����ֵ���������ӵĳ˻������������ֵ������������Ϊ�������ֵ���Ե�ǰ����ֵ
		//����ǰ����ֵ���������ӵĳ˻�������С����ֵ������������Ϊ��С����ֵ���Ե�ǰ����ֵ
		if((currentScale < mMaxScale && scaleFactor > 1.0f) || (currentScale > mMinScale && scaleFactor < 1.0f)) {
			if(currentScale * scaleFactor > mMaxScale) {
				scaleFactor = mMaxScale / currentScale;
			}
			if(currentScale * scaleFactor < mMinScale) {
				scaleFactor = mMinScale / currentScale;
			}

			//���������ƽ���Ϊ��������
			mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());

			//��̬��������ʱ�ı߽������
			adjustBorderAndCenterWhenZooming();

			//����ͼ��任����
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
		//�������Ƽ��������MotionEvent
		mScaleGestureDetector.onTouchEvent(event);

		//��ǰMotionEvent�µĽ�������
		float x = 0f;
		float y = 0f;

		//��ȡ��ǰMotionEvent�µ���ָ���������㽹������
		int pointerCount = event.getPointerCount();
		for(int i = 0; i < pointerCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}
		x /= pointerCount;
		y /= pointerCount;

		//����ǰMotionEvent�µ���ָ��������һ��MotionEvent����ʱ����ָ����������mLastX��mLastY��Ϊ��ǰ��������
		//�����ص������ӻ����ʱ��ִ�иò�����ʹ��deltaX��deltaY�ڴ��ص������ʱ����Ϊ0���������϶���Ϊ
		if(mLastPointerCount != pointerCount) {
			mLastX = x;
			mLastY = y;
			mLastPointerCount = pointerCount;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_MOVE:
			//�����϶���X��Y������
			float deltaX = x - mLastX;
			float deltaY = y - mLastY;

			//�ж��Ƿ�ﵽ���Խ����϶����ٽ�����
			if(!mCanDrag) {
				mCanDrag = isDragAction(deltaX, deltaY);
			}
			
			//�����������϶����ٽ�����������϶�
			if(mCanDrag) {
				mIsHorizontalAdjustNeeded = true;
				mIsVerticalAdjustNeeded = true;

				RectF rectF = getImageRectf();
				if(getDrawable() != null) {
					//���ͼƬ��ǰ���ڵľ�������Ŀ��С����ͼ������϶���X��������Ϊ0
					//ͬʱ��mIsHorizontalAdjustNeeded��Ϊfalse
					if(rectF.width() <= getWidth()) {
						deltaX = 0;
						mIsHorizontalAdjustNeeded = false;
					}
					//���ͼƬ��ǰ���ڵľ�������ĸ߶�С����ͼ�߶����϶���Y��������Ϊ0
					//ͬʱ��mIsVerticalAdjustNeeded��Ϊfalse
					if(rectF.height() <= getHeight()) {
						deltaY = 0;
						mIsVerticalAdjustNeeded = false;
					}
				}

				//����λ��
				mScaleMatrix.postTranslate(deltaX, deltaY);

				//��̬�����϶�ʱ�ı߽�
				adjustBorderWhenDragging();

				//����ͼ��任����
				setImageMatrix(mScaleMatrix);
			}

			//����һ��MotionEvent�¼�����ʱ�Ľ���������Ϊ��ǰ��������
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_UP:
			//����MotionEvent.ACTION_UPʱ��mLastPointerCount��Ϊ0����mCanDrag��Ϊfalse
			mLastPointerCount = 0;
			mCanDrag = false;
			break;
		case MotionEvent.ACTION_CANCEL:
			//����MotionEvent.ACTION_CANCELʱ��mLastPointerCount��Ϊ0����mCanDrag��Ϊfalse
			mLastPointerCount = 0;
			mCanDrag = false;
			break;
		default:
			break;
		}

		return true;
	}

}
