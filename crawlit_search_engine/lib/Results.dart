import 'dart:math';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:html/parser.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:shimmer/shimmer.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

class Results extends StatelessWidget {
  const Results({
    Key? key,
    required this.result,
    required this.query,
  }) : super(key: key);
  final String result;
  final List<String> query;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String>(
      future: request(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const ShimmerResult();
        }

        if (snapshot.hasData) {
          var doc = parse(snapshot.data);
          var title = '';

          //make sure the website has a title
          doc.getElementsByTagName('title').isNotEmpty
              ? title = doc.getElementsByTagName('title')[0].text
              : title = result;
          title = title.trim();
          //get first paragraph
          String firstParagraph = '';
          if (doc.getElementsByTagName('p').isEmpty) {
            firstParagraph = 'No description';
          }

          //check if there is a paragraph
          if (doc.getElementsByTagName('p').isNotEmpty) {
            //get all paragraphs
            var paragraphs = doc.getElementsByTagName('p');
            firstParagraph = '';
            //add all paragraphs to the first paragraph
            for (var i = 0; i < paragraphs.length; i++) {
              firstParagraph += paragraphs[i].text;
            }
          }

          //get first paragraph as a list
          List<String> firstParagraphList = firstParagraph.split(' ');
          //for each word in the first paragraph
          for (var i = 0; i < firstParagraphList.length; i++) {
            //if the word is in the query
            if (query.contains(firstParagraphList[i])) {
              //make the word bold
              firstParagraphList[i] = '**${firstParagraphList[i]}**';
            }
          }
          //join the list to a string
          firstParagraph = firstParagraphList.join(' ');
          firstParagraph = firstParagraph.trim();

          //for each word in the query get its index in first paragraph and save the smalest index
          int smallestIndex = 0;
          for (var i = 0; i < query.length; i++) {
            if (firstParagraph.contains('**' + query[i] + '**')) {
              int index = firstParagraph.indexOf('**' + query[i] + '**');
              if (index < smallestIndex) {
                smallestIndex = index;
              }
            }
          }
          //if the query is in the first paragraph
          if (smallestIndex != -1) {
            //if the index is under 100, do nothing
            //otherwise only show 100 characters before the query
            if (smallestIndex < 100) {
              //do nothing
            } else {
              firstParagraph =
                  firstParagraph.substring(smallestIndex - 100) + '...';
              //add '...' to the start of the string
              firstParagraph = '...' + firstParagraph;
            }
          }

          //if larger than 500 characters, only show 500 or so characters
          if (firstParagraph.length > 500) {
            //substring first paragraph to a random number of characters between 450 and 550
            firstParagraph =
                firstParagraph.substring(0, Random().nextInt(100) + 450);
            //add '...' to the end of the string
            firstParagraph = firstParagraph + '...';
          }

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
                    overflow: TextOverflow.ellipsis,
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
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Color.fromARGB(129, 14, 18, 228),
                    fontSize: 16,
                  ),
                ),
                MarkdownBody(
                  data: firstParagraph,
                  styleSheet: MarkdownStyleSheet(
                    p: const TextStyle(
                      fontSize: 12,
                      color: Color.fromARGB(255, 34, 79, 104),
                    ),
                    code: const TextStyle(
                      fontSize: 12,
                      color: Color.fromARGB(255, 34, 79, 104),
                    ),
                    blockquote: const TextStyle(
                      fontSize: 12,
                      color: Color.fromARGB(255, 34, 79, 104),
                    ),
                    blockquoteDecoration: null,
                    codeblockDecoration: null,
                  ),
                ),
              ],
            ),
          );
        } else if (snapshot.hasError) {
          return Text("${snapshot.error}");
        }
        return const ShimmerResult();
      },
    );
  }

  Future<String> request() async {
    final url = Uri.parse(result);
    var response = await http.get(url);
    if (response.statusCode == 200) {
      return response.body;
    } else {
      return ' ';
    }
  }
}

class ShimmerResult extends StatelessWidget {
  const ShimmerResult({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 200.0,
      height: 100.0,
      child: Shimmer.fromColors(
        baseColor: Colors.blueGrey.shade100,
        highlightColor: const Color.fromARGB(255, 230, 218, 220),
        child: Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 14.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Container(
                        width: MediaQuery.of(context).size.width * 0.6,
                        height: 20.0,
                        color: Colors.red,
                      ),
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 6.0),
                      ),
                      Container(
                        width: double.infinity,
                        height: 8.0,
                        color: Colors.white,
                      ),
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 2.0),
                      ),
                      Container(
                        width: double.infinity,
                        height: 8.0,
                        color: Colors.white,
                      ),
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 2.0),
                      ),
                      Container(
                        width: double.infinity,
                        height: 8.0,
                        color: Colors.white,
                      ),
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 2.0),
                      ),
                      Container(
                        width: 40.0,
                        height: 8.0,
                        color: Colors.white,
                      ),
                    ],
                  ),
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
