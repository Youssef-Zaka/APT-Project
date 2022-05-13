import 'package:crawlit_search_engine/strings/string_en.dart';
import 'package:flutter/material.dart';
import 'package:flutter_speech/flutter_speech.dart';
import 'package:crawlit_search_engine/util/CrawlitServer.dart' as crawlit;

class VoiceScreen extends StatefulWidget {
  const VoiceScreen({Key? key}) : super(key: key);

  @override
  State<VoiceScreen> createState() => _VoiceScreenState();
}

class _VoiceScreenState extends State<VoiceScreen> {
  var _speech = SpeechRecognition();
  bool _speechRecognitionAvailable = false;
  bool _isListening = false;
  String transcription = '';
  @override
  void initState() {
    super.initState();
    _speech.setAvailabilityHandler(
        (bool result) => setState(() => _speechRecognitionAvailable = result));

    _speech.setRecognitionStartedHandler(
        () => setState(() => _isListening = true));
    // this handler will be called during recognition.
    // On Android devices, only the final transcription is received
    _speech.setRecognitionResultHandler(
        (String text) => setState(() => transcription = text));

    _speech.setRecognitionCompleteHandler(
      (_) => setState(() async {
        if (transcription.isEmpty) {
          return;
        }
        crawlit.ServerUtil.query(transcription).then((_) {
          Navigator.popAndPushNamed(context, '/search',
              arguments: transcription);
        });
      }),
    );

    _speech
        .activate('en_US') // activate speech recognition with US english.
        .then(
          (res) => setState(() => _speechRecognitionAvailable = res),
        );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _isListening
          ? Center(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  children: [
                    const SizedBox(
                      height: 20,
                    ),
                    const Text(
                      'Listening...',
                      style: TextStyle(
                        fontSize: 40,
                        fontWeight: FontWeight.bold,
                        color: Colors.red,
                      ),
                    ),
                    const SizedBox(
                      height: 40,
                    ),
                    Padding(
                      padding: const EdgeInsets.all(14.0),
                      child: Text(
                        transcription,
                        style: const TextStyle(
                          fontSize: 30,
                          color: Colors.blue,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            )
          : Center(
              child: IconButton(
                  icon: const Icon(
                    Icons.mic,
                    color: Colors.red,
                    size: 50,
                  ),
                  onPressed: () {
                    _speech.listen().then((result) {});
                  }),
            ),
    );
  }
}
