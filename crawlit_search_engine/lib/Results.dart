import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:html/parser.dart';
import 'package:url_launcher/url_launcher.dart';

class Results extends StatelessWidget {
  const Results({Key? key, required this.result}) : super(key: key);
  final String result;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String>(
      future: request(),
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          var doc = parse(snapshot.data);
          //get the title
          var title = doc.getElementsByTagName('title')[0].text;
          //get first paragraph
          String firstParagraph;
          if (doc.getElementsByTagName('p').isEmpty) {
            firstParagraph = 'No description';
          }
          doc.getElementsByTagName('p').length > 1
              ? firstParagraph = doc.getElementsByTagName('p')[0].text +
                  ' ' +
                  doc.getElementsByTagName('p')[1].text
              : firstParagraph = doc.getElementsByTagName('p')[0].text;
          return Padding(
            padding: const EdgeInsets.all(14.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                GestureDetector(
                  onTap: () async {
                    final url = Uri.parse(result);
                    if (await canLaunchUrl(url)) {
                      await launchUrl(url);
                    } else {
                      throw 'Could not launch $url';
                    }
                  },
                  child: Text(
                    title,
                    maxLines: 3,
                    overflow: TextOverflow.fade,
                    style: const TextStyle(
                      color: Colors.red,
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                    ),
                  ),
                ),
                Text(
                  result,
                  maxLines: 1,
                  overflow: TextOverflow.fade,
                  style: const TextStyle(
                    color: Color.fromARGB(129, 14, 18, 228),
                    fontSize: 16,
                  ),
                ),
                Text(
                  firstParagraph,
                  maxLines: 5,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color.fromARGB(255, 34, 79, 104),
                  ),
                ),
              ],
            ),
          );
        } else if (snapshot.hasError) {
          return Text("${snapshot.error}");
        }
        return Container(
          color: Colors.amber,
        );
      },
    );
  }

  Future<String> request() async {
    final url = Uri.parse(result);
    var response = await http.get(url);
    return response.body;
  }
}
