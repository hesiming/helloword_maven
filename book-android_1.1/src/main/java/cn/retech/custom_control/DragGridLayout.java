package cn.retech.custom_control;

import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import cn.retech.activity.R;

public class DragGridLayout extends MyGridLayout {
  private Handler handler;

  private boolean isOut = false;// 判断是否超出屏幕/
  private float currentX = 0;
  private boolean isLeft = true;

  private OnLongClickListener onLongClickListener;
  private OnDragListener viewDragListener;
  private OnDragListener viewGroupDragListener;

  // 为了防止ViewGroup和View同时监听drag事件而起冲突:true-ViewGroup可监听;false-View监听
  private boolean isItemHandleDragEvent = false;

  public DragGridLayout(Context context) {
    super(context);
    init();
  }

  public DragGridLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public DragGridLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  @Override
  public void updateData() {
    super.updateData();

    for (int i = 0; i < getChildCount(); i++) {
      final View view = getChildAt(i);
      view.setOnLongClickListener(onLongClickListener);
      view.setOnDragListener(viewDragListener);
      view.setTag(R.id.timer, new Timer());
      view.setTag(R.id.isPreparing, false);
    }
  }

  @SuppressLint("DrawAllocation")
  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (getHeight() < ((View) getParent()).getHeight()) {
      this.getLayoutParams().height = getResources().getDisplayMetrics().heightPixels - 20;
      ((ViewGroup) getParent()).updateViewLayout(this, this.getLayoutParams());
    }
  }

  @SuppressLint("HandlerLeak")
  private void init() {
    handler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        ViewGroupAddAndRemove viewGroupAddAndRemove = (ViewGroupAddAndRemove) msg.obj;
        ViewGroup parentView = viewGroupAddAndRemove.getViewGroup();
        final View view = viewGroupAddAndRemove.getListenerView();
        final View dragView = viewGroupAddAndRemove.getDragView();

        /**
         * 能够触发可位移操作的两个条件: 1.停留超过指定秒数后并继续拖动,根据左移入还是右移入来判断当坐标与"监听View的2分之1宽度值"的值 2.拖动View是一个CellView
         */
        if (msg.what == 1 || dragView instanceof BookFolderLayout) {
          // 为了防止监听View在执行布局调整动画时仍然继续监听onDrag事件,并然后在0.35秒后恢复其监听功能
          view.setTag(R.id.isMoving, true);
          new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  view.setTag(R.id.isMoving, false);
                  view.setTag(R.id.isPreparing, false);
                  view.animate().alpha((float) 1.0).setListener(null);
                }
              });
            }
          }, 350);

          // 清空拖动View和与其相关的CellView的关联状态
          if (null != dragView.getTag()) {
            BookFolderLayout cellView = (BookFolderLayout) dragView.getTag();
            cellView.removeChildView(dragView);
            dragView.setTag(null);
          }

          /**
           * 先删除,然后再添加到监听View之前(即在执行删除拖动View的操作之前)的索引位置
           */
          if (null != ((ViewGroup) dragView.getParent())) {
            ((ViewGroup) dragView.getParent()).removeView(dragView);
          }
          parentView.addView(dragView, msg.arg1);

          // 强制执行一次布局动画,使子元素能够正常排列(防止错乱现象)
          ((ViewGroup) dragView.getParent()).scheduleLayoutAnimation();

        } else {
          /**
           * 不满足移动条件时,则触发闪烁动画
           */
          view.animate().alpha((float) 0.3).setDuration(400).setListener(new AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
              if (view.getAlpha() == (float) 1.0) {
                view.animate().setDuration(400).alpha((float) 0.3);
              } else {
                view.animate().setDuration(400).alpha((float) 1.0);
              }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }
          });
          view.setTag(R.id.isPreparing, true);// 表示正处于准备状态,可以随时发生交互行为
        }
      }
    };

    onLongClickListener = new OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        // 设置数据,会被拖动目标ViewGroup所接收,即用来传递信息
        ClipData data = ClipData.newPlainText("", "");

        // 设置拖动阴影,即你所拖动的那个图标(这也同时说明,你拖动的不过是一个影分身,本尊其实并没有移动)
        DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
        view.startDrag(data, shadowBuilder, view, 0);

        view.setAlpha(0);

        isItemHandleDragEvent = false;

        return true;
      }
    };

    // 每个单独View的拖动监听
    viewDragListener = new OnDragListener() {
      @SuppressLint("NewApi")
      @Override
      public boolean onDrag(final View view, DragEvent event) {
        // 如果监听View正在执行一个移动动画则直接return
        if (null != view.getTag(R.id.isMoving) && (Boolean) view.getTag(R.id.isMoving)) {
          return false;
        }

        final View dragView = (View) event.getLocalState();

        // 如果拖动事件遮蔽自己则直接return
        if (view.equals(dragView)) {
          return false;
        }

        final GridLayout parentView = (GridLayout) view.getParent();
        if (null == parentView) {
          return false;
        }

        final int index = parentView.indexOfChild(view);

        Timer timer = (Timer) view.getTag(R.id.timer);

        switch (event.getAction()) {
          case DragEvent.ACTION_DRAG_ENTERED:
            // 开启计时器,在只有当拖动View在监听View处停留0.25秒后才会有相应效果
            timer.schedule(new TimerTask() {
              @Override
              public void run() {
                handler.post(new Runnable() {
                  @Override
                  public void run() {
                    ViewGroupAddAndRemove viewGroupAddAndRemove = new ViewGroupAddAndRemove();
                    viewGroupAddAndRemove.setViewGroup(parentView);
                    viewGroupAddAndRemove.setListenerView(view);
                    viewGroupAddAndRemove.setDragView(dragView);

                    // 发送Message以触发Handle处理,其中包含ViewGroupAnddRemove以及监听View的索引
                    Message message = new Message();
                    message.obj = viewGroupAddAndRemove;
                    message.arg1 = index;// 监听View索引

                    handler.sendMessage(message);
                  }
                });
              }
            }, 250);

            return true;

            /**
             * 触发可以移动的两个条件之1:当监听View处于可以操作状态(即停留了0.25秒,这里采用alpha值来判断,因为停留0.25秒后监听View会执行闪烁动画,
             * 改变alpha值。 但这不是一个可靠的参数,建议专门使用一个标记位来进行标识),并且继续移动超过了"监听View的2分之1宽度值"
             */
          case DragEvent.ACTION_DRAG_LOCATION:
            boolean canMove = false;// 标记是否超过了"监听View的2分之1宽度值"

            // 这个isLeft的赋值操作在ViewGroup的拖动监听中执行
            if (isLeft) {
              canMove = event.getX() >= view.getWidth() * 3 / 4;
            } else {
              canMove = event.getX() <= view.getWidth() / 4;
            }

            if ((Boolean) view.getTag(R.id.isPreparing) && canMove) {
              ViewGroupAddAndRemove viewGroupAddAndRemove = new ViewGroupAddAndRemove();
              viewGroupAddAndRemove.setViewGroup(parentView);
              viewGroupAddAndRemove.setListenerView(view);
              viewGroupAddAndRemove.setDragView(dragView);

              Message message = new Message();
              message.obj = viewGroupAddAndRemove;
              message.arg1 = index;
              message.what = 1;// 直接设置为1,当handler处理时,将直接判定为能够触发可位移操作

              handler.sendMessage(message);
            }

            return true;

          case DragEvent.ACTION_DRAG_EXITED:
            view.setTag(R.id.isPreparing, false);
            view.animate().alpha((float) 1.0).setListener(null);

            // 取消定时器,并重置
            timer.cancel();
            view.setTag(R.id.timer, new Timer());

            return true;

          case DragEvent.ACTION_DROP:
            // 取消定时器,并重置
            timer.cancel();
            view.setTag(R.id.timer, new Timer());

            if ((Boolean) view.getTag(R.id.isPreparing)) {
              view.setTag(R.id.isPreparing, false);
              view.animate().alpha((float) 1.0).setListener(null);

              if (view instanceof BookFolderLayout) {
                ((BookFolderLayout) view).addChildView(dragView);
              } else {
                // 新建一个CellView,并初始化相应数据
                BookFolderLayout cellView = new BookFolderLayout(getContext());
                cellView.setTag(R.id.timer, new Timer());
                cellView.setTag(R.id.isPreparing, false);
                cellView.setOnDragListener(viewDragListener);
                cellView.setOnLongClickListener(onLongClickListener);
                cellView.setOnClickListener(new OnClickListener() {
                  /**
                   * 点击打开文件夹界面
                   */
                  @Override
                  public void onClick(View v) {
                    // TODO
                  }
                });

                cellView.addChildView(view);
                cellView.addChildView(dragView);
                if (index >= parentView.getChildCount()) {
                  parentView.addView(cellView);
                } else {
                  parentView.addView(cellView, index);
                }

              }

              isItemHandleDragEvent = true;
            }

            // 此处返回false是为了让ViewGroup能够捕获到DragEvent.ACTION_DROP事件!!!
            return false;
          case DragEvent.ACTION_DRAG_ENDED:
            view.setTag(R.id.isPreparing, false);
            view.animate().alpha((float) 1.0).setListener(null);

            // 判断是否被拖出Activity之外,如果是则将拖动View复原
            if (isOut) {
              dragView.setAlpha(1);
            }

            return false;

          default:

            return true;
        }
      }
    };

    // ViewGroup的拖动监听
    viewGroupDragListener = new OnDragListener() {
      @Override
      public boolean onDrag(View view, DragEvent event) {
        final View dragView = (View) event.getLocalState();
        switch (event.getAction()) {
          case DragEvent.ACTION_DROP:
            dragView.setOnDragListener(viewDragListener);

            float e_x = event.getX() - dragView.getWidth() / 2;
            float e_y = event.getY() - dragView.getHeight() / 2;

            if (!isItemHandleDragEvent) {
              dragView.setAlpha(1);

              if (null != dragView.getTag()) {
                /**
                 * 这里是拖动View从一个文件夹拖动至桌面的情况分支。首先清空拖动View和与其相关的CellView的关联状态，然后再将拖动View加入到桌面上。这里，
                 * 在添加操作中是没有动画效果的，可以考虑添加一下...//TODO
                 */
                BookFolderLayout cellView = (BookFolderLayout) dragView.getTag();
                cellView.removeChildView(dragView);
                dragView.setTag(null);

                if (null != ((ViewGroup) dragView.getParent())) {
                  ((ViewGroup) dragView.getParent()).removeView(dragView);
                }

                DragGridLayout.this.addView(dragView);

                // 重新添加个Listener
                dragView.setOnDragListener(viewDragListener);
                dragView.setOnLongClickListener(onLongClickListener);

              } else {
                /**
                 * 这里是拖动View处于桌面拖动的情况分支。只须执行 从拖动点移回至原位的动画。
                 */
                float x = dragView.getX();
                float y = dragView.getY();

                dragView.setX(e_x);
                dragView.setY(e_y);

                dragView.animate().x(x).y(y).setDuration(350);
              }
            } else {
              // 两个View的移位动画未结束时就拦截住DROP事件处理方案
              // TODO
            }

            break;
          case DragEvent.ACTION_DRAG_ENTERED:
            isOut = false;

            // 记录拖动开始位置坐标,并隐藏文件夹界面
            currentX = event.getX();
            // TODO
            // hiddenLayout();

            break;

          case DragEvent.ACTION_DRAG_LOCATION:
            // 判断是左移还是右移,这个值将会在每个单独View的拖动监听中判断是否可触发移动效果时用到
            if (event.getX() > currentX) {
              isLeft = true;
            } else {
              isLeft = false;
            }

            currentX = event.getX();

            // dragView.setX(event.getX() - dragView.getWidth() / 2);
            // dragView.setY(event.getY() - dragView.getHeight() / 2);

            break;
          case DragEvent.ACTION_DRAG_EXITED:
            isOut = true;

            break;
          case DragEvent.ACTION_DRAG_ENDED:
            // 判断是否被拖出Activity之外,如果是则将拖动View复原
            if (isOut) {
              dragView.setAlpha(1);
            }

            return false;

          default:
            break;
        }

        return true;
      }
    };
    this.setOnDragListener(viewGroupDragListener);
  }
}
