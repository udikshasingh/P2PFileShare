import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class Logger {
	static FileOutputStream fos;
	static OutputStreamWriter outputstream;

	public static void setup(String f) throws IOException {
		fos = new FileOutputStream(f);
		outputstream = new OutputStreamWriter(fos, "UTF-8");
	}

	public static void stop() {
		try {
			outputstream.flush();
			fos.close();
		} catch (Exception e) {}
	}

	public static void info(String str) {
		try {
			outputstream.write(str + '\n');
		} catch (IOException e) {}
	}

}
