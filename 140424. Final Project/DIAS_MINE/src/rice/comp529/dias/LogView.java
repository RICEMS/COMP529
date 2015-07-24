package rice.comp529.dias;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
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
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

public class LogView extends Fragment implements EventLog
{
    private TextView m_textView;   
    private Activity m_activity;
    private ScrollView m_scroll;
    private static final String TAG = "LogView";
    private String m_text = "";
    private FileOutputStream m_file;
    private boolean m_writeToFile = false;
    private String datafile;
    
	public LogView()
	{
		String filename = getNewFileName();
		try
		{
			datafile = //"/sdcard/" +
					Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
					//GlobalOptions.getDirectory() + 
					filename + ".txt";
			Log.i(TAG, "datafile=" + datafile);
			m_file = new FileOutputStream(new File(datafile));

			//m_activity.openFileOutput(datafile, Context.MODE_WORLD_READABLE);
			Log.i(TAG, datafile + " data file opened.");
			m_writeToFile = true;
		}
		catch (Exception e)
		{
			Log.e(TAG, e.getMessage());
			m_file = null;
			m_writeToFile = false;
		}
	}
	
	private String getNewFileName()
	{
		Calendar c = Calendar.getInstance();
		String s = "";
		s += c.get(Calendar.YEAR) + "_" + (c.get(Calendar.MONTH)+1) + "_" + c.get(Calendar.DAY_OF_MONTH) + "_";
		s += c.get(Calendar.HOUR_OF_DAY) + "_" + c.get(Calendar.MINUTE) + "_" + c.get(Calendar.SECOND);
		
		return s;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_activity = getActivity();
		
		Log.i(TAG, "onCreateView");
        
        View view = inflater.inflate(R.layout.logview, container, false);
        return view;
	}
	
	@Override
	public void onActivityCreated (Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		Log.i(TAG, "onActivityCreated");
    	//  Initialize the text.
        m_textView = (TextView)m_activity.findViewById(R.id.logTextView);
    	m_textView.setText(m_text);
       	m_textView.setMovementMethod(ScrollingMovementMethod.getInstance());
    	
       	//  Get the scroller.
        m_scroll = (ScrollView)m_activity.findViewById(R.id.logScrollView);
     	//  Initialize Buttons
        Button b;
		b = (Button)m_activity.findViewById(R.id.logButtonClear);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.i(TAG, "Clear");
				clear();
			}
		});
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
		//m_time = System.currentTimeMillis() - m_startTime;
		append(System.currentTimeMillis() + " \t" + s);
	}
	public void append(final String s)
	{
		if (m_writeToFile && (m_file != null))
		{
			try {
				m_file.write((s+"\r\n").getBytes());
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, e.getMessage());
				m_textView.append(e.getMessage());
			}
		}
		m_text += s + "\n";
		m_activity.runOnUiThread(new Runnable()
		{
			public void run()
			{
				//m_textView.append(((m_file==null)?"nullfile":datafile) + "\n");
				m_textView.append(s+"\n");
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
