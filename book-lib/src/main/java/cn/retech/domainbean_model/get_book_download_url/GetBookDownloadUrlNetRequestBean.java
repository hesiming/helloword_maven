package cn.retech.domainbean_model.get_book_download_url;

import cn.retech.domainbean_model.login.LogonNetRespondBean;

public final class GetBookDownloadUrlNetRequestBean {
	// 书籍ID
	private final String contentId;
	// 跟要下载的书籍绑定的账号, 这里是服务器端做的安全策略, 要检测跟目标书籍绑定的账号是否有下载权限.
	private final LogonNetRespondBean bindAccount;

	public GetBookDownloadUrlNetRequestBean(String contentId, LogonNetRespondBean bindAccount) {
		this.contentId = contentId;
		this.bindAccount = bindAccount;
	}

	public String getContentId() {
		return contentId;
	}

	public LogonNetRespondBean getBindAccount() {
		return bindAccount;
	}

	@Override
	public String toString() {
		return "GetBookDownloadUrlNetRequestBean [contentId=" + contentId + ", bindAccount=" + bindAccount + "]";
	}

}
