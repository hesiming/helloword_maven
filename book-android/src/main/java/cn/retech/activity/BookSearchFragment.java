package cn.retech.activity;

import java.io.File;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import cn.retech.adapter.BookStoreAdapter;
import cn.retech.domainbean_model.book_search.BookSearchNetRequestBean;
import cn.retech.domainbean_model.booklist_in_bookstores.BookInfo;
import cn.retech.domainbean_model.booklist_in_bookstores.BookListInBookstoresNetRespondBean;
import cn.retech.domainbean_model.local_book_list.LocalBook;
import cn.retech.domainbean_model.local_book_list.LocalBookList;
import cn.retech.domainbean_model.login.LogonNetRespondBean;
import cn.retech.global_data_cache.GlobalDataCacheForMemorySingleton;
import cn.retech.global_data_cache.LocalCacheDataPathConstant;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton.NetRequestIndex;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.IDomainBeanAsyncNetRespondListener;
import cn.retech.my_domainbean_engine.net_error_handle.NetErrorBean;
import cn.retech.toolutils.DebugLog;

public class BookSearchFragment extends Fragment implements IFragmentOptions {
	private final String TAG = this.getClass().getSimpleName();
	private BookStoreAdapter bookStoreAdapter;
	private LocalBookList bookList = new LocalBookList();
	private GridView bookstoreGridView;
	private RelativeLayout netResuqesttingLayout;
	private View noDataImageView;
	private TextView noDataSmg;
	private final String nO_DATA_SMGString = "没有搜到\"";
	private NetRequestIndex netRequestIndexForBookSearch = new NetRequestIndex();

	private LogonNetRespondBean bindAccount = new LogonNetRespondBean();

	@Override
	public void doSearch(String bookName, boolean isLoaclSearch) {
		if (null != bookName && "".equals(bookName)) {
			noDataSmg.setText(nO_DATA_SMGString + bookName + "\"");
			noDataImageView.setVisibility(View.VISIBLE);
			bookStoreAdapter = new BookStoreAdapter(getActivity());
			bookstoreGridView.setAdapter(bookStoreAdapter);
			bookStoreAdapter.changeDataSource(new LocalBookList());

			return;
		}

		if (isLoaclSearch) {
			searchBooksFromLocation(bookName);
		} else {
			searchBooksFromNet(bookName);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_book_search, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		netResuqesttingLayout = (RelativeLayout) getView().findViewById(R.id.net_requestting_layout);
		noDataImageView = getView().findViewById(R.id.no_data_imageView);
		noDataSmg = (TextView) getView().findViewById(R.id.no_data_smg_textView);

		bookstoreGridView = (GridView) getView().findViewById(R.id.book_list_gridView);
		bookstoreGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final LocalBook book = bookList.get(position);
				switch (book.getBookStateEnum()) {
				case kBookStateEnum_Unpaid:// 未付费(只针对收费的书籍, 如果是免费的书籍, 会直接到下一个状态.

					break;
				case kBookStateEnum_Paiding:// 支付中....

					break;
				case kBookStateEnum_Paid: // 已付费(已付费的书籍可以直接下载了)
					book.setBindAccount(bindAccount);
					book.startDownloadBook();
					break;
				case kBookStateEnum_Downloading:// 正在下载中...
					book.stopDownloadBook();

					break;
				case kBookStateEnum_Pause:// 暂停(也就是未下载完成, 可以进行断点续传)
					book.startDownloadBook();
					break;
				case kBookStateEnum_GetBookDownloadUrl:
					book.stopDownloadBook();
					break;
				case kBookStateEnum_NotInstalled:// 未安装(已经下载完成, 还未完成安装)
					book.unzipBookZipResSelectorInBackground();
					break;
				case kBookStateEnum_Unziping:// 解压书籍zip资源包中....

					break;
				case kBookStateEnum_Installed:// 已安装(已经解压开的书籍, 可以正常阅读了)
					do {
						File tempFile = new File(LocalCacheDataPathConstant.localBookCachePath() + "/" + book.getBookInfo().getContent_id());
						if (!tempFile.exists()) {
							DebugLog.e(TAG, "压缩包不存在！");
						}
						readDocument(tempFile);
					} while (false);
					break;
				case kBookStateEnum_Update:// 有可以更新的内容

					break;
				default:

					break;
				}
				// 将点击下载的书籍保存到GlobalDataCacheForMemorySingleton中
				GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().addBook(book);
			}
		});

		Bundle bundle = getArguments();
		do {
			if (null == bundle) {
				break;
			}

			String bookName = bundle.getString("bookName");
			if (null != bookName) {
				doSearch(bookName, false);
			}

			String loginState = bundle.getString("LogonState", "no-data!");
			// if (LogonStateEnum.PRIVATE_BOOK_STORE.getState().equals(loginState)) {
			// bindAccount =
			// GlobalDataCacheForMemorySingleton.getInstance.getPrivateAccountLogonNetRespondBean();
			//
			// } else if
			// (LogonStateEnum.PUBLIC_BOOK_STORE.getState().equals(loginState)) {
			// bindAccount.setUsername(GlobalDataCacheForMemorySingleton.getInstance.getPublicUserNameString());
			// bindAccount.setPassword(GlobalDataCacheForMemorySingleton.getInstance.getPublicUserPasswordString());
			// }
			bindAccount.setUsername(GlobalDataCacheForMemorySingleton.getInstance.getUsernameForLastSuccessfulLogon());
			bindAccount.setPassword(GlobalDataCacheForMemorySingleton.getInstance.getPasswordForLastSuccessfulLogon());
		} while (false);
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub

	}

	private void readDocument(final File file) {
		Intent intent = new Intent();
		intent.setClass(getActivity(), ShowBookActivity.class);
		intent.putExtra(ShowBookActivity.EXTRA_ZIP_FILE, file.getPath());
		startActivity(intent);
	}

	private void searchBooksFromLocation(String searchContent) {
		bookList = new LocalBookList();
		LocalBookList localBookListFromLocal = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList();
		for (int i = 0; i < localBookListFromLocal.size(); i++) {
			DebugLog.e(TAG, localBookListFromLocal.get(i).getBookInfo().getName());
		}

		for (int i = 0; i < localBookListFromLocal.size(); i++) {
			LocalBook localBook = localBookListFromLocal.get(i);
			if (localBook.getBookInfo().getName().contains(searchContent)) {
				bookList.addBook(localBook);
			}
		}
		bookStoreAdapter = new BookStoreAdapter(getActivity());
		bookstoreGridView.setAdapter(bookStoreAdapter);
		bookList.deleteObservers();
		bookStoreAdapter.changeDataSource(bookList);

		if (bookList.size() == 0) {
			noDataSmg.setText(nO_DATA_SMGString + searchContent + "\"");
			noDataImageView.setVisibility(View.VISIBLE);
		} else {
			noDataSmg.setText("");
			noDataImageView.setVisibility(View.INVISIBLE);
		}
	}

	private void searchBooksFromNet(final String searchContent) {
		netResuqesttingLayout.setVisibility(View.VISIBLE);

		BookSearchNetRequestBean bookSearchNetRequestBean = new BookSearchNetRequestBean(searchContent);
		DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForBookSearch, bookSearchNetRequestBean, new IDomainBeanAsyncNetRespondListener() {

			@Override
			public void onFailure(NetErrorBean error) {
				DebugLog.e(TAG, "testSearchBook error = " + error.getErrorMessage());

				netResuqesttingLayout.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onSuccess(Object respondDomainBean) {
				DebugLog.e(TAG, "testSearchBook onSuccess = " + respondDomainBean);
				netResuqesttingLayout.setVisibility(View.INVISIBLE);

				bookList = new LocalBookList();

				BookListInBookstoresNetRespondBean bookListInBookstoresNetRespondBean = (BookListInBookstoresNetRespondBean) respondDomainBean;
				LocalBookList localBookListFromLocal = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList();// 本地
				for (BookInfo bookInfo : bookListInBookstoresNetRespondBean.getBookInfoList()) {
					LocalBook newBook = localBookListFromLocal.bookByContentID(bookInfo.getContent_id());
					if (newBook == null) {
						newBook = new LocalBook(bookInfo);
					} else {
						newBook.setBookInfo(bookInfo);
					}
					bookList.addBook(newBook);
				}
				bookStoreAdapter = new BookStoreAdapter(getActivity());
				bookstoreGridView.setAdapter(bookStoreAdapter);
				bookList.deleteObservers();
				bookStoreAdapter.changeDataSource(bookList);

				if (bookList.size() == 0) {
					noDataSmg.setText(nO_DATA_SMGString + searchContent + "\"");
					noDataImageView.setVisibility(View.VISIBLE);
				} else {
					noDataSmg.setText("");
					noDataImageView.setVisibility(View.INVISIBLE);
				}
			}
		});
	}

}
