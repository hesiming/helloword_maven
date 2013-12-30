package main.java.cn.retech.activity;

import java.io.File;

import main.java.cn.retech.adapter.BookShelfAdapter;
import main.java.cn.retech.custom_control.MyGridLayout;
import main.java.cn.retech.custom_control.SearchLayout;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;
import cn.retech.activity.R;
import cn.retech.domainbean_model.local_book_list.LocalBook;
import cn.retech.domainbean_model.local_book_list.LocalBookList;
import cn.retech.domainbean_model.login.LogonNetRequestBean;
import cn.retech.domainbean_model.login.LogonNetRespondBean;
import cn.retech.global_data_cache.GlobalDataCacheForMemorySingleton;
import cn.retech.global_data_cache.LocalCacheDataPathConstant;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton.NetRequestIndex;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.IDomainBeanAsyncNetRespondListener;
import cn.retech.my_domainbean_engine.net_error_handle.NetErrorBean;
import cn.retech.toolutils.DebugLog;
import cn.retech.toolutils.ToolsFunctionForThisProgect;

import com.umeng.analytics.MobclickAgent;

public class BookShelfActivity extends Activity {
	private final String TAG = this.getClass().getSimpleName();
	// 开机动画是否完成
	private NetRequestIndex netRequestIndexForLoginPublicLibrary = new NetRequestIndex();
	private NetRequestIndex netRequestIndexForLoginPrivateLibrary = new NetRequestIndex();
	private FragmentManager mFragmentManager = getFragmentManager();
	private Fragment searchFragment;
	private View searchContentLayout;// 展示搜索结果的层
	private Button publicBookstoreButton;
	private Button privateBookstoreButton;
	private MyGridLayout bookstoreGridView;
	private SearchLayout mSearchLayout;// 搜索控件
	// 用户退出相关
	private TextView quiteLoginTextView;
	private View bookshelf_bottom_layout;
	// 记录用户登录到书城/企业
	private BookListFragment.LogonStateEnum logonStateEnum;
	// ///////////////解压书籍相关
	public static final String PREFERENCE_FILE = "preference_file";
	public static final String ORIENTATION = "orientation";

	private BookShelfAdapter bookShelfAdapter;

	public void onClickForNothing(View view) {
		//
	}

	// 判断横竖屏状态
	@Override
	public void onConfigurationChanged(Configuration config) {
		super.onConfigurationChanged(config);
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// 横屏显示
			setWelcomeViewLandScape();
			// welcomeCustomControl
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			// 竖屏显示
			setWelcomeViewPortrait();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showDialogQuiteApp();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		DebugLog.i(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.book_shelf_layout);

		// 书院
		publicBookstoreButton = (Button) findViewById(R.id.public_bookstore_button);
		publicBookstoreButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// publicLibraryButtonOnClickListener();
				logonStateEnum = BookListFragment.LogonStateEnum.PUBLIC_BOOK_STORE;
				startStoreActivity();
			}
		});

		// 企业

		privateBookstoreButton = (Button) findViewById(R.id.private_bookstore_button);
		privateBookstoreButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				privateLibraryButtonOnClickListener();
			}
		});

		bookstoreGridView = (MyGridLayout) findViewById(R.id.bookstore_gridView);
		// 去除点击效果
		// bookstoreGridView.setSelector(new ColorDrawable(Color.TRANSPARENT));
		// 从缓存中获取已经下载的书籍列表
		bookShelfAdapter = new BookShelfAdapter(BookShelfActivity.this);

		bookstoreGridView.setAdapter(bookShelfAdapter);

		// 设置监听
		bookstoreGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final LocalBook book = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().get(position);
				switch (book.getBookStateEnum()) {
				case kBookStateEnum_Unpaid:// 未付费(只针对收费的书籍, 如果是免费的书籍, 会直接到下一个状态.
					break;
				case kBookStateEnum_Paiding:// 支付中....

					break;
				case kBookStateEnum_Paid: // 已付费(已付费的书籍可以直接下载了)
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
					Toast.makeText(BookShelfActivity.this, "正在解压中。。。。。。", Toast.LENGTH_SHORT).show();
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
			}

		});
		bookstoreGridView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

				final LocalBook book = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().get(position);
				switch (book.getBookStateEnum()) {
				case kBookStateEnum_Downloading:// 正在下载中...
					// 正在下载中的书籍需要先暂停
					book.stopDownloadBook();
					break;
				default:
					break;
				}
				// 删除本地书籍
				deleteLocalBookDialog(book);

				return false;
			}
		});

		bookshelf_bottom_layout = findViewById(R.id.bookshelf_bottom_layout);

		quiteLoginTextView = (TextView) findViewById(R.id.quite_login_textView);
		quiteLoginTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				showDialogQuite();
			}
		});

		mSearchLayout = (SearchLayout) findViewById(R.id.search_layout);
		// 设置各种Icon
		mSearchLayout.setIconBackground(R.drawable.ic_search_head_shelf, R.drawable.ic_search_edittext_shelf, R.drawable.selector_cancel_button_shelf);
		mSearchLayout.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				return false;
			}

			/**
			 * 当编辑框点回车键(搜索键)时回调搜索方法
			 */
			@Override
			public boolean onQueryTextSubmit(String query) {
				if (null != searchFragment && searchFragment instanceof IFragmentOptions) {
					((IFragmentOptions) searchFragment).doSearch(query, true);// 执行搜索
				}

				// 展现搜索结果的层的显示动画
				searchContentLayout.setVisibility(View.VISIBLE);
				searchContentLayout.animate().y(bookstoreGridView.getY()).setDuration(550).setListener(null);

				return true;
			}
		});
		mSearchLayout.setOnCloseListener(new OnCloseListener() {
			/**
			 * 点击取消按钮时回调
			 */
			@Override
			public boolean onClose() {
				closeSearch(searchContentLayout);

				return true;
			}
		});

		// 初始化用以搜索搜索fragment
		searchFragment = new BookSearchFragment();
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		fragmentTransaction.disallowAddToBackStack();
		fragmentTransaction.add(R.id.search_content_layout, searchFragment);
		fragmentTransaction.commit();

		// 隐藏展现搜索结果的层
		searchContentLayout = findViewById(R.id.search_content_layout);
		closeSearch(searchContentLayout);

		showLoginQuiteByGlobalDataCache();
	}

	@Override
	protected void onDestroy() {
		DebugLog.i(TAG, "onDestroy");

		super.onDestroy();
	}

	@Override
	protected void onPause() {
		DebugLog.i(TAG, "onPause");
		super.onPause();

		MobclickAgent.onPause(this);
	}

	@Override
	protected void onRestart() {
		DebugLog.i(TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		DebugLog.i(TAG, "onResume");
		showLoginQuiteByGlobalDataCache();
		GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().deleteObservers();
		bookShelfAdapter.changeDataSource(GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList());
		// 复位书院/企业按钮的状态
		publicBookstoreButton.setClickable(true);
		privateBookstoreButton.setClickable(true);
		publicBookstoreButton.setTextColor(Color.WHITE);
		privateBookstoreButton.setTextColor(Color.WHITE);
		super.onResume();

		MobclickAgent.onResume(this);
	}

	@Override
	protected void onStart() {
		DebugLog.i(TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		DebugLog.i(TAG, "onStop");
		super.onStop();
	}

	private void closeSearch(View view) {
		searchContentLayout.animate().y(getResources().getDisplayMetrics().heightPixels + 50).setListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				searchContentLayout.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
		});
	}

	private void deleteLocalBookDialog(final LocalBook book) {
		AlertDialog.Builder builder = new AlertDialog.Builder(BookShelfActivity.this);
		builder.create();
		builder.setTitle("提示");
		builder.setMessage("是否删除" + book.getBookInfo().getName());
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// 长按提示删除书籍，需要先将书籍从本地缓存中删除
				GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList().removeBook(book);
				LocalBookList localBookList = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList();
				for (int i = 0; i < localBookList.size(); i++) {
					DebugLog.e(TAG, localBookList.get(i).getBookInfo().getName());

				}
				bookShelfAdapter.notifyDataSetChanged();
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	private void loginForPrivateLibrary(final String userID, final String userPassWord) {

		LogonNetRequestBean netRequestBean = new LogonNetRequestBean.Builder(userID, userPassWord).builder();
		DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForLoginPrivateLibrary, netRequestBean, new IDomainBeanAsyncNetRespondListener() {
			@Override
			public void onFailure(NetErrorBean error) {
				// 设置企业按钮可点击
				privateBookstoreButton.setClickable(true);
				privateBookstoreButton.setTextColor(Color.WHITE);
				Toast.makeText(BookShelfActivity.this, "网络异常!", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onSuccess(Object respondDomainBean) {
				// 如果成功登录需要将返回的业务bean存储到全局缓存中，当用户切换界面时直接读取数据，判断用户是否已经登录
				LogonNetRespondBean privateAccountLogonNetRespondBean = (LogonNetRespondBean) respondDomainBean;
				privateAccountLogonNetRespondBean.setUsername(userID);
				privateAccountLogonNetRespondBean.setPassword(userPassWord);
				GlobalDataCacheForMemorySingleton.getInstance.setPrivateAccountLogonNetRespondBean(privateAccountLogonNetRespondBean);

				GlobalDataCacheForMemorySingleton.getInstance.setUsernameForLastSuccessfulLogon(userID);
				GlobalDataCacheForMemorySingleton.getInstance.setPasswordForLastSuccessfulLogon(userPassWord);
				logonStateEnum = BookListFragment.LogonStateEnum.PRIVATE_BOOK_STORE;
				startStoreActivity();
			}

		});
		// 判断联网状态，非空闲不可点击
		if (netRequestIndexForLoginPrivateLibrary.getIndex() != DomainBeanNetworkEngineSingleton.IDLE_NETWORK_REQUEST_ID) {
			// 暂时禁用 私有图书馆(企业) 按钮
			privateBookstoreButton.setClickable(false);
			privateBookstoreButton.setTextColor(getResources().getColor(R.color.book_shelf_button_click));
		}
	}

	// 企业按钮点击事件
	private void privateLibraryButtonOnClickListener() {
		// 先取消书院的网络请求
		DomainBeanNetworkEngineSingleton.getInstance.cancelNetRequestByRequestIndex(netRequestIndexForLoginPublicLibrary);
		// 设置书院按钮可以点击
		publicBookstoreButton.setClickable(true);
		publicBookstoreButton.setTextColor(Color.WHITE);

		// 从缓存中获取用户信息判断是否已经登录
		LogonNetRespondBean logonNetRespondBean = GlobalDataCacheForMemorySingleton.getInstance.getPrivateAccountLogonNetRespondBean();
		if (logonNetRespondBean == null) {
			showDialogLogin();
		} else {
			logonStateEnum = BookListFragment.LogonStateEnum.PRIVATE_BOOK_STORE;
			startStoreActivity();
			// loginForPrivateLibrary(logonNetRespondBean.getUsername(),
			// logonNetRespondBean.getPassword());
		}
	}

	private void readDocument(final File file) {
		Intent intent = new Intent();
		intent.setClass(BookShelfActivity.this, ShowBookActivity.class);
		intent.putExtra(ShowBookActivity.EXTRA_ZIP_FILE, file.getPath());
		startActivity(intent);
	}

	private void setWelcomeViewLandScape() {

	}

	private void setWelcomeViewPortrait() {

	}

	private void showDialogLogin() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.login_dialog, null);

		final EditText userNameEditText = (EditText) v.findViewById(R.id.user_name_editText);
		final EditText passwordEditText = (EditText) v.findViewById(R.id.password_editText);
		// 这里的功能还未添加，是用来保存用户的账号功能
		// final CheckBox autoLoginCheckBox = (CheckBox)
		// v.findViewById(R.id.auto_login_checkBox);

		userNameEditText.setText("appletest");
		passwordEditText.setText("appletest");

		AlertDialog.Builder builder = new AlertDialog.Builder(BookShelfActivity.this);
		builder.setView(v);
		builder.create();
		builder.setTitle("用户登录");
		builder.setCancelable(false);// 这里是屏蔽用户点击back按键
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				loginForPrivateLibrary(userNameEditText.getText().toString(), passwordEditText.getText().toString());
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	private void showDialogQuite() {

		AlertDialog.Builder builder = new AlertDialog.Builder(BookShelfActivity.this);
		builder.create();
		builder.setTitle("提示");
		builder.setMessage("是否退出当前企业账号?");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				LogonNetRespondBean privateAccountLogonNetRespondBean = null;
				GlobalDataCacheForMemorySingleton.getInstance.setPrivateAccountLogonNetRespondBean(privateAccountLogonNetRespondBean);
				showLoginQuiteByGlobalDataCache();
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	private void showDialogQuiteApp() {
		AlertDialog.Builder builder = new AlertDialog.Builder(BookShelfActivity.this);
		builder.create();
		builder.setTitle("提示");
		builder.setMessage("是否退出应用");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				// 完整退出应用
				ToolsFunctionForThisProgect.quitApp(BookShelfActivity.this);
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
	}

	private void showLoginQuiteByGlobalDataCache() {
		// 从缓存中获取用户信息判断是否已经登录
		LogonNetRespondBean logonNetRespondBean = GlobalDataCacheForMemorySingleton.getInstance.getPrivateAccountLogonNetRespondBean();
		if (logonNetRespondBean == null) {
			bookshelf_bottom_layout.setVisibility(View.GONE);
		} else {
			bookshelf_bottom_layout.setVisibility(View.VISIBLE);
		}

	}

	private void startStoreActivity() {
		Bundle bundle = new Bundle();
		bundle.putString("LogonState", logonStateEnum.getState());

		Intent intent = new Intent();
		intent.putExtras(bundle);
		intent.setClass(BookShelfActivity.this, BookStoreActivity.class);
		startActivityForResult(intent, 1);
		// startActivity(intent);

	}
}
