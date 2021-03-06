import 'package:crawlit_search_engine/screens/homeScreen.dart';
import 'package:crawlit_search_engine/screens/searchScreen.dart';
import 'package:crawlit_search_engine/screens/voiceScreen.dart';
import 'package:flutter/material.dart';
import 'strings/string_en.dart';

void main() {
  Paint.enableDithering = true;
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: Strings.appNameCamel,
      initialRoute: '/',
      routes: {
        // When navigating to the "/" route, build the FirstScreen widget.
        // '/': (context) => const HomeScreen(),
        '/': (context) => const HomeScreen(),
        '/search': (context) => const SearchScreen(),
        '/voice': (context) => const VoiceScreen(),
      },
      debugShowCheckedModeBanner: false,
    );
  }
}
