package rice.comp529.dias;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import android.util.Log;

public class JettyServer{

	public static final int SERVERPORT = 8080; 
	
	private Server server;

	public void start() 
	{
		Handler handler = new AbstractHandler() {

			@Override
			public void handle(String arg0, Request arg1,
					HttpServletRequest request, HttpServletResponse servletResponse)
							throws IOException, ServletException {
				servletResponse.setContentType("text/html");
				servletResponse.setStatus(HttpServletResponse.SC_OK);
				servletResponse.getWriter().println("<h1>Hello World</h1>");
				((Request) request).setHandled(true);
			}

		};

		this.server = new Server(SERVERPORT);
		server.setHandler(handler);
		try {
			server.start();
			Log.e("JT", "started");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void stop()
	{
		try {
			this.server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}
	public static final int SERVERPORT = 1234;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	 // A placeholder fragment containing a simple view.

	public static class PlaceholderFragment extends Fragment implements OnClickListener {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			((Button)rootView.findViewById(R.id.button1)).setOnClickListener(this);
			return rootView;
		}

		@Override
		public void onClick(View v) {
			st();
		}
	 **/

}

