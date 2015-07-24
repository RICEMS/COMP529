package rice.comp529.dias;

import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

public class OptionsView extends Fragment
{   
    private Activity m_activity;
    private static final String TAG = "OptionsView";
    
	public OptionsView()
	{
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_activity = getActivity();
		Log.i(TAG, "onCreateView");
        
        View view = inflater.inflate(R.layout.optionsview, container, false);
        return view;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		Log.i(TAG, "onActivityCreated");
	}
	
	@Override
	public void onPause ()
	{
		super.onPause();
		Log.i(TAG, "onPause");
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		Log.i(TAG, "onResume");
	}
	
	@Override
	public void onDestroyView ()
	{
		super.onDestroyView();
		Log.i(TAG, "onDestroyView");
	}
}
