package main.java.cn.retech.custom_control;

import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import cn.retech.activity.R;

@SuppressLint("HandlerLeak")
public class WelcomeCustomControl extends FrameLayout {

  public enum WelcomeCustomControlTypeEnum {
    FINISH
  }

  private ImageView moveBackgroundImage;
  private View welcome_layout;
  // 设置动画前界面显示时间
  private int intervalTime = 500;
  // 设置图片移动时间
  private int durationTime = 2000;
  // 设置图片放大时间
  private int scaleTime = 1000;

  private ICustomControlDelegate delegate;
  TimerTask task = new TimerTask() {
    @Override
    public void run() {
      Message message = new Message();
      message.what = 1;
      handler.sendMessage(message);
    }
  };
  final Handler handler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case 1:
          moveBackgroundImage.animate().x(-(WelcomeCustomControl.this.getRight()) / 4).setDuration(durationTime).setListener(
              moveAnimatorListener);
          break;
      }
      super.handleMessage(msg);
    }
  };

  private AnimatorListener moveAnimatorListener = new AnimatorListener() {

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
      welcome_layout.animate().scaleX((float) 1.5).scaleY((float) 1.5).alpha(0).setDuration(scaleTime).setListener(scaleAnimatorListener);

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationStart(Animator animation) {

    }
  };

  private AnimatorListener scaleAnimatorListener = new AnimatorListener() {

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationEnd(Animator animation) {
      if (delegate != null) {
        delegate.customControlOnAction(this, WelcomeCustomControlTypeEnum.FINISH);
      }
    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public void onAnimationStart(Animator animation) {

    }
  };

  public WelcomeCustomControl(Context context, AttributeSet attrs) {
    super(context, attrs);
    final LayoutInflater inflaterInstance = LayoutInflater.from(context);
    inflaterInstance.inflate(R.layout.welcome_coustom_control_layout, this);

    welcome_layout = findViewById(R.id.welcome_layout);
    moveBackgroundImage = (ImageView) findViewById(R.id.move_imageView); // ImageView对象
    Timer timer = new Timer(true);
    timer.schedule(task, intervalTime); // 延时1000ms后执行，1000ms执行一次

  }

  public int getDurationTime() {
    return durationTime;
  }

  public int getIntervalTime() {
    return intervalTime;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return true;
  }

  public void setDelegate(ICustomControlDelegate delegate) {
    this.delegate = delegate;
  }

  public void setDurationTime(int durationTime) {
    this.durationTime = durationTime;
  }

  public void setIntervalTime(int intervalTime) {
    this.intervalTime = intervalTime;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

  }
}
