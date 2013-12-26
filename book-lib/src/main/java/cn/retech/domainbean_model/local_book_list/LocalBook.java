package cn.retech.domainbean_model.local_book_list;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Observable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import cn.retech.domainbean_model.booklist_in_bookstores.BookInfo;
import cn.retech.domainbean_model.get_book_download_url.GetBookDownloadUrlNetRequestBean;
import cn.retech.domainbean_model.get_book_download_url.GetBookDownloadUrlNetRespondBean;
import cn.retech.domainbean_model.login.LogonNetRespondBean;
import cn.retech.global_data_cache.LocalCacheDataPathConstant;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.DomainBeanNetworkEngineSingleton.NetRequestIndex;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.IDomainBeanAsyncNetRespondListener;
import cn.retech.my_domainbean_engine.domainbean_network_engine_singleton.IFileAsyncHttpResponseHandler;
import cn.retech.my_domainbean_engine.net_error_handle.NetErrorBean;
import cn.retech.toolutils.DebugLog;
import cn.retech.toolutils.SimpleSDCardTools;

public final class LocalBook extends Observable {

	private final String TAG = this.getClass().getSimpleName();

	private Handler handler = new Handler(Looper.getMainLooper());
	private NetRequestIndex netRequestIndexForDownloadBookFile = new NetRequestIndex();
	private NetRequestIndex netRequestIndexForGetBookDownloadUrl = new NetRequestIndex();

	public static enum ObserverEnum {
		// 书籍下载进度
		kBookDownloadProgress,
		// 书籍状态
		kBookState
	}

	// 书籍状态枚举
	public static enum BookStateEnum {

		// 收费
		kBookStateEnum_Unpaid,
		// 支付中....
		kBookStateEnum_Paiding,
		// 已支付
		kBookStateEnum_Paid,
		// 免费
		kBookStateEnum_Free,
		// 有可以更新的内容
		kBookStateEnum_Update,

		// 正在获取用于书籍下载的URL中...
		kBookStateEnum_GetBookDownloadUrl,
		// 正在下载中...
		kBookStateEnum_Downloading,
		// 暂停(也就是未下载完成, 可以进行断电续传)
		kBookStateEnum_Pause,
		// 未安装(已经下载完成, 还未完成安装)
		kBookStateEnum_NotInstalled,
		// 解压书籍zip资源包中....
		kBookStateEnum_Unziping,
		// 已安装(已经解压开的书籍, 可以正常阅读了)
		kBookStateEnum_Installed

	};

	public LocalBook(BookInfo bookInfo) {
		// 进行 "数据保护"
		this.bookInfo = bookInfo.clone();
		double price = 0.0;
		try {
			price = Double.valueOf(bookInfo.getPrice());
		} catch (NumberFormatException e) {
			price = 0.0;
		}
		if (price > 0) {
			bookStateEnum = BookStateEnum.kBookStateEnum_Unpaid;
		} else {
			bookStateEnum = BookStateEnum.kBookStateEnum_Free;
		}

	}

	// 书籍信息(从服务器获取的, 这个属性在初始化 LocalBook 时被赋值, 之后就是只读数据了)
	private BookInfo bookInfo;

	public BookInfo getBookInfo() {
		return bookInfo;
	}

	public void setBookInfo(BookInfo bookInfo) {
		this.bookInfo = bookInfo;
	}

	// 解压进度, 100% 数值是 1, 外部可以这样计算完成百分比 downloadProgress * 100
	private int decompressProgress;

	private void setDecompressProgress(int decompressProgress) {
		this.decompressProgress = decompressProgress;
		handler.post(new Runnable() {
			@Override
			public void run() {
				setChanged();
				notifyObservers(ObserverEnum.kBookState);
			}
		});

	}

	public int getDecompressProgress() {
		return decompressProgress;
	}

	// 下载进度, 100% 数值是 1, 外部可以这样计算完成百分比 downloadProgress * 100
	private float downloadProgress;

	private void setDownloadProgress(float downloadProgress) {
		this.downloadProgress = downloadProgress;
		handler.post(new Runnable() {

			@Override
			public void run() {
				setChanged();
				notifyObservers(ObserverEnum.kBookDownloadProgress);
				clearChanged();
			}
		});

	}

	public float getDownloadProgress() {
		return downloadProgress;
	}

	// 书籍状态
	private BookStateEnum bookStateEnum;

	// 书籍下载解压过程中, 如果发生错误时, 通知控制层的块
	// private BookDownloadErrorBlock bookDownloadErrorBlock;

	public BookStateEnum getBookStateEnum() {
		return bookStateEnum;
	}

	public void setBookStateEnum(BookStateEnum bookStateEnum) {
		this.bookStateEnum = bookStateEnum;
		handler.post(new Runnable() {

			@Override
			public void run() {
				setChanged();
				notifyObservers(ObserverEnum.kBookState);
			}
		});

	}

	// 书籍保存文件夹路径
	private String bookSaveDirPath() {
		return LocalCacheDataPathConstant.localBookCachePath() + "/" + bookInfo.getContent_id();
	}

	private static final String kTmpDownloadBookFileName = "tmp.zip";

	private String bookTmpZipResFilePath() {
		return bookSaveDirPath() + "/" + kTmpDownloadBookFileName;
	}

	// 删除书籍下载临时文件
	private void removeBookTmpZipResFile() {
		File file = new File(bookTmpZipResFilePath());
		if (file.exists()) {
			if (!file.delete()) {
				DebugLog.e(TAG, "删除缓存的未下载完成的书籍数据失败!");
			}
		}
	}

	// 从书城中, 点击一本还未下载的书籍时, 这本书籍会被加入
	// "本地书籍列表(在 GlobalDataCacheForMemorySingleton->localBookList 中保存)"
	// 目前有两个需求:
	// 1) 当A账户登录书城下载书籍时, 如果此时A账户退出了(或者被B账户替换了), 那么就要暂停正在进行下载的所有跟A账户绑定的书籍;
	// 这里考虑的一点是, 如果A/B账户切换时, 当前账户是希望独享下载网速的.
	// 但是, 对于跟 "公共账户" 绑定的书籍, 是不需要停止下载的.
	// 2) 已经存在于 "本地书籍列表" 中的未下载完成的书籍, 再次进行断点续传时, 需要将跟这本书绑定的账号信息传递给服务器,
	// 才能获取到最新的书籍下载地址.
	// 因为服务器为了防止盗链, 所以每次断点续传时, 都需要重新获取目标书籍的最新下载地址.
	private LogonNetRespondBean bindAccount;

	public LogonNetRespondBean getBindAccount() {
		return bindAccount;
	}

	public void setBindAccount(LogonNetRespondBean bindAccount) {
		this.bindAccount = bindAccount;
	}

	// 当前书籍所归属的本地文件夹
	private String folder;

	public String getFolder() {
		return folder;
	}

	public void setFolder(String folder) {
		this.folder = folder;
	}

	// 设置当前书籍最新的版本(可以通过书籍的版本来确定服务器是否有可以下载的更新包)
	public void setBookVersion(String bookLatestVersion) {

	};

	// 开始下载一本书籍
	public boolean startDownloadBook() {
		return startDownloadBookWithReceipt(null);
	}

	// 开始下载一本书籍(需要 "收据", 对应那种收费的书籍)
	public boolean startDownloadBookWithReceipt(byte[] receipt) {
		do {
			if (bookStateEnum != BookStateEnum.kBookStateEnum_Paid && bookStateEnum != BookStateEnum.kBookStateEnum_Free && bookStateEnum != BookStateEnum.kBookStateEnum_Update && bookStateEnum != BookStateEnum.kBookStateEnum_Pause) {
				// 只有书籍处于 Paid / Free / Update / Pause 状态时, 才有可能触发书籍下载
				break;
			}

			if (netRequestIndexForGetBookDownloadUrl.getIndex() != DomainBeanNetworkEngineSingleton.IDLE_NETWORK_REQUEST_ID) {
				// 已经在获取书籍的URL, 不需要重复发起书籍下载请求
				break;
			}

			GetBookDownloadUrlNetRequestBean netRequestBean = new GetBookDownloadUrlNetRequestBean(getBookInfo().getContent_id(), getBindAccount());

			DomainBeanNetworkEngineSingleton.getInstance.requestDomainProtocol(netRequestIndexForGetBookDownloadUrl, netRequestBean, new IDomainBeanAsyncNetRespondListener() {

				@Override
				public void onFailure(NetErrorBean error) {
					DebugLog.e(TAG, "获取书籍下载URL失败." + error.toString());
					//
					bookDownloadOrUnzipErrorHandlerWithMessage(error.getErrorMessage());
				}

				@Override
				public void onSuccess(Object respondDomainBean) {
					DebugLog.i(TAG, "获取要下载的书籍URL 成功!");
					GetBookDownloadUrlNetRespondBean getBookDownloadUrlNetRespondBean = (GetBookDownloadUrlNetRespondBean) respondDomainBean;
					startDownloadBookWithURLString(getBookDownloadUrlNetRespondBean.getBookDownloadUrl());
				}
			});

			// 更新书籍状态->GetBookDownloadUrl
			setBookStateEnum(BookStateEnum.kBookStateEnum_GetBookDownloadUrl);
			return true;
		} while (false);

		return false;
	}

	// 开始下载一本书籍(为了防止盗链, 所以每次下载书籍时的URL都是一次性的)
	private boolean startDownloadBookWithURLString(final String urlString) {
		String messageOfCustomError = "";
		do {
			DebugLog.i(TAG, "判断本地是否存在文件/文件夹!");
			if (!SimpleSDCardTools.isHasSDCard()) {
				// SD卡不存在, 就没有必要进行下载了
				break;
			}

			if (TextUtils.isEmpty(urlString)) {
				assert false : "入参urlString为空!";
				break;
			}
			DebugLog.i(TAG, "要下载的书籍URL = " + urlString);

			if (BookStateEnum.kBookStateEnum_Installed == bookStateEnum) {
				DebugLog.i(TAG, "已经安装成功的书籍不能重复下载!");
				break;
			}

			//
			DomainBeanNetworkEngineSingleton.getInstance.cancelNetRequestByRequestIndex(netRequestIndexForDownloadBookFile);

			// 创建书籍保存路径
			File file = new File(bookSaveDirPath());
			if (!file.exists()) {
				if (!file.mkdir()) {
					// 创建特定书籍文件夹失败, 此时没有必要再进行下载了
					messageOfCustomError = "创建要下载到本地的书籍的保存文件夹失败!";
					break;
				}
			}

			// 本地缓存的未下载完成的 书籍zip资源包 文件路径
			file = new File(bookTmpZipResFilePath());
			if (file.exists()) {
				if (BookStateEnum.kBookStateEnum_Paid == bookStateEnum) {
					// 如果当前书籍状态是 "已付费" 状态, 证明是还未进行操作/或者下载解压过程中出现失败情况, 此时会被复位成 "Paid"
					// 此时要先删除缓存数据, 然后重新下载.
					if (!file.delete()) {
						DebugLog.e(TAG, "删除缓存的未下载完成的书籍数据失败!");
						messageOfCustomError = "删除缓存的未下载完成的书籍文件 tmp.zip 失败!";
						break;
					}
				}
			}

			DebugLog.i(TAG, "开始联网下载");
			// 开始下载目标书籍 bookTmpZipResFilePath()
			DomainBeanNetworkEngineSingleton.getInstance.requestBookDownlaod(netRequestIndexForDownloadBookFile, urlString, bindAccount, bookTmpZipResFilePath(), new IFileAsyncHttpResponseHandler() {

				@Override
				public void onFailure(final NetErrorBean error) {
					DebugLog.i(TAG, "书籍下载失败 error=" + error.toString());

					bookDownloadOrUnzipErrorHandlerWithMessage(error.getErrorMessage());

					// 删除临时文件
					removeBookTmpZipResFile();
				}

				@Override
				public void onProgress(final long bytesWritten, final long totalSize) {

					float pro = (float) bytesWritten / totalSize * 100.0f;

					setDownloadProgress(pro);
				}

				@Override
				public void onSuccess(final File file) {
					DebugLog.i(TAG, "下载成功!");

					setBookStateEnum(BookStateEnum.kBookStateEnum_Unziping);
					// 当书籍下载成功后自动进行解压操作
					handler.post(new Runnable() {

						@Override
						public void run() {
							unzipBookZipResSelectorInBackground();
						}
					});

				}
			});

			// 更新书籍状态->Downloading
			setBookStateEnum(BookStateEnum.kBookStateEnum_Downloading);
			return true;
		} while (false);

		bookDownloadOrUnzipErrorHandlerWithMessage(messageOfCustomError);
		return false;
	}

	// 停止下载一本书籍
	public void stopDownloadBook() {
		if (bookStateEnum != BookStateEnum.kBookStateEnum_Downloading && bookStateEnum != BookStateEnum.kBookStateEnum_GetBookDownloadUrl) {
			// 只有处于 "Downloading / GetBookDownloadUrl" 状态的书籍, 才能被暂停.
			return;
		}

		// 更新书籍状态->Pause
		setBookStateEnum(BookStateEnum.kBookStateEnum_Pause);
		//
		DomainBeanNetworkEngineSingleton.getInstance.cancelNetRequestByRequestIndex(netRequestIndexForDownloadBookFile);
	}

	@Override
	public boolean equals(Object o) {
		return bookInfo.getContent_id().equals(((LocalBook) o).bookInfo.getContent_id());
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * 解压书籍
	 * 
	 * @author hesiming
	 * 
	 */
	public void unzipBookZipResSelectorInBackground() {

		String zipFilePath = LocalCacheDataPathConstant.localBookCachePath() + "/" + getBookInfo().getContent_id() + "/" + kTmpDownloadBookFileName;

		do {
			if (!SimpleSDCardTools.isHasSDCard()) {
				// SD卡不存在, 就没有必要进行解压了
				break;
			}

			if (TextUtils.isEmpty(zipFilePath)) {
				assert false : "入参zipFilePath为空!";
				break;
			}
			DebugLog.i(TAG, "要解压的书籍地址 = " + zipFilePath);

			if (BookStateEnum.kBookStateEnum_Installed == bookStateEnum) {
				DebugLog.i(TAG, "已经安装成功的书籍不能重复安装!");
				break;
			}
			File file = new File(zipFilePath);
			if (!file.exists()) {
				if (!file.mkdir()) {
					// 创建本地书籍缓存文件夹失败, 此时不能在创建特定书籍文件夹了
					break;
				}
			}
			// 更改状态
			ExtractZipFileTaskInSdcard extractZipFileTaskInSdcard = new ExtractZipFileTaskInSdcard(file);
			extractZipFileTaskInSdcard.execute();

			return;
		} while (false);

		//
		bookDownloadOrUnzipErrorHandlerWithMessage("解压书籍失败.");
	}

	private class ExtractZipFileTaskInSdcard extends AsyncTask<String, Integer, String> {
		private File zipFile;
		private int entryCount;

		public ExtractZipFileTaskInSdcard(File zipFile) {
			this.zipFile = zipFile;
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				createDocumentFileInSdCard(zipFile);
				Log.d(TAG, "Contents File Proc Complete............");
				return "Success";
			} catch (Exception e) {
				e.printStackTrace();
				return "Fail";
			} finally {
				// 删除临时文件
				removeBookTmpZipResFile();
			}
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			int percent = values[0] * 100 / entryCount;
			DebugLog.e(TAG, percent + "");
		}

		@Override
		protected void onPostExecute(String result) {
			if (result.equals("Success")) {
				setBookStateEnum(BookStateEnum.kBookStateEnum_Installed);
				DebugLog.e(TAG, "已经解压完成");

			} else {
				DebugLog.e(TAG, "无法正确读取课件，请确认设备以及SD卡的可用空间，或者在删除课件之后重新下载！");
				bookDownloadOrUnzipErrorHandlerWithMessage("解压书籍失败.");
			}

		}

		private void createDocumentFileInSdCard(File zipFile) throws Exception {
			// 创建zip文件
			ZipFile file = new ZipFile(zipFile);
			entryCount = file.size();
			// 获取输入流
			@SuppressWarnings("resource")
			ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry ze;
			// 获取上层路径
			String zipFileParent = zipFile.getParent();
			int index = 0;

			while ((ze = zip.getNextEntry()) != null) {
				// 获取zip实体的名称
				String path = ze.getName();

				if (ze.getName().indexOf("/") != -1) {
					// 创建以zip实体名称的文件
					File parent = new File(path).getParentFile();
					if (!parent.exists())
						if (!parent.mkdirs())
							throw new IOException("Unable to create folder " + parent);
				}

				Log.d(TAG, path);
				// 获取出zip文件名的路径，并创建输出流
				FileOutputStream fout = new FileOutputStream(zipFileParent + "/" + path);
				byte[] bytes = new byte[4096];
				// 循环将输入流离的数据写入到输出流中
				for (int c = zip.read(bytes); c != -1; c = zip.read(bytes)) {
					fout.write(bytes, 0, c);
				}
				zip.closeEntry();
				fout.close();

				index++;
				setDecompressProgress(index);
				publishProgress(index);

			}
		}
	}

	public interface IBookDownloadErrorBlockHandler {
		public void onError(final String errorMessage);
	}

	private IBookDownloadErrorBlockHandler bookDownloadErrorBlockHandler;

	// 书籍下载/解压过程中发生了错误时的处理方法
	// 此方法作用 :
	// 1. 通知外层发生了错误
	// 2. 复位书籍状态-->Pause, 好让用户可以重新下载
	public void setBookDownloadErrorBlockHandler(IBookDownloadErrorBlockHandler bookDownloadErrorBlockHandler) {
		this.bookDownloadErrorBlockHandler = bookDownloadErrorBlockHandler;
	}

	private void bookDownloadOrUnzipErrorHandlerWithMessage(final String message) {
		if (bookDownloadErrorBlockHandler != null) {

			// 通知外层, 发生了错误
			bookDownloadErrorBlockHandler.onError(message);
		}

		// 复位下载进度, 此时不需要通知外层
		downloadProgress = 0;

		// 复位当前书籍状态, 好让用户可以重新下载
		setBookStateEnum(BookStateEnum.kBookStateEnum_Pause);
	}
}
