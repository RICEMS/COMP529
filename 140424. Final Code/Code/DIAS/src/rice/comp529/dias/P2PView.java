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

public class P2PView extends Fragment implements EventLog
{
    private TextView m_textView;   
    private Activity m_activity;
    private ScrollView m_scroll;
    private static final String TAG = "P2PView";
    private String m_text = "";
    
	public P2PView()
	{
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_activity = getActivity();
		
		Log.i(TAG, "onCreateView");
        
        View view = inflater.inflate(R.layout.p2pview, container, false);
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
	
	//  Remove first lines from text view if over max length.
	private void removeTopLines()
	{
	    int linesToRemove = m_textView.getLineCount() - 400;
	    if (linesToRemove <= 0)
	    	return;
	    CharSequence cs = m_textView.getText();
	    int index;
	    for (index = 0; index < cs.length(); index++)
	    {
	    	if (cs.charAt(index) == '\n')
	    	{
	    		linesToRemove--;
	    	}
	    	if (linesToRemove == 0)
	    		break;
	    }
	    m_text = cs.subSequence(index++, cs.length()).toString();
	    m_textView.setText(m_text);
	}
	
	public void addEvent(final String s)
	{
		m_text += s;
		m_activity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				m_textView.append(s);
				removeTopLines();
				m_scroll.post(new Runnable()
				{
		            @Override
		            public void run()
		            {
		                m_scroll.fullScroll(ScrollView.FOCUS_DOWN);
		            }
		        });
			}
		});
	}
	
	public void clear()
	{
		m_text = "";
		m_textView.setText(m_text);
	}
    
}
