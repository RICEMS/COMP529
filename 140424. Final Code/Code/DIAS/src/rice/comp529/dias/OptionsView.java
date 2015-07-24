package rice.comp529.dias;

import android.app.Activity;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

public class OptionsView extends Fragment
{   
	private Activity m_activity;
	private EditText m_EditText_Name;
	private EditText m_EditText_Psw;
	private EditText m_EditText_FailO;

	private static final String TAG = "OptionsView";
	private static MainActivity m_main;

	public void setMainActivity(MainActivity main)
	{
		m_main = main;
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
		
		//  Load the Options
		GlobalOptions go = GlobalOptions.getOptions(getActivity());
		m_EditText_Name=(EditText)m_activity.findViewById(R.id.txtName);
		m_EditText_Name.setText(go.nodeName);
		m_EditText_Psw=(EditText)m_activity.findViewById(R.id.txtPsw);
		m_EditText_Psw.setText("********");
		m_EditText_FailO=(EditText)m_activity.findViewById(R.id.txtFailover);
		m_EditText_FailO.setText(go.replicationNode);

		//Submit Form
		Button Submitbtn = (Button)m_activity.findViewById(R.id.btnSubmit);
		Submitbtn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				Log.i(TAG, "Saving Options");
				GlobalOptions go = GlobalOptions.getOptions(getActivity());
				go.nodeName = m_EditText_Name.getText().toString();
				go.replicationNode = m_EditText_FailO.getText().toString();
				m_main.saveOptions();
			}
		});
		//Clear Form
		Button Clearbtn = (Button)m_activity.findViewById(R.id.btnClear);
		Clearbtn.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				Log.i(TAG, "Clear Configuration");

				m_EditText_Name=(EditText)m_activity.findViewById(R.id.txtName);
				m_EditText_Psw=(EditText)m_activity.findViewById(R.id.txtPsw);
				m_EditText_FailO=(EditText)m_activity.findViewById(R.id.txtFailover);
				m_EditText_Name.setText("");
				m_EditText_Psw.setText("");
				m_EditText_FailO.setText("");
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

}
