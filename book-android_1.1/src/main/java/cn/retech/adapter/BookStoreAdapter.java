package cn.retech.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import cn.retech.custom_control.BookStoreBookCell;
import cn.retech.domainbean_model.local_book_list.LocalBook;
import cn.retech.domainbean_model.local_book_list.LocalBookList;

public class BookStoreAdapter extends BaseAdapter {
	private LocalBookList dataSource = new LocalBookList();
	private Context mContext;

	public BookStoreAdapter(Context context) {
		this.mContext = context;
	}

	public void changeDataSource(final LocalBookList newDataSource) {
		if (newDataSource == null) {
			assert false : "入参 newDataSource 为空. ";
			return;
		}

		this.dataSource = newDataSource;
		this.notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return dataSource.size();
	}

	@Override
	public Object getItem(int position) {
		return dataSource.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LocalBook book = (LocalBook) getItem(position);
		BookStoreBookCell bookCell = (BookStoreBookCell) convertView;
		if (bookCell == null) {
			bookCell = new BookStoreBookCell(mContext);
		}
		bookCell.bind(book);
		return bookCell;
	}

}
