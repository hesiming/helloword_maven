package cn.retech.domainbean_model.local_book_list;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;
import cn.retech.global_data_cache.LocalCacheDataPathConstant;
import cn.retech.toolutils.DebugLog;

//本地书籍列表(本地书籍包括, 已经下载完全, 并且已经解压开, 可以正常阅读的书籍; 也包括那些未下载完全的书籍(可以进行断电续传)).
public final class LocalBookList {
	private final String TAG = this.getClass().getSimpleName();
	// 外部可以使用KVO来监听localBookList, 当localBookList属性 增加/删除 一本书时, 都会触发KVO
	private List<LocalBook> localBookList = new ArrayList<LocalBook>();

	public int size() {
		return localBookList.size();
	}

	public LocalBook get(int location) {
		if (location < 0 || location >= size()) {
			return null;
		}

		return localBookList.get(location);
	}

	// 对外的接口方法 (操作列表)
	public LocalBook bookByContentID(String contentIDString) {
		if (TextUtils.isEmpty(contentIDString)) {
			assert false : "入参错误 contentIDString !";
			return null;
		}

		LocalBook result = null;
		for (LocalBook localBook : localBookList) {
			if (contentIDString.equals(localBook.getBookInfo().getContent_id())) {
				result = localBook;
				break;
			}
		}

		return result;
	}

	public boolean addBook(LocalBook newBook) {
		do {
			if (newBook == null) {
				assert false : "入参 newBook 非法.";
				break;
			}

			if (localBookList.contains(newBook)) {
				DebugLog.i(TAG, "当前书籍已经存在本地了, bookname=" + newBook.getBookInfo().getName());
				break;
			}

			localBookList.add(newBook);
			return true;
		} while (false);
		return false;

	}

	public void removeBook(LocalBook book) {
		if (book == null) {
			assert false : "入参 book 非法.";
			return;
		}

		removeBookByContentID(book.getBookInfo().getContent_id());
	}

	public boolean removeBookAtIndex(int index) {
		if (index >= localBookList.size() || index < 0) {
			assert false : "入参 index 数组越界.";
			return false;
		}

		LocalBook book = localBookList.get(index);
		removeBookByContentID(book.getBookInfo().getContent_id());
		return true;
	}

	/**
	 * 根据书籍ID, 删除本地书籍列表中的书籍对象(所有删除书籍对象的方法, 最终都要调用这个方法, 因为只有这个方法, 才会将书籍从文件系统中删除)
	 * 
	 * @param contentIDString
	 * 
	 * @return
	 */
	public boolean removeBookByContentID(String contentIDString) {
		if (TextUtils.isEmpty(contentIDString)) {
			assert false : "入参 contentIDString 非法.";
			return false;
		}

		for (LocalBook book : localBookList) {
			if (contentIDString.equals(book.getBookInfo().getContent_id())) {

				// 删除文件系统中保存的书籍
				deleteBookFromFileSystemWithContentID(book.getBookInfo().getContent_id());
				// 删除内存中保存的书籍
				localBookList.remove(book);
				return true;
			}
		}

		return false;
	}

	public int indexOfBookByContentID(String contentIDString) {
		do {
			if (TextUtils.isEmpty(contentIDString)) {
				assert false : "入参 contentIDString 非法.";
				break;
			}

			for (int i = 0; i < localBookList.size(); i++) {
				LocalBook book = localBookList.get(i);
				if (contentIDString.equals(book.getBookInfo().getContent_id())) {
					return i;
				}
			}
		} while (false);

		return -1;
	}

	// ////

	private void deleteBookFromFileSystemWithContentID(String contentIDString) {
		do {
			String filePath = LocalCacheDataPathConstant.localBookCachePath() + "/" + contentIDString;
			File tempFile = new File(filePath);
			if (!tempFile.exists()) {
				DebugLog.e(TAG, "压缩包不存在！");
				break;
			}
			if (!tempFile.exists()) {
				System.out.println("删除文件失败：" + filePath + "文件不存在");
				break;
			} else {
				if (tempFile.isFile()) {

					deleteFile(filePath);
				} else {
					deleteDirectory(filePath);
				}
			}

		} while (false);

	}

	/**
	 * 删除单个文件
	 * 
	 * @param fileName
	 *          被删除文件的文件名
	 * @return 单个文件删除成功返回true,否则返回false
	 */
	public boolean deleteFile(String fileName) {
		File file = new File(fileName);
		if (file.isFile() && file.exists()) {
			file.delete();
			return true;
		} else {
			DebugLog.e(TAG, "删除单个文件" + fileName + "失败！");
			return false;
		}
	}

	/**
	 * 删除目录（文件夹）以及目录下的文件
	 * 
	 * @param dir
	 *          被删除目录的文件路径
	 * @return 目录删除成功返回true,否则返回false
	 */
	public boolean deleteDirectory(String dir) {
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		if (!dir.endsWith(File.separator)) {
			dir = dir + File.separator;
		}
		File dirFile = new File(dir);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			DebugLog.e(TAG, "删除目录失败" + dir + "目录不存在！");
			return false;
		}
		boolean flag = true;
		// 删除文件夹下的所有文件(包括子目录)
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				flag = deleteFile(files[i].getAbsolutePath());
				if (!flag) {
					break;
				}
			}
			// 删除子目录
			else {
				flag = deleteDirectory(files[i].getAbsolutePath());
				if (!flag) {
					break;
				}
			}
		}

		if (!flag) {
			DebugLog.e(TAG, "删除目录失败");
			return false;
		}

		// 删除当前目录
		if (dirFile.delete()) {
			return true;
		} else {
			DebugLog.e(TAG, "删除目录" + dir + "失败！");
			return false;
		}
	}

	public void deleteObservers() {
		for (LocalBook book : localBookList) {
			book.deleteObservers();
		}
	}
}
