package main.java.cn.retech.activity;

/**
 * Fragment部分操作功能抽象
 * 
 * @author Administrator
 * 
 */
public interface IFragmentOptions {
  /**
   * 查询图书
   */
  public void doSearch(String bookName, boolean isLocalSearch);

  /**
   * 刷新当前页面
   */
  public void refresh();
}
