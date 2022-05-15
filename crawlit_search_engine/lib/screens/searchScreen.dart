import 'package:crawlit_search_engine/Results.dart';
import 'package:crawlit_search_engine/strings/string_en.dart';
import 'package:flutter/material.dart';
import 'package:number_paginator/number_paginator.dart';
import 'package:crawlit_search_engine/util/CrawlitServer.dart' as crawlit;

class SearchScreen extends StatefulWidget {
  const SearchScreen({Key? key}) : super(key: key);

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  var textController = TextEditingController();

  static List<String> results = [];
  List<String> paginatedResults = [];

  @override
  void initState() {
    super.initState();
    //comment the following two lines to test out a no results page
    results = crawlit.ServerUtil.urls;
    paginatedResults = results.length > 10 ? results.sublist(0, 10) : results;
  }

  @override
  Widget build(BuildContext context) {
    final String phrase = ModalRoute.of(context)!.settings.arguments as String;

    textController.text = phrase;

    return Scaffold(
      body: ListView(children: [
        Column(
          mainAxisSize: MainAxisSize.min,
          mainAxisAlignment: MainAxisAlignment.start,
          children: [
            const SizedBox(
              height: 30,
            ),
            Padding(
              padding: const EdgeInsets.only(left: 20, right: 20),
              child: TextField(
                onEditingComplete: () async {
                  if (textController.text.isEmpty) {
                    return;
                  }
                  await crawlit.ServerUtil.query(textController.text);
                  Navigator.pushNamed(context, '/search',
                      arguments: textController.text);
                },
                enableSuggestions: true,
                controller: textController,
                cursorColor: Colors.red,
                cursorHeight: 20,
                decoration: InputDecoration(
                  suffixIcon: GestureDetector(
                    onTap: () {
                      //open Voice Search
                      Navigator.popAndPushNamed(context, '/voice');
                    },
                    child: const Icon(
                      Icons.mic,
                      color: Color.fromARGB(255, 3, 81, 197),
                    ),
                  ),
                  hintText: Strings.search,
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(30),
                    borderSide: const BorderSide(
                      width: 1,
                      color: Colors.red,
                    ),
                  ),
                ),
              ),
            ),
            results.isNotEmpty
                ? Flexible(
                    child: SizedBox(
                      height: MediaQuery.of(context).size.height,
                      child: ListView.builder(
                          shrinkWrap: true,
                          physics: const ClampingScrollPhysics(),
                          itemBuilder: (context, index) {
                            return Results(result: paginatedResults[index]);
                          },
                          itemCount: paginatedResults.length),
                    ),
                  )
                : const NoResults(),
            const SizedBox(
              height: 20,
            ),
            results.isNotEmpty
                ? Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: NumberPaginator(
                      initialPage: 0,
                      buttonUnselectedForegroundColor: Colors.blue.shade800,
                      buttonSelectedBackgroundColor: Colors.red,
                      buttonSelectedForegroundColor: Colors.white,
                      buttonUnselectedBackgroundColor: Colors.white,
                      buttonShape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(15),
                      ),
                      numberPages: (results.length % 10 == 0)
                          ? results.length ~/ 10
                          : results.length ~/ 10 + 1,
                      onPageChange: (page) {
                        setState(() {
                          paginatedResults = results.sublist(
                              page * 10,
                              ((page + 1) * 10) > results.length
                                  ? results.length
                                  : ((page + 1) * 10));
                        });
                      },
                    ),
                  )
                : Container(),
          ],
        ),
      ]),
    );
  }
}

class BulletText extends StatelessWidget {
  const BulletText({
    Key? key,
    required this.text,
  }) : super(key: key);
  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      '• $text',
      style: const TextStyle(
        fontSize: 15,
        color: Colors.black,
      ),
    );
  }
}

class NoResults extends StatelessWidget {
  const NoResults({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Center(
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Image(
              image: AssetImage('assets/images/notFound.gif'),
            ),
            FittedBox(
              fit: BoxFit.contain,
              child: Padding(
                padding: EdgeInsets.symmetric(horizontal: 100),
                child: Text(
                  'Your search did not return any results.',
                  overflow: TextOverflow.fade,
                  style: TextStyle(
                    fontSize: 100,
                    fontWeight: FontWeight.bold,
                    color: Colors.red,
                  ),
                ),
              ),
            ),
            SizedBox(
              height: 20,
            ),
            FittedBox(
              fit: BoxFit.contain,
              child: Padding(
                padding: EdgeInsets.only(left: 20),
                child: BulletText(
                    text: 'Make sure that all words are spelled correctly.'),
              ),
            ),
            FittedBox(
              fit: BoxFit.contain,
              child: Padding(
                padding: EdgeInsets.only(left: 20),
                child: BulletText(text: 'Try different keywords.'),
              ),
            ),
            FittedBox(
              fit: BoxFit.contain,
              child: Padding(
                padding: EdgeInsets.only(left: 20),
                child: BulletText(text: 'Try more general keywords.'),
              ),
            ),
            FittedBox(
              fit: BoxFit.contain,
              child: Padding(
                padding: EdgeInsets.only(left: 20),
                child: BulletText(text: 'Try fewer keywords.'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
