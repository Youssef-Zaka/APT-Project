import 'package:crawlit_search_engine/Results.dart';
import 'package:crawlit_search_engine/strings/string_en.dart';
import 'package:flutter/material.dart';

class SearchScreen extends StatefulWidget {
  const SearchScreen({Key? key}) : super(key: key);

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  var textController = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final String phrase = ModalRoute.of(context)!.settings.arguments as String;

    //TODO: Search for phrase and get List of results

    List<String> results = [
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
      'https://pub.dev/',
      'https://stackoverflow.com/',
      'https://en.wikipedia.org/wiki/Main_Page',
      'http://eng.cu.edu.eg/en/',
      'https://github.com/',
    ];

    textController.text = phrase;

    return Scaffold(
      body: Column(
        mainAxisAlignment: MainAxisAlignment.start,
        children: [
          const SizedBox(
            height: 30,
          ),
          Padding(
            padding: const EdgeInsets.only(left: 20, right: 20),
            child: TextField(
              onEditingComplete: () {
                textController.text.isEmpty
                    ? null
                    : Navigator.popAndPushNamed(context, '/search',
                        arguments: textController.text);
              },
              enableSuggestions: true,
              controller: textController,
              cursorColor: Colors.red,
              cursorHeight: 20,
              decoration: InputDecoration(
                suffixIcon: const Icon(
                  Icons.mic,
                  color: Color.fromARGB(255, 3, 81, 197),
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
          Flexible(
            child: SizedBox(
              height: MediaQuery.of(context).size.height,
              child: ListView.builder(
                  shrinkWrap: true,
                  physics: const ClampingScrollPhysics(),
                  itemBuilder: (context, index) {
                    return Results(result: results[index]);
                  },
                  itemCount: results.length),
            ),
          ),
          const SizedBox(
            height: 20,
          ),
        ],
      ),
    );
  }
}
