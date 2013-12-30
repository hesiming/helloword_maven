package cn.retech.custom_control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import cn.retech.activity.R;

public class MyScrollView extends PullDownAndUpView {
  private boolean canScroll = true;

  public MyScrollView(Context context) {
    super(context);
  }

  public MyScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  /**
   * 触碰之后,更改标志位,使View在onLayout时不再会自动上滑
   */
  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    canScroll = false;
    return super.onTouchEvent(ev);
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    super.onLayout(changed, l, t, r, b);

    ViewGroup viewGroup = (ViewGroup) getChildAt(0);
    if (viewGroup.getHeight() < getHeight()) {
      this.updateViewLayout(viewGroup, new LayoutParams(LayoutParams.MATCH_PARENT, getResources().getDisplayMetrics().heightPixels + 20));
    }

    if (canScroll) {
      this.scrollTo(0, (int) findViewById(R.id.bookstore_gridView).getY() - 10);
    }
  }

}
