package cn.retech.custom_control;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import cn.retech.activity.R;

public class CircleProgressBar extends View {
	private final String TAG = this.getClass().getSimpleName();

	public static enum ShowViewTypeEnum {
		// 暂停状态
		Suspended_state,
		// 下载状态
		Download_Status,
		// 联网状态
		Networking_status
	}

	private Handler handler;
	private float degress = 10;// 画布旋转角度

	private int maxProgress = 100;
	private float progress = 0;
	// 未完成的进度圈的宽度
	private int progressStrokeWidth = Integer.parseInt(getResources().getString(R.string.progressStrokeWidth));
	// 已经完成的进度圈的宽度
	private int finishProgressStrokeWidth = Integer.parseInt(getResources().getString(R.string.finishProgressStrokeWidth));// 默认5
	// 画圆所在的距形区域
	RectF oval;
	Paint paint;

	// 暂停时两个矩形的间距
	int pauseThePitch = 5;// 默认12

	public CircleProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO 自动生成的构造函数存根
		oval = new RectF();
		paint = new Paint();
		handler = new Handler();
	}

	public int getMaxProgress() {
		return maxProgress;
	}

	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
	}

	public void setPauseThePitch(int pauseThePitch) {
		this.pauseThePitch = pauseThePitch;
	}

	/**
	 * 非ＵＩ线程调用
	 */
	public void setProgressNotInUiThread(float progress) {
		this.progress = progress;
		this.invalidate();
	}

	private ShowViewTypeEnum showViewTypeEnum;

	public void setShowViewTypeEnum(ShowViewTypeEnum showViewTypeEnum) {
		this.showViewTypeEnum = showViewTypeEnum;
		this.invalidate();
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		switch (this.showViewTypeEnum) {
		case Download_Status:// 下载状态
			drawOnSuccessfulConnect(canvas, true);
			break;
		case Suspended_state:// 暂停状态
			drawOnSuccessfulConnect(canvas, false);
			break;
		case Networking_status:// 联网状态
			canvas.rotate(degress, canvas.getWidth() / 2, canvas.getHeight() / 2);
			degress += 10;
			drawOnConnecting(canvas);

			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					handler.post(new Runnable() {
						@Override
						public void run() {
							CircleProgressBar.this.invalidate();
						}
					});
				}
			}, 25);
			break;
		default:
			break;
		}
	}

	// 网络连接中的画图效果
	private void drawOnConnecting(Canvas canvas) {
		int width = this.getWidth();
		int height = this.getHeight();

		if (width != height) {
			int min = Math.min(width, height);
			width = min;
			height = min;
		}

		paint.setAntiAlias(true); // 设置画笔为抗锯齿
		paint.setColor(Color.WHITE); // 设置画笔颜色
		canvas.drawColor(Color.TRANSPARENT); // 白色背景
		// 这里绘制未完成的进度
		paint.setStrokeWidth(progressStrokeWidth); // 线宽
		paint.setStyle(Style.STROKE);

		oval.left = progressStrokeWidth / 2; // 左上角x
		oval.top = progressStrokeWidth / 2; // 左上角y
		oval.right = width - progressStrokeWidth / 2; // 左下角x
		oval.bottom = height - progressStrokeWidth / 2; // 右下角y
		paint.setColor(Color.rgb(59, 175, 218));
		canvas.drawArc(oval, 0, 342, false, paint); // 绘制白色圆圈，即进度条背景
	}

	// 网络连接成功后的画图效果
	private void drawOnSuccessfulConnect(Canvas canvas, boolean isRun) {
		int width = this.getWidth();
		int height = this.getHeight();

		if (width != height) {
			int min = Math.min(width, height);
			width = min;
			height = min;
		}

		paint.setAntiAlias(true); // 设置画笔为抗锯齿
		paint.setColor(Color.WHITE); // 设置画笔颜色
		canvas.drawColor(Color.TRANSPARENT); // 白色背景
		// 这里绘制未完成的进度
		paint.setStrokeWidth(progressStrokeWidth); // 线宽
		paint.setStyle(Style.STROKE);

		oval.left = progressStrokeWidth / 2; // 左上角x
		oval.top = progressStrokeWidth / 2; // 左上角y
		oval.right = width - progressStrokeWidth / 2; // 左下角x
		oval.bottom = height - progressStrokeWidth / 2; // 右下角y
		paint.setColor(Color.rgb(59, 175, 218));
		canvas.drawArc(oval, -90, 360, false, paint); // 绘制白色圆圈，即进度条背景
		// 这里绘制已经完成的进度
		oval.left = progressStrokeWidth / 2 + finishProgressStrokeWidth; // 左上角x
		oval.top = progressStrokeWidth / 2 + finishProgressStrokeWidth; // 左上角y
		oval.right = width - progressStrokeWidth / 2 - finishProgressStrokeWidth; // 左下角x
		oval.bottom = height - progressStrokeWidth / 2 - finishProgressStrokeWidth; // 右下角y
		paint.setColor(Color.rgb(59, 175, 218));
		paint.setStrokeWidth(progressStrokeWidth + finishProgressStrokeWidth); // 已完成线宽
		canvas.drawArc(oval, -90, (progress / maxProgress) * 360, false, paint); // 绘制进度圆弧，这里是蓝色
		// 绘制正方形
		paint.setStyle(Style.FILL);// 设置填满
		if (!isRun) {
			// 暂停
			canvas.drawRect(width / 3, height / 3, width / 3 + pauseThePitch, height / 3 * 2, paint);//
			canvas.drawRect(width / 3 * 2 - pauseThePitch, height / 3, width / 3 * 2, height / 3 * 2, paint);//
		} else {
			// 正方形
			canvas.drawRect(width / 3, height / 3, width / 3 * 2, height / 3 * 2, paint);//
		}
	}

}
