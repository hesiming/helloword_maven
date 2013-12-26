package cn.retech.global_data_cache;

import org.apache.http.client.CookieStore;

import android.os.Handler;
import cn.retech.domainbean_model.local_book_list.LocalBookList;
import cn.retech.domainbean_model.login.LogonNetRespondBean;

/**
 * 需要全局缓存的数据
 * 
 */
public enum GlobalDataCacheForMemorySingleton {
	getInstance;

	static {

	}

	private Handler handler;

	public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	// 是否是第一次启动App
	private boolean isFirstStartApp;

	public boolean isFirstStartApp() {
		return isFirstStartApp;
	}

	public void setFirstStartApp(boolean isFirstStartApp) {
		this.isFirstStartApp = isFirstStartApp;
	}

	// 是否需要在app启动时, 显示 "初学者指南界面"
	private boolean isNeedShowBeginnerGuide;

	public boolean isNeedShowBeginnerGuide() {
		return isNeedShowBeginnerGuide;
	}

	public void setNeedShowBeginnerGuide(boolean isNeedShowBeginnerGuide) {
		this.isNeedShowBeginnerGuide = isNeedShowBeginnerGuide;
	}

	// 是否需要自动登录的标志
	private boolean isNeedAutologin;

	public boolean isNeedAutologin() {
		return isNeedAutologin;
	}

	public void setNeedAutologin(boolean isNeedAutologin) {
		this.isNeedAutologin = isNeedAutologin;
	}

	// 发起登录请求后, 要缓存服务器返回的 cookie, session id 在cookie中
	private CookieStore cookieStore;

	public CookieStore getCookieStore() {
		return cookieStore;
	}

	public void setCookieStore(CookieStore cookieStore) {
		this.cookieStore = cookieStore;
	}

	// 私有用户登录成功后, 服务器返回的信息(判断此对象是否为空, 来确定当前是否有企业账户处于登录状态)
	private LogonNetRespondBean privateAccountLogonNetRespondBean;

	public LogonNetRespondBean getPrivateAccountLogonNetRespondBean() {
		return privateAccountLogonNetRespondBean;
	}

	public void setPrivateAccountLogonNetRespondBean(LogonNetRespondBean privateAccountLogonNetRespondBean) {
		this.privateAccountLogonNetRespondBean = privateAccountLogonNetRespondBean;
	}

	// 用户最后一次登录成功时的用户名/密码(企业账户/公共账户 登录成功都会保存在这里)
	private String usernameForLastSuccessfulLogon;

	public String getUsernameForLastSuccessfulLogon() {
		return usernameForLastSuccessfulLogon;
	}

	public void setUsernameForLastSuccessfulLogon(String usernameForLastSuccessfulLogon) {
		this.usernameForLastSuccessfulLogon = usernameForLastSuccessfulLogon;
	}

	private String passwordForLastSuccessfulLogon;

	public String getPasswordForLastSuccessfulLogon() {
		return passwordForLastSuccessfulLogon;
	}

	public void setPasswordForLastSuccessfulLogon(String passwordForLastSuccessfulLogon) {
		this.passwordForLastSuccessfulLogon = passwordForLastSuccessfulLogon;
	}

	// 本地缓存的数据的大小(字节)
	private int localCacheDataSize;

	public int getLocalCacheDataSize() {
		return localCacheDataSize;
	}

	public void setLocalCacheDataSize(int localCacheDataSize) {
		this.localCacheDataSize = localCacheDataSize;
	}

	// 本地书籍列表
	private LocalBookList localBookList;

	public LocalBookList getLocalBookList() {
		return localBookList;
	}

	public void setLocalBookList(LocalBookList localBookList) {
		this.localBookList = localBookList;
	}
}
