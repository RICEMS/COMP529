package rice.comp529.dias;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ActionBar.Tab;

public class TabController
{
	private int m_fragmentId;
	private ActionBar m_actionBar;
	public static boolean m_enabled = true;
	
	public TabController(Activity activity, int fragmentid)
	{
        m_actionBar = activity.getActionBar();
        //actionBar.setDisplayHomeAsUpEnabled(true);
        m_actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        m_fragmentId = fragmentid;
	}
	
	public void addTab(String text, Fragment fragment)
	{
        ActionBar.Tab tab = m_actionBar.newTab().setText(text);
        tab.setTabListener(new TabListener(fragment));
        m_actionBar.addTab(tab);
    }
	
	class TabListener implements ActionBar.TabListener
	{
    	public Fragment m_fragment;

    	public TabListener(Fragment fragment) {
    		m_fragment = fragment;
    	}

    	@Override
    	public void onTabReselected(Tab tab, FragmentTransaction ft) {
    		//do what you want when tab is reselected, I do nothing
    	}

    	@Override
    	public void onTabSelected(Tab tab, FragmentTransaction ft) {
    		if (m_enabled)
    			ft.replace(m_fragmentId, m_fragment);
    	}

    	@Override
    	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    		if (m_enabled)
    			ft.remove(m_fragment);
    	}
    }
}
