package cn.retech.custom_control;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import cn.retech.activity.R;

public class BookFolderLayout extends RelativeLayout {
  public ArrayList<View> childViews;
  private ViewGroup gridLayout;

  public BookFolderLayout(Context context) {
    super(context);

    init();
  }

  public BookFolderLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    init();
  }

  public BookFolderLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    init();
  }

  public void addChildView(View childView) {
    if (null != childView.getTag()) {
      ((BookFolderLayout) childView.getTag()).removeChildView(childView);
    }
    if (null != childView.getParent()) {
      ((ViewGroup) childView.getParent()).removeView(childView);
    }

    childView.setTag(this);
    childViews.add(childView);

    ImageView bookFaceImageView = new ImageView(getContext());
    bookFaceImageView.setScaleType(ScaleType.FIT_XY);
    ImageLoader.getInstance().displayImage(((BookShelfBookCell) childView).getThumbnail(), bookFaceImageView);

    android.widget.GridLayout.LayoutParams layoutParams = new android.widget.GridLayout.LayoutParams();
    layoutParams.width = (int) getResources().getDimension(R.dimen.book_cell_width) / 3;
    layoutParams.height = (int) getResources().getDimension(R.dimen.book_image_height) / 3;
    gridLayout.addView(bookFaceImageView, 0, layoutParams);
    childView.setTag(R.id.bookFaceImageView, bookFaceImageView);
  }

  /**
   * @return the childViews
   */
  public ArrayList<View> getChildViews() {
    return childViews;
  }

  public void removeChildView(View childView) {
    gridLayout.removeView((View) childView.getTag(R.id.bookFaceImageView));

    if (childViews.contains(childView)) {
      childViews.remove(childView);
    }

    if (0 == childViews.size()) {
      ((ViewGroup) this.getParent()).removeView(this);
    }

    childView.setTag(null);
  }

  private void init() {
    childViews = new ArrayList<View>();

    LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    layoutInflater.inflate(R.layout.book_folder_layout, this);
    gridLayout = (ViewGroup) findViewById(R.id.book_folder_gridLayout);
  }
}
