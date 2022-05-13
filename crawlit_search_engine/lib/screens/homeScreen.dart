import 'package:crawlit_search_engine/strings/string_en.dart';
import 'package:flutter/material.dart';
import 'package:crawlit_search_engine/Recents.dart';
import 'package:crawlit_search_engine/util/CrawlitServer.dart' as crawlit;

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  var textController = TextEditingController();
  @override
  void initState() {
    super.initState();
    crawlit.ServerUtil();
  }

  @override
  Widget build(BuildContext context) {
    final Size size = MediaQuery.of(context).size;
    return Scaffold(
      body: SingleChildScrollView(
        child: Container(
          height: size.height,
          color: Colors.white,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              SizedBox(
                height: 30,
                width: size.width,
              ),
              const ClipRect(
                child: Align(
                  child: Image(
                    image: AssetImage('assets/images/CRAWLIT.png'),
                    width: 130,
                    height: 130,
                  ),
                  heightFactor: 0.4,
                  alignment: Alignment.center,
                ),
                clipBehavior: Clip.hardEdge,
              ),
              Padding(
                padding: const EdgeInsets.only(left: 20, right: 20),
                child: TextField(
                  controller: textController,
                  onEditingComplete: () async {
                    if (textController.text.isEmpty) {
                      return;
                    }
                    await crawlit.ServerUtil.query(textController.text);
                    Navigator.pushNamed(context, '/search',
                        arguments: textController.text);
                  },
                  cursorColor: Colors.red,
                  cursorHeight: 20,
                  decoration: InputDecoration(
                    suffixIcon: GestureDetector(
                      onTap: () {
                        //open Voice Search
                        Navigator.pushNamed(context, '/voice');
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
                        width: 2,
                        color: Colors.red,
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(
                height: 60,
              ),
              Recents(size: size),
            ],
          ),
        ),
      ),
    );
  }
}
