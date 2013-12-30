package main.java.cn.retech.custom_control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import cn.retech.activity.R;

public class PageTitle extends LinearLayout {
  private ICustomControlDelegate iCustomControlDelegate;// 控制层抽象,即Activity,用以处理按钮的点击事件
  private View backView;
  private ImageButton refreshButton;
  private ImageButton doSearchButton;
  private SearchLayout searchLayout;
  private TextView titleTextView;
  private RelativeLayout relativeLayout;

  public PageTitle(final Context context) {
    super(context);

    init(context);
  }

  public PageTitle(Context context, AttributeSet attrs) {
    super(context, attrs);

    init(context);
  }

  /**
   * 关闭搜索框,并触发回调方法
   */
  public void closeSearchView() {
    searchLayout.closeSearch();
  }

  /**
   * @param iCustomControlDelegate the iCustomControlDelegate to set
   */
  public void setiCustomControlDelegate(ICustomControlDelegate iCustomControlDelegate) {
    this.iCustomControlDelegate = iCustomControlDelegate;
  }

  public void setTitle(String title) {
    titleTextView.setText(title);
  }

  @SuppressLint("ResourceAsColor")
  private void init(Context context) {
    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.page_title_layout, this);

    relativeLayout = (RelativeLayout) findViewById(R.id.page_title_relativeLayout);
    titleTextView = (TextView) findViewById(R.id.title_TextView);

    // 返回按钮
    backView = findViewById(R.id.cancel_button);
    backView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        iCustomControlDelegate.customControlOnAction(v, ControlOnActionEnum.BACK_TO_MYBOOKSHLF);
      }
    });

    // 刷新按钮
    refreshButton = (ImageButton) findViewById(R.id.refresh_ImageButton);
    refreshButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        iCustomControlDelegate.customControlOnAction(v, ControlOnActionEnum.REFRESH);
      }
    });

    // 搜索框
    searchLayout = (SearchLayout) findViewById(R.id.search_SearchView);
    searchLayout.setY(200);
    final int searchViewIndex = relativeLayout.indexOfChild(searchLayout);
    searchLayout.setOnCloseListener(new OnCloseListener() {
      @Override
      public boolean onClose() {
        searchLayout.animate().y(200);
        searchLayout.showOrHidenInput(false);// 隐藏键盘

        for (int i = 0; i < relativeLayout.getChildCount(); i++) {
          if (i != searchViewIndex) {
            final View view = relativeLayout.getChildAt(i);
            final float y = (Float) view.getTag();

            view.animate().y(y);
          }
        }

        iCustomControlDelegate.customControlOnAction(null, ControlOnActionEnum.CLOSE_SEARCH);

        return false;
      }
    });
    searchLayout.setOnQueryTextListener(new OnQueryTextListener() {
      @Override
      public boolean onQueryTextChange(String newText) {
        return true;
      }

      @Override
      public boolean onQueryTextSubmit(String query) {
        iCustomControlDelegate.customControlOnAction(query, ControlOnActionEnum.SHOW_SEARCH);

        return true;
      }
    });

    // 搜索按钮
    doSearchButton = (ImageButton) findViewById(R.id.doSearch_ImageButton);
    doSearchButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        searchLayout.animate().y(0);
        searchLayout.showOrHidenInput(true);// 显示键盘

        for (int i = 0; i < relativeLayout.getChildCount(); i++) {
          if (i != searchViewIndex) {
            final View view = relativeLayout.getChildAt(i);
            if (null == view.getTag()) {
              final float y = view.getY();
              view.setTag(y);
            }

            view.animate().y(-100);
          }
        }

        iCustomControlDelegate.customControlOnAction(null, ControlOnActionEnum.OPEN_SEARCH);
      }
    });
  }
}
