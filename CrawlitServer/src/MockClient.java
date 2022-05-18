import java.io.*;
import java.net.Socket;

public class MockClient {

	public static void main(String[] args) throws IOException {
		try (// TODO Auto-generated method stub
		Socket mockSocket = new Socket("localhost", 6667)) {
			PrintWriter mockOut = new PrintWriter(mockSocket.getOutputStream(), true);

			BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
			BufferedReader mockReader = new BufferedReader(new InputStreamReader(mockSocket.getInputStream()));
			String message = null;
			System.out.print("Borrow (b) or Return (r): ");
			while ((message = consoleReader.readLine()) != null) {
			    String response = null;
			    mockOut.println(message);
			    response = mockReader.readLine();
			    System.out.println(response);
			}
		}

	}

}
