package scrollsexplorer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyLoader
{
	public static String fileSep = System.getProperty("file.separator");

	public static String pathSep = System.getProperty("path.separator");

	public static File propFile;

	public static Properties properties;

	public static void load() throws IOException
	{
		load(null);
	}

	public static void load(String root) throws IOException
	{
		if (root == null)
			root = System.getProperty("user.home") + fileSep + "philjord";

		String filePath = root;
		File dirFile = new File(filePath);
		if (!dirFile.exists())
			dirFile.mkdirs();
		filePath = filePath + fileSep + "ElderScrollsExplorer.ini";
		propFile = new File(filePath);
		properties = new Properties();
		if (propFile.exists())
		{
			FileInputStream in = new FileInputStream(propFile);
			properties.load(in);
			in.close();
		}
	}

	public static void save()
	{
		//android needs different gear, I smell an interface
		//http://developer.android.com/guide/topics/data/data-storage.html#pref
		try
		{
			if (!propFile.exists())
				propFile.createNewFile();

			if (propFile.exists())
			{
				FileOutputStream out = new FileOutputStream(propFile);
				properties.store(out, "ElderScrollsExplorer Properties");
				out.close();
			}
		}
		catch (Throwable exc)
		{
			new Exception("Exception while saving application properties", exc).printStackTrace();
		}
	}
}
