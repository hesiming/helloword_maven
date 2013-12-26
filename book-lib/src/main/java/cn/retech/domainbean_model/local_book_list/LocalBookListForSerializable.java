package cn.retech.domainbean_model.local_book_list;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LocalBookListForSerializable implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1493155650591388953L;

	private List<LocalBookForSerializable> localBookList = new ArrayList<LocalBookForSerializable>();

	public List<LocalBookForSerializable> getLocalBookList() {
		return localBookList;
	}

	@Override
	public String toString() {
		return "LocalBookListForSerializable [localBookList=" + localBookList + "]";
	}

}
