package cn.retech.activity;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import cn.retech.adapter.BookStoreAdapter;
import cn.retech.custom_control.MyGridLayout;
import cn.retech.domainbean_model.booklist_in_bookstores.BookInfo;
import cn.retech.domainbean_model.booklist_in_bookstores.BookListInBookstoresNetRequestBean;
import cn.retech.domainbean_model.booklist_in_bookstores.BookListInBookstoresNetRespondBean;
import cn.retech.domainbean_model.local_book_list.LocalBook;
import cn.retech.domainbean_model.local_book_list.LocalBookList;
import cn.retech.domainbean_model.login.LogonNetRespondBean;
import cn.retech.global_data_cache.GlobalConstant;
import cn.retech.global_data_cache.GlobalDataCacheForMemorySingleton;
import cn.retech.global_data_cache.GlobalDataCacheForNeedSaveToFileSystem;
import cn.retech.global_data_cache.LocalCacheDataPathConstant;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton.NetRequestIndex;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.IDomainBeanAsyncNetRespondListener;
import cn.retech.my_domainbean_engine.net_error_handle.NetErrorBean;
import cn.retech.toolutils.DebugLog;

public class BookListFragment extends Fragment implements IFragmentOptions {
	private final String TAG = this.getClass().getSimpleName();

	public static enum LogonStateEnum {
		PRIVATE_BOOK_STORE("private_book_store"), PUBLIC_BOOK_STORE("public_book_store");

		private String state;

		private LogonStateEnum(String state) {
			this.state = state;
		}

		public String getState() {
			return state;
		}

	}

	private NetRequestIndex netRequestIndexForBookListInBookstores = new NetRequestIndex();
	private BookStoreAdapter bookStoreAdapter;
	private String identifier;
	private MyGridLayout bookstoreGridView;
	// 书城图书列表(完成本地图书列表和从服务器新获取的图书列表进行了数据同步)
	private LocalBookList bookList = new LocalBookList();

	private RelativeLayout netResuqesttingLayout;;

	private LogonNetRespondBean bindAccount = new LogonNetRespondBean();

	public BookListFragment() {
		super();
	}

	@Override
	public void doSearch(String bookName, boolean isLoaclSearch) {
		if (null != bookList && bookList.size() != 0) {
			LocalBookList searchLocalBookList = new LocalBookList();

			for (int i = 0; i < bookList.size(); i++) {
				LocalBook localBook = bookList.get(i);
				BookInfo bookInfo = localBook.getBookInfo();
				if (bookInfo.getName().contains(bookName)) {
					searchLocalBookList.addBook(localBook);
				}
			}
			bookStoreAdapter = new BookStoreAdapter(getActivity());
			bookstoreGridView.setAdapter(bookStoreAdapter);
			bookStoreAdapter.changeDataSource(searchLocalBookList);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DebugLog.i(TAG, "onCreate");
		Bundle bundle = getArguments();
		identifier = bundle.getString("identifier");

		String loginState = bundle.getString("LogonState");
		if (LogonStateEnum.PRIVATE_BOOK_STORE.getState().equals(loginState)) {
			bindAccount = GlobalDataCacheForMemorySingleton.getInstance.getPrivateAccountLogonNetRespondBean();

		} else if (LogonStateEnum.PUBLIC_BOOK_STORE.getState().equals(loginState)) {
			bindAccount.setUsername(GlobalConstant.publicUserName);
			bindAccount.setPassword(GlobalConstant.publicUserPassword);
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		DebugLog.i(TAG, "onCreateView");
		return inflater.inflate(R.layout.fragment_book_list, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		netResuqesttingLayout = (RelativeLayout) getView().findViewById(R.id.net_requestting_layout);

		bookstoreGridView = (MyGridLayout) getView().findViewById(R.id.book_list_gridView);
		bookstoreGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final LocalBook book = bookList.get(position);
				switch (book.getBookStateEnum()) {
				case kBookStateEnum_Unpaid:// 未付费(只针对收费的书籍, 如果是免费的书籍, 会直接到下一个状态.
					showDialog();
					break;
				case kBookStateEnum_Paiding:// 支付中....

					break;
				case kBookStateEnum_Paid: // 已付费(已付费的书籍可以直接下载了)
					book.setBindAccount(bindAccount);
					book.startDownloadBook();
					// 将点击下载的书籍保存到GlobalDataCacheForMemorySingleton中
					GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().addBook(book);
					break;
				case kBookStateEnum_Free:
					book.setBindAccount(bindAccount);
					GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().addBook(book);
					book.startDownloadBook();
					// 将点击下载的书籍保存到GlobalDataCacheForMemorySingleton中

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
					GlobalDataCacheForNeedSaveToFileSystem.writeAllCacheData();
					break;
				}

			}
		});

		getBookList();
	}

	@Override
	public void refresh() {
		// 如果netResuqesttingLayout为已显示状态说明网络正在请求中,则不再重复执行网络请求操作
		if (null != netResuqesttingLayout && netResuqesttingLayout.getVisibility() != View.VISIBLE) {
			getBookList();
		}
	}

	private void getBookList() {
		netResuqesttingLayout.setVisibility(View.VISIBLE);

		BookListInBookstoresNetRequestBean netRequestBean = new BookListInBookstoresNetRequestBean();
		netRequestBean.setCategory_id(identifier);
		DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForBookListInBookstores, netRequestBean, new IDomainBeanAsyncNetRespondListener() {

			@Override
			public void onFailure(NetErrorBean error) {
				DebugLog.e(TAG, error.getErrorMessage());
			}

			@Override
			public void onSuccess(Object respondDomainBean) {
				netResuqesttingLayout.setVisibility(View.INVISIBLE);
				BookListInBookstoresNetRespondBean bookListInBookstoresNetRespondBean = (BookListInBookstoresNetRespondBean) respondDomainBean;
				// 1，转型从网络获取的书籍列表
				// 2，将网络获取的书籍列表与本地书籍列表比对
				// 3，如果相同则从本地书籍列表中获取数据，加入到localbook中
				// 4，如果不同则将网络返回的书籍加入到localBook中
				// 5，刷新数据
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
			}
		});
	}

	private void readDocument(final File file) {
		Intent intent = new Intent();
		intent.setClass(getActivity(), ShowBookActivity.class);
		intent.putExtra(ShowBookActivity.EXTRA_ZIP_FILE, file.getPath());
		startActivity(intent);
	}

	private void showDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.create();
		builder.setTitle("提示");
		builder.setMessage("暂不支持付费功能，请下载免费书籍");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		builder.show();
	}
}
