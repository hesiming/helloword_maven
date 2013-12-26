package cn.retech.domainbean_model.local_book_list;

import java.io.Serializable;

import cn.retech.domainbean_model.booklist_in_bookstores.BookInfo;
import cn.retech.domainbean_model.local_book_list.LocalBook.BookStateEnum;
import cn.retech.domainbean_model.login.LogonNetRespondBean;

public class LocalBookForSerializable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7076347749773007818L;

	private BookInfo bookInfo;
	private BookStateEnum bookStateEnum;
	private LogonNetRespondBean bindAccount;

	public BookInfo getBookInfo() {
		return bookInfo;
	}

	public void setBookInfo(BookInfo bookInfo) {
		this.bookInfo = bookInfo;
	}

	public BookStateEnum getBookStateEnum() {
		return bookStateEnum;
	}

	public void setBookStateEnum(BookStateEnum bookStateEnum) {
		this.bookStateEnum = bookStateEnum;
	}

	public LogonNetRespondBean getBindAccount() {
		return bindAccount;
	}

	public void setBindAccount(LogonNetRespondBean bindAccount) {
		this.bindAccount = bindAccount;
	}

	@Override
	public String toString() {
		return "LocalBookForSerializable [bookInfo=" + bookInfo + ", bookStateEnum=" + bookStateEnum + ", bindAccount=" + bindAccount + "]";
	}

}
