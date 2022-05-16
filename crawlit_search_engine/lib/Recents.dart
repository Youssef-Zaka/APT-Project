import 'package:crawlit_search_engine/strings/string_en.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

class Recents extends StatelessWidget {
  const Recents({
    Key? key,
    required this.size,
  }) : super(key: key);

  final Size size;

  @override
  Widget build(BuildContext context) {
    return Flexible(
      child: DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.2,
        minChildSize: 0.2,
        maxChildSize: 0.8,
        builder: (context, scrollController) {
          return SingleChildScrollView(
            controller: scrollController,
            child: Container(
              height: size.height * 0.8,
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                    colors: [
                      Color.fromARGB(255, 255, 2, 57),
                      Color.fromARGB(255, 3, 81, 197),
                      Color.fromARGB(255, 3, 158, 197),
                    ],
                    begin: Alignment.topRight,
                    end: Alignment.bottomLeft,
                    tileMode: TileMode.clamp),
                borderRadius: BorderRadius.only(
                  topLeft: Radius.circular(40),
                  topRight: Radius.circular(40),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  const SizedBox(
                    height: 40,
                  ),
                  const Text(
                    Strings.About,
                    style: TextStyle(
                      fontSize: 30,
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  SizedBox(
                    height: size.height * 0.15,
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20),
                    child: Column(
                      children: [
                        RichText(
                          text: TextSpan(
                            children: [
                              const TextSpan(
                                text: 'Crawlit',
                                style: TextStyle(
                                  fontSize: 25,
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const TextSpan(
                                text:
                                    ' is an awsome search engine that helps you find the best '
                                    'content for you. '
                                    'So what are you looking for? just type in the search bar and '
                                    'press enter. '
                                    'You can also use voice search to find the content you are looking for. '
                                    'Awsome content is just a search away!'
                                    '\n\n',
                                style: TextStyle(
                                  fontSize: 20,
                                  color: Colors.white,
                                ),
                              ),
                              const TextSpan(
                                text:
                                    'Crawlit is a free and open source project. '
                                    ' You can find the source code on github, '
                                    'just click on the link below to open the repo:'
                                    '\n\n',
                                style: TextStyle(
                                  fontSize: 15,
                                  color: Colors.white,
                                ),
                              ),
                              TextSpan(
                                text:
                                    'https://github.com/Youssef-Zaka/APT-Project'
                                    '\n\n',
                                style: const TextStyle(
                                  fontSize: 15,
                                  color: Color.fromARGB(255, 230, 118, 118),
                                ),
                                recognizer: TapGestureRecognizer()
                                  ..onTap = () {
                                    var url = Uri.parse(
                                        'https://github.com/Youssef-Zaka/APT-Project');
                                    launchUrl(url);
                                  },
                              ),
                              //new lines are added here
                            ],
                          ),
                        ),
                        SizedBox(
                          height: size.height * 0.05,
                        ),
                        const Text('YOUSSEF',
                            style: TextStyle(
                              fontSize: 23,
                              color: Colors.white,
                              fontWeight: FontWeight.w200,
                              letterSpacing: 7,
                            )),
                        const Text(
                          'ZAKA',
                          style: TextStyle(
                            fontSize: 23,
                            color: Colors.white,
                            fontWeight: FontWeight.w300,
                            letterSpacing: 7,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
