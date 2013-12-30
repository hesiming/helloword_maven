package cn.retech.custom_control;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import cn.retech.activity.R;

public class MyGridLayout extends GridLayout {
  private DataSetObserver dataSetObserver;
  protected int margin;
  protected BaseAdapter adapter;
  protected OnItemClickListener onItemClickListener;

  public MyGridLayout(Context context) {
    super(context);
    init();
  }

  public MyGridLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public MyGridLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  @Override
  public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
    super.addView(child, index, params);

    LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
    layoutParams.width = LayoutParams.WRAP_CONTENT;
    layoutParams.height = LayoutParams.WRAP_CONTENT;
    layoutParams.topMargin = margin;
    layoutParams.leftMargin = margin;
    layoutParams.rightMargin = margin;
  }

  /**
   * @param adapter the adapter to set
   */
  public void setAdapter(BaseAdapter newAdapter) {
    adapter = newAdapter;

    adapter.registerDataSetObserver(dataSetObserver);

    updateData();
  }

  /**
   * @param onItemClickListener the onItemClickListener to set
   */
  public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
    this.onItemClickListener = onItemClickListener;
  }

  public void updateData() {
    if (null != adapter) {
      this.removeAllViews();

      int screenWidth = getResources().getDisplayMetrics().widthPixels;
      int bookCellWidth = (int) getResources().getDimension(R.dimen.book_folder_width);
      // int eachColumnBookNumber = screenWidth / (bookCellWidth + 2 * 10);
      int eachColumnBookNumber = Integer.parseInt(getResources().getString(R.string.eachColumnBookNumber));
      margin = (screenWidth - eachColumnBookNumber * bookCellWidth) / (2 * eachColumnBookNumber);

      this.setColumnCount(eachColumnBookNumber);

      for (int i = 0; i < adapter.getCount(); i++) {
        final View view = adapter.getView(i, null, this);
        final int position = i;
        final long id = adapter.getItemId(position);

        view.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
            if (null != onItemClickListener) {
              onItemClickListener.onItemClick(null, view, position, id);
            }
          }
        });

        this.addView(view);
      }
    }
  }

  private void init() {
    dataSetObserver = new DataSetObserver() {
      @Override
      public void onChanged() {
        updateData();
      }

      @Override
      public void onInvalidated() {
        updateData();
      }
    };
  }
}
