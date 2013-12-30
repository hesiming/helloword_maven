package main.java.cn.retech.activity;

import java.util.ArrayList;
import java.util.List;

import main.java.cn.retech.adapter.MyFragmentPagerAdapter;
import main.java.cn.retech.custom_control.ControlOnActionEnum;
import main.java.cn.retech.custom_control.ICustomControlDelegate;
import main.java.cn.retech.custom_control.MyViewPaper;
import main.java.cn.retech.custom_control.MyViewPaper.OnScrollListener;
import main.java.cn.retech.custom_control.PageTitle;
import main.java.cn.retech.custom_control.TabNavigation;
import main.java.cn.retech.custom_control.TabNavigation.OnScrollFullListener;
import main.java.cn.retech.custom_control.TabNavigation.OnTabChangeListener;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import cn.retech.activity.R;
import cn.retech.domainbean_model.book_categories.BookCategoriesNetRequestBean;
import cn.retech.domainbean_model.book_categories.BookCategoriesNetRespondBean;
import cn.retech.domainbean_model.book_categories.BookCategory;
import cn.retech.domainbean_model.login.LogonNetRequestBean;
import cn.retech.domainbean_model.login.LogonNetRespondBean;
import cn.retech.global_data_cache.GlobalConstant;
import cn.retech.global_data_cache.GlobalDataCacheForMemorySingleton;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton.NetRequestIndex;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.IDomainBeanAsyncNetRespondListener;
import cn.retech.my_domainbean_engine.net_error_handle.NetErrorBean;
import cn.retech.toolutils.DebugLog;

import com.umeng.analytics.MobclickAgent;

public class BookStoreActivity extends FragmentActivity implements ICustomControlDelegate {
	private final String TAG = this.getClass().getSimpleName();
	private NetRequestIndex netRequestIndexForLoginPublicLibrary = new NetRequestIndex();
	private NetRequestIndex netRequestIndexForLoginPrivateLibrary = new NetRequestIndex();
	private boolean isSearching = false;
	private FragmentManager mFragmentManager = getFragmentManager();
	private final static String SEARCH_FRAGMENT_TAG = "SearchBook";

	private NetRequestIndex netRequestIndexForBookCategories = new NetRequestIndex();

	private PageTitle pageTitle;
	private ImageView shadowSide;
	private TabNavigation mTabNavigation;
	private List<BookCategory> mbookCategories = new ArrayList<BookCategory>();
	private MyFragmentPagerAdapter mAdapter;
	private MyViewPaper mViewPager;
	int viewPageCurrentIndex;

	private RelativeLayout mbookSearchLayout;
	// 记录用户登录到书城/企业
	private BookListFragment.LogonStateEnum logonStateEnum;

	@Override
	public void customControlOnAction(Object contorl, Object actionTypeEnum) {

		if (actionTypeEnum instanceof ControlOnActionEnum) {
			switch ((ControlOnActionEnum) actionTypeEnum) {
			case BACK_TO_MYBOOKSHLF:
				this.finish();

				break;
			case REFRESH:
				Fragment currentFragment = null;

				if (mAdapter != null) {
					currentFragment = mAdapter.getItem(mViewPager.getCurrentItem());
				}

				do {
					if (currentFragment == null) {
						break;
					}
					if (!(currentFragment instanceof IFragmentOptions)) {
						break;
					}
					((IFragmentOptions) currentFragment).refresh();

				} while (false);

				break;
			case SHOW_SEARCH:
				android.app.Fragment searchFragment = mFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG);

				if (null == searchFragment) {
					mbookSearchLayout.setAlpha((float) 1.0);

					Bundle bundle = new Bundle();
					bundle.putString("bookName", (String) contorl);
					bundle.putString("LogonState", BookListFragment.LogonStateEnum.PUBLIC_BOOK_STORE.getState());
					searchFragment = android.app.Fragment.instantiate(this, BookSearchFragment.class.getName(), bundle);

					FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
					fragmentTransaction.add(R.id.book_search_layout, searchFragment, SEARCH_FRAGMENT_TAG);
					fragmentTransaction.commit();

					searchFragment = mFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG);
				}

				if (searchFragment instanceof IFragmentOptions) {
					((IFragmentOptions) searchFragment).doSearch((String) contorl, false);
				}

				break;
			case OPEN_SEARCH:
				isSearching = true;

				if (null != mFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)) {
					FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
					fragmentTransaction.remove(mFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG));
					fragmentTransaction.commit();
				}

				mbookSearchLayout.animate().y(0).setDuration(500);

				break;
			case CLOSE_SEARCH:
				isSearching = false;

				mbookSearchLayout.animate().setDuration(450).y(-mbookSearchLayout.getHeight()).setListener(new AnimatorListener() {
					@Override
					public void onAnimationCancel(Animator animation) {
					}

					@Override
					public void onAnimationEnd(Animator animation) {
						if (null != mFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)) {
							FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
							fragmentTransaction.remove(mFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG));
							fragmentTransaction.commit();
						}

						mbookSearchLayout.setAlpha((float) 0.6);
						mbookSearchLayout.animate().setListener(null);
					}

					@Override
					public void onAnimationRepeat(Animator animation) {
					}

					@Override
					public void onAnimationStart(Animator animation) {
					}
				});

				break;
			default:
				break;
			}
		}

	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (null != mTabNavigation) {
			int eachPageButtonNum = Integer.parseInt(getResources().getString(R.string.tabnavigation_button_number));
			if (mbookCategories.size() != 0 && mbookCategories.size() < eachPageButtonNum) {
				eachPageButtonNum = mbookCategories.size();

				shadowSide.setVisibility(View.GONE);
			}
			mTabNavigation.showCategory(mbookCategories, eachPageButtonNum);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean returnValue = true;

		if (keyCode == KeyEvent.KEYCODE_BACK && isSearching) {
			pageTitle.closeSearchView();
		} else {
			returnValue = super.onKeyDown(keyCode, event);
		}

		return returnValue;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.book_store_layout);

		ActionBar actionBar = getActionBar();
		pageTitle = new PageTitle(this);
		pageTitle.setiCustomControlDelegate(this);

		actionBar.setCustomView(pageTitle);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		if (bundle.get("LogonState").equals(BookListFragment.LogonStateEnum.PRIVATE_BOOK_STORE.getState())) {
			logonStateEnum = BookListFragment.LogonStateEnum.PRIVATE_BOOK_STORE;
			pageTitle.setTitle(getResources().getString(R.string.private_bookstore_title));

			LogonNetRespondBean logonNetRespondBean = GlobalDataCacheForMemorySingleton.getInstance.getPrivateAccountLogonNetRespondBean();
			if (logonNetRespondBean != null) {
				loginForPrivateLibrary(logonNetRespondBean.getUsername(), logonNetRespondBean.getPassword());
			}

		} else if (bundle.get("LogonState").equals(BookListFragment.LogonStateEnum.PUBLIC_BOOK_STORE.getState())) {
			logonStateEnum = BookListFragment.LogonStateEnum.PUBLIC_BOOK_STORE;
			pageTitle.setTitle(getResources().getString(R.string.public_bookstore_title));
			publicLibraryButtonOnClickListener();
		}

		mbookSearchLayout = (RelativeLayout) findViewById(R.id.book_search_layout);
		mbookSearchLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pageTitle.closeSearchView();
			}
		});
		mbookSearchLayout.setY(-2000);

		shadowSide = (ImageView) findViewById(R.id.shadow_side_imageView);
		mTabNavigation = (TabNavigation) findViewById(R.id.tabNavigation);
		mTabNavigation.setOnTabChangeListener(new OnTabChangeListener() {
			@Override
			public void onTabChange(int postion) {
				mViewPager.setCurrentItem(postion);
			}
		});
		mTabNavigation.setOnScrollFullLeftListener(new OnScrollFullListener() {
			@Override
			public void onScrollFullLeft() {
				shadowSide.setVisibility(View.VISIBLE);

				shadowSide.setImageDrawable(getResources().getDrawable(R.drawable.shadow_side_full_left));
			}

			@Override
			public void onScrollFullRight() {
				shadowSide.setVisibility(View.VISIBLE);
				shadowSide.setImageDrawable(getResources().getDrawable(R.drawable.shadow_side_full_right));
			}

			@Override
			public void onScrolling() {
				shadowSide.setVisibility(View.VISIBLE);
				shadowSide.setImageDrawable(getResources().getDrawable(R.drawable.shadow_side));
			}
		});

		mViewPager = (MyViewPaper) findViewById(R.id.book_list_viewPager);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageSelected(int arg0) {
				mTabNavigation.setCurrentItem(arg0);
				viewPageCurrentIndex = arg0;
			}
		});
		mViewPager.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScroll(int l, int oldl, int width) {
				mTabNavigation.scrollKit(l, oldl, width);
			}
		});
		// testBookCategories();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();

		MobclickAgent.onPause(this);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();

		MobclickAgent.onResume(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void loginForPrivateLibrary(final String userID, final String userPassWord) {

		LogonNetRequestBean netRequestBean = new LogonNetRequestBean.Builder(userID, userPassWord).builder();
		DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForLoginPrivateLibrary, netRequestBean, new IDomainBeanAsyncNetRespondListener() {
			@Override
			public void onFailure(NetErrorBean error) {
				showErrorDialog(error);
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
				testBookCategories();
			}

		});
	}

	// /////////////////////////书院登录逻辑////////////////////////////////////
	// 书院按钮点击事件
	private void publicLibraryButtonOnClickListener() {
		// 需要先取消企业的网络请求
		DomainBeanNetworkEngineSingleton.getInstance.cancelNetRequestByRequestIndex(netRequestIndexForLoginPrivateLibrary);
		// 登录书院
		LogonNetRequestBean netRequestBean = new LogonNetRequestBean.Builder(GlobalConstant.publicUserName, GlobalConstant.publicUserPassword).builder();
		DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForLoginPublicLibrary, netRequestBean, new IDomainBeanAsyncNetRespondListener() {
			@Override
			public void onFailure(NetErrorBean error) {
				Toast.makeText(BookStoreActivity.this, "网络异常!", Toast.LENGTH_SHORT).show();
			}

			@Override
			public void onSuccess(Object respondDomainBean) {
				GlobalDataCacheForMemorySingleton.getInstance.setUsernameForLastSuccessfulLogon(GlobalConstant.publicUserName);
				GlobalDataCacheForMemorySingleton.getInstance.setPasswordForLastSuccessfulLogon(GlobalConstant.publicUserPassword);
				logonStateEnum = BookListFragment.LogonStateEnum.PUBLIC_BOOK_STORE;
				testBookCategories();
			}

		});
	}

	// /////////////////////////企业登录逻辑////////////////////////////////////

	private void showErrorDialog(NetErrorBean error) {
		AlertDialog.Builder builder = new AlertDialog.Builder(BookStoreActivity.this);
		builder.create();
		builder.setTitle("提示");
		builder.setMessage("网络未连接！");
		builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.setClass(BookStoreActivity.this, BookShelfActivity.class);
				BookStoreActivity.this.setResult(1);
				BookStoreActivity.this.finish();
			}
		});
		builder.show().setOnDismissListener(new OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface dialog) {
				Intent intent = new Intent();
				intent.setClass(BookStoreActivity.this, BookShelfActivity.class);
				BookStoreActivity.this.setResult(1);
				BookStoreActivity.this.finish();
			}
		});
	}

	private void testBookCategories() {
		BookCategoriesNetRequestBean netRequestBean = new BookCategoriesNetRequestBean();
		DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForBookCategories, netRequestBean, new IDomainBeanAsyncNetRespondListener() {

			@Override
			public void onFailure(NetErrorBean error) {
				DebugLog.e(TAG, "testBookCategories = " + error.getErrorMessage());
			}

			@Override
			public void onSuccess(Object respondDomainBean) {
				BookCategoriesNetRespondBean respondBean = (BookCategoriesNetRespondBean) respondDomainBean;
				// 将书本为0的分类剔除
				for (BookCategory bookCategory : respondBean.getCategories()) {
					if (bookCategory.getBookcount() > 0) {
						mbookCategories.add(bookCategory);
					}
				}

				int eachPageButtonNum = Integer.parseInt(getResources().getString(R.string.tabnavigation_button_number));
				if (mbookCategories.size() != 0 && mbookCategories.size() < eachPageButtonNum) {
					eachPageButtonNum = mbookCategories.size();

					shadowSide.setVisibility(View.GONE);
				}
				mTabNavigation.showCategory(mbookCategories, eachPageButtonNum);

				List<Fragment> fragments = new ArrayList<Fragment>();
				for (BookCategory bookCategory : mbookCategories) {
					Bundle bundle = new Bundle();
					bundle.putString("identifier", bookCategory.getIdentifier());
					bundle.putString("LogonState", logonStateEnum.getState());
					fragments.add(Fragment.instantiate(BookStoreActivity.this, BookListFragment.class.getName(), bundle));
				}
				mAdapter = new MyFragmentPagerAdapter(BookStoreActivity.this.getSupportFragmentManager(), fragments);
				mViewPager.setAdapter(mAdapter);
				mViewPager.setCurrentItem(0);
			}
		});
	}
}
