import 'dart:async';
import 'dart:io';

//Utilities that manage connections with server sockets.

//ServerUtil Class
class ServerUtil {
  static const port = 6666;
  static const host = '192.168.1.4';
  static late Socket socket;
  static bool connected = false;
  //a list of urls returned by the server
  static List<String> urls = [];

  //Constructor
  ServerUtil() {
    //Initialize the socket.
    Socket.connect(host, port).then((Socket sock) {
      socket = sock;
      connected = true;
      socket.listen(dataHandler,
          onError: errorHandler, onDone: doneHandler, cancelOnError: false);
      //send a message to the server.
    }).catchError((e) {
      print("Unable to connect: $e");
    });
  }

  //Query method that sends a message to the server. The server will return a list of urls.
  //The urls will be added to the urls list.
  //The urls list will be returned.
  static Future<List<String>> query(String userQuery) async {
    //check if socket is connected.
    if (connected) {
      urls.clear();
      //send the query to the server.
      socket.writeln(userQuery);

      await Future.delayed(const Duration(milliseconds: 700));

      return urls;
    }
    return urls;
  }

  //Handles data from the server.
  void dataHandler(data) {
    //String of received data.
    String dataString = String.fromCharCodes(data).trim();
    //if string == [] then urls is empty.
    if (dataString == "[]" || dataString == "][") {
      return;
    }
    //remove first and last character from the string.
    dataString = dataString.substring(1, dataString.length - 1);
    //remove all the whitespace characters from the string.
    dataString = dataString.replaceAll(RegExp(r'\s+'), '');
    urls.addAll(dataString.split(','));
    //print urls count.
    print('zaka: ' + urls.length.toString());
  }

  //Handles errors from the server.
  void errorHandler(error, StackTrace trace) {
    print(error);
  }

//Handles when the connection is done.
  void doneHandler() {
    socket.destroy();
  }
}
