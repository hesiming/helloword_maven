package cn.retech.my_domainbean_engine.net_error_handle;

import java.io.Serializable;

import org.apache.http.HttpStatus;

/**
 * 网络访问过程中出现错误时的数据Bean
 */
public final class NetErrorBean implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4841567150604927632L;

	// 错误类型
	private NetErrorTypeEnum errorType = NetErrorTypeEnum.NET_ERROR_TYPE_SUCCESS;
	// 错误代码
	private int errorCode = HttpStatus.SC_OK;
	// 错误描述信息
	private String errorMessage = "OK";

	public NetErrorBean() {
	}

	/**
	 * 重新初始化
	 * 
	 * @param srcObject
	 */
	public void reinitialize(final NetErrorBean srcObject) {
		if (srcObject != null) {
			this.errorCode = srcObject.errorCode;
			this.errorMessage = srcObject.errorMessage;
			this.errorType = srcObject.errorType;
		} else {
			this.errorCode = HttpStatus.SC_OK;
			this.errorMessage = "OK";
			this.errorType = NetErrorTypeEnum.NET_ERROR_TYPE_SUCCESS;
		}
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public NetErrorTypeEnum getErrorType() {
		return errorType;
	}

	public void setErrorType(NetErrorTypeEnum errorType) {
		this.errorType = errorType;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public NetErrorBean clone() {
		NetErrorBean o = null;
		try {
			o = (NetErrorBean) super.clone();// Object 中的clone()识别出你要复制的是哪一个对象。
			o.errorType = this.errorType;
			o.errorCode = this.errorCode;
			o.errorMessage = this.errorMessage;
		} catch (CloneNotSupportedException e) {
			System.out.println(e.toString());
		}
		return o;
	}

	@Override
	public String toString() {
		return "NetErrorBean [errorType=" + errorType + ", errorCode=" + errorCode + ", errorMessage=" + errorMessage + "]";
	}

}
