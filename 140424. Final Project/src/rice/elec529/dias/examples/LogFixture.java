package rice.elec529.dias.examples;

import rice.elec529.dias.interfaces.LogInterface;

public class LogFixture implements LogInterface
{
	@Override
	public void log(String s)
	{
		System.out.println(s);
	}

}
