package cn.retech.global_data_cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.text.TextUtils;
import cn.retech.domainbean_model.local_book_list.LocalBook;
import cn.retech.domainbean_model.local_book_list.LocalBookForSerializable;
import cn.retech.domainbean_model.local_book_list.LocalBookList;
import cn.retech.domainbean_model.local_book_list.LocalBookListForSerializable;
import cn.retech.domainbean_model.login.LogonNetRespondBean;
import cn.retech.toolutils.DebugLog;

/**
 * 这里序列化对象的保存目录是 : /data/data/cn.skyduck.activity/files/ , 这个目录会在用户在 "应用程序管理"
 * 中点击 "清理数据" 按钮后被清理
 * 
 * @author computer
 * 
 */
public final class GlobalDataCacheForNeedSaveToFileSystem {
	private final static String TAG = GlobalDataCacheForNeedSaveToFileSystem.class.getSimpleName();

	private GlobalDataCacheForNeedSaveToFileSystem() {

	}

	private enum CacheDataNameForSaveToFile {
		// 自动登录的标志
		AutoLoginMark,
		// 用户最后一次成功登录时得到的响应业务Bean
		PrivateAccountLogonNetRespondBean,
		// 用户是否是首次启动App
		FirstStartApp,
		// 是否需要显示 初学者指南
		BeginnerGuide,
		// 本地书籍列表
		LocalBookList,
		// 当前app版本号, 用了防止升级app时, 本地缓存的序列化数据恢复出错.
		LocalAppVersion
	};

	private static void serializeObjectToFileSystemWithObject(Object object, String fileName, String directoryPath) {
		if (object == null) {
			// 如果入参为空, 就证明要删除本地缓存的该对象的序列化文件
			File file = new File(directoryPath + "/" + fileName);
			if (!file.delete()) {
				DebugLog.e(TAG, "删除序列化到本地的对象文件失败!");
			}
		} else {
			serializeObjectToFile(fileName, directoryPath, object);
		}
	}

	/**
	 * 读取本地缓存的 用户登录信息
	 */
	public static void readUserLoginInfoToGlobalDataCacheForMemorySingleton() {

		// 自动登录的标志

		// 私有用户登录成功后, 服务器返回的信息(判断此对象是否为空, 来确定当前是否有企业账户处于登录状态)
		DebugLog.i(TAG, "start loading --> privateAccountLogonNetRespondBean");
		final LogonNetRespondBean privateAccountLogonNetRespondBean = (LogonNetRespondBean) deserializeObjectFromFile(CacheDataNameForSaveToFile.PrivateAccountLogonNetRespondBean.name());
		GlobalDataCacheForMemorySingleton.getInstance.setPrivateAccountLogonNetRespondBean(privateAccountLogonNetRespondBean);
	}

	/**
	 * 读取本地缓存的 书籍列表
	 */
	public static void readLocalBookListToGlobalDataCacheForMemorySingleton() {
		LocalBookList localBookList = new LocalBookList();
		final LocalBookListForSerializable localBookListForSerializable = (LocalBookListForSerializable) deserializeObjectFromFile(CacheDataNameForSaveToFile.LocalBookList.name());
		do {
			if (localBookListForSerializable == null) {
				DebugLog.i(TAG, "无本地序列化书籍列表！");
				break;
			}
			for (LocalBookForSerializable bookForSerializable : localBookListForSerializable.getLocalBookList()) {
				LocalBook book = new LocalBook(bookForSerializable.getBookInfo());
				book.setBookStateEnum(bookForSerializable.getBookStateEnum());
				book.setBindAccount(bookForSerializable.getBindAccount());
				localBookList.addBook(book);
			}
		} while (false);

		GlobalDataCacheForMemorySingleton.getInstance.setLocalBookList(localBookList);
	}

	public static void readAllCacheData() {
		// 读取本地缓存的 "用户登录信息"
		readUserLoginInfoToGlobalDataCacheForMemorySingleton();
		// 读取本地缓存的书籍列表
		readLocalBookListToGlobalDataCacheForMemorySingleton();
	}

	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////

	/**
	 * 保存用户登录信息到设备文件系统中
	 */
	public static void writeUserLoginInfoToFileSystem() {

		// 自动登录的标志

		//
		final LogonNetRespondBean privateAccountLogonNetRespondBean = GlobalDataCacheForMemorySingleton.getInstance.getPrivateAccountLogonNetRespondBean();
		serializeObjectToFileSystemWithObject(privateAccountLogonNetRespondBean, CacheDataNameForSaveToFile.PrivateAccountLogonNetRespondBean.name(), LocalCacheDataPathConstant.importantDataCachePath());
	}

	/**
	 * 保存书籍列表信息到设备文件系统中
	 */
	public static void writeLocalBookListToFileSystem() {

		final LocalBookList localBookList = GlobalDataCacheForMemorySingleton.getInstance.getLocalBookList();
		LocalBookListForSerializable localBookListForSerializable = new LocalBookListForSerializable();
		for (int i = 0; i < localBookList.size(); i++) {
			LocalBook book = localBookList.get(i);
			LocalBookForSerializable localBookForSerializable = new LocalBookForSerializable();
			localBookForSerializable.setBindAccount(book.getBindAccount());
			localBookForSerializable.setBookInfo(book.getBookInfo());
			LocalBook.BookStateEnum bookStateEnum = book.getBookStateEnum();
			if (bookStateEnum == LocalBook.BookStateEnum.kBookStateEnum_Downloading) {
				bookStateEnum = LocalBook.BookStateEnum.kBookStateEnum_Pause;
			} else if (bookStateEnum == LocalBook.BookStateEnum.kBookStateEnum_Unziping) {
				bookStateEnum = LocalBook.BookStateEnum.kBookStateEnum_NotInstalled;
			}
			localBookForSerializable.setBookStateEnum(bookStateEnum);
			localBookListForSerializable.getLocalBookList().add(localBookForSerializable);
		}
		serializeObjectToFileSystemWithObject(localBookListForSerializable, CacheDataNameForSaveToFile.LocalBookList.name(), LocalCacheDataPathConstant.importantDataCachePath());
	}

	public static void writeAllCacheData() {
		// 保存 "用户登录信息"
		writeUserLoginInfoToFileSystem();
		// 保存书籍列表
		writeLocalBookListToFileSystem();
	}

	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////////

	/**
	 * 将一个对象, 序列化到文件中
	 * 
	 * @param fileName
	 * @param directoryPath
	 * @param object
	 */
	private static void serializeObjectToFile(final String fileName, final String directoryPath, final Object object) {
		File file = null;
		FileOutputStream fileOutputStream = null;
		ObjectOutputStream objectOutputStream = null;
		try {

			do {
				if (object == null) {
					break;
				}
				if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(directoryPath)) {
					break;
				}

				file = new File(directoryPath + "/" + fileName);
				if (file.exists()) {
					file.delete();
				}
				file = new File(directoryPath + "/" + fileName);
				fileOutputStream = new FileOutputStream(file);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(object);

			} while (false);

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				if (objectOutputStream != null) {
					objectOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * 从一个文件中, 反序列化一个对象
	 * 
	 * 文件保存目录示例 : /data/data/cn.skyduck.activity/files 这个目录会在用户在应用程序管理中点击 "清理数据"
	 * 按钮后被清理
	 * 
	 * @param fileName
	 * @param directoryPath
	 * @return
	 */
	private static Object deserializeObjectFromFile(final String fileName, final String directoryPath) {
		Object object = null;
		File file = null;
		FileInputStream fileInputStream = null;
		ObjectInputStream objectInputStream = null;

		try {
			do {
				if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(directoryPath)) {
					break;
				}

				file = new File(directoryPath + "/" + fileName);
				if (!file.exists()) {
					break;
				}
				fileInputStream = new FileInputStream(file);
				objectInputStream = new ObjectInputStream(fileInputStream);
				object = objectInputStream.readObject();
			} while (false);
		} catch (Exception ex) {
			object = null;
			ex.printStackTrace();
		} finally {
			try {
				if (objectInputStream != null) {
					objectInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return object;
	}

	private static Object deserializeObjectFromFile(final String fileName) {
		return deserializeObjectFromFile(fileName, LocalCacheDataPathConstant.importantDataCachePath());
	}

}
