package cn.retech.adapter;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MyFragmentPagerAdapter extends FragmentPagerAdapter {
  private List<Fragment> mFragments = new ArrayList<Fragment>();

  public MyFragmentPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
    super(fm);
    this.mFragments = fragments;
  }

  @Override
  public int getCount() {
    if (null != mFragments) {
      return mFragments.size();
    }

    return 0;
  }

  @Override
  public Fragment getItem(int position) {
    return mFragments.get(position);
  }

}
