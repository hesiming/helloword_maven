package cn.retech.custom_control;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import cn.retech.activity.R;
import cn.retech.domainbean_model.local_book_list.LocalBook;
import cn.retech.domainbean_model.local_book_list.LocalBookList;
import cn.retech.global_data_cache.GlobalDataCacheForMemorySingleton;

public class SearchLayout extends RelativeLayout {
	private AutoCompleteTextView searchEditText;
	private ImageButton mImageButton;
	private OnCloseListener onCloseListener;
	private OnQueryTextListener onQueryTextListener;

	public SearchLayout(Context context) {
		super(context);

		init();
	}

	public SearchLayout(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	public SearchLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init();
	}

	public void closeSearch() {
		if (onCloseListener != null) {
			onCloseListener.onClose();
		}
	}

	/**
	 * 初始化搜索控件的图片,即自定义风格
	 * 
	 * @param imageViewIconId
	 *            放大镜icon的资源id
	 * @param editTextIconId
	 *            输入框背景图的资源id
	 * @param imageButtonIconId
	 *            取消按钮背景图的资源id
	 */
	public void setIconBackground(int imageViewIconId, int editTextIconId, int imageButtonIconId) {
		ImageView searchHeadImageView = (ImageView) findViewById(R.id.search_head_imageView);
		searchHeadImageView.setBackgroundDrawable(getResources().getDrawable(imageViewIconId));

		searchEditText.setBackgroundDrawable(getResources().getDrawable(editTextIconId));

		mImageButton.setBackgroundDrawable(getResources().getDrawable(imageButtonIconId));
	}

	public void setOnCloseListener(OnCloseListener onCloseListener) {
		this.onCloseListener = onCloseListener;
	}

	public void setOnQueryTextListener(OnQueryTextListener onQueryTextListener) {
		this.onQueryTextListener = onQueryTextListener;
	}

	/**
	 * 设置搜索建议
	 * 
	 * @param suggestions
	 *            建议数组
	 */
	private void setSuggestions() {
		// 设置搜索建议
		LocalBookList localBookList = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList();
		String[] bookNames = new String[localBookList.size()];
		if (0 != localBookList.size()) {
			for (int i = 0; i < localBookList.size(); i++) {
				LocalBook localBook = localBookList.get(i);
				bookNames[i] = localBook.getBookInfo().getName();
			}
		}
		ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, bookNames);
		searchEditText.setAdapter(adapter);
	}

	/**
	 * 显示或者隐藏输入法
	 * 
	 * @param isShow
	 *            标记是否显示或隐藏
	 */
	public void showOrHidenInput(boolean isShow) {
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (isShow) {
			imm.showSoftInput(searchEditText, InputMethodManager.SHOW_FORCED);
		} else {
			imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
			searchEditText.setText("");
		}
	}

	private void init() {
		LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.search_view_layout, this);
		searchEditText = (AutoCompleteTextView) findViewById(R.id.search_editText);
		searchEditText.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				setSuggestions();
				return false;
			}
		});
		searchEditText.setOnKeyListener(new OnKeyListener() {
			@Override
			/**
			 * 捕获输入法的KEYCODE_ENTER事件,回调onQueryTextListener.onQueryTextSubmit()方法
			 */
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					if (null != onQueryTextListener) {
						onQueryTextListener.onQueryTextSubmit(((AutoCompleteTextView) v).getEditableText().toString());
					}

					return true;
				} else {
					return false;
				}
			}
		});

		mImageButton = (ImageButton) findViewById(R.id.cancel_button_ImageButton);
		mImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onCloseListener != null) {
					onCloseListener.onClose();
				}
			}
		});
	}

}
